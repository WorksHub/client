(ns wh.user.events
  (:require
    [ajax.formats :as ajax-formats]
    [cljs-time.coerce :as time-coerce]
    [goog.net.cookies]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx reg-fx]]
    [wh.common.graphql-queries :as graphql]
    [wh.db :as db]
    [wh.user.db :as user]
    [wh.util :as util]))

(reg-event-db
  ::logged-out
  db/default-interceptors
  (fn [db _]
    db))

(reg-event-fx
  :user/logout
  db/default-interceptors
  (fn [{db :db} _]
    {:db              (assoc db ::user/sub-db user/default-db)
     :analytics/reset nil
     :navigate        [:homepage]
     :http-xhrio      {:method          :post
                       :uri             "/api/logout"
                       :timeout         5000
                       :format          (ajax-formats/text-request-format)
                       :response-format (ajax-formats/text-response-format)
                       :on-success      [::logged-out]
                       :on-failure      [::logged-out]}}))

(defn user-db->identify
  [{:keys [::user/email ::user/name ::user/visa-status ::user/visa-status-other
           ::user/skills ::user/id ::user/approval
           ::user/salary]}]
  (cond-> {:id                id
           :email             email
           :name              name
           :skills            (mapv :name skills)
           :visa-status       visa-status
           :visa-status-other visa-status-other
           :approval          approval
           :min-salary        (:min salary)
           :currency          (:currency salary)}))

(reg-event-fx
  :user/init
  db/default-interceptors
  (fn [{db :db} []]
    (let [{:keys [id] :as identify-user} (-> (::user/sub-db db)
                            user-db->identify
                            (assoc :board (::db/vertical db))
                            (util/dissoc-selected-keys-if-blank #{:skills :visa-status
                                                                  :visa-status-other
                                                                  :min-salary :currency
                                                                  :approval
                                                                  :id
                                                                  :email :name}))]
      (when id
        {:analytics/identify identify-user}))))

(reg-event-db
  ::save-consent-success
  db/default-interceptors
  (fn [db [{{{consented :consented} :update_user} :data}]]
    (-> db
        (assoc-in [::user/sub-db ::user/saving-consent?] false)
        (assoc-in [::user/sub-db ::user/consented] consented))))

(reg-event-db
  ::save-consent-failure
  db/default-interceptors
  (fn [db _]
    (-> db
        (assoc-in [::user/sub-db ::user/saving-consent?] false)
        (assoc-in [::user/sub-db ::user/save-consent-error?] true))))

(def add-welcome-msg
  {:venia/operation {:operation/type :mutation
                     :operation/name "add_welcome_msg"}
   :venia/variables [{:variable/name "welcome_msg"
                      :variable/type :String!}]
   :venia/queries [[:add_welcome_msg {:welcome_msg :$welcome_msg} [:id]]]})

(reg-event-fx
  ::add-welcome-msg
  db/default-interceptors
  (fn [{db :db} [msg]]
    (if (db/logged-in? db)
      {:graphql {:query      add-welcome-msg
                 :variables  {:welcome_msg msg}
                 :on-success [::add-welcome-msg-success msg]}}
      {:dispatch [::add-welcome-msg-success msg]})))

(reg-event-db
  ::add-welcome-msg-success
  db/default-interceptors
  (fn [db [msg]]
    (update-in db [::user/sub-db ::user/welcome-msgs] conj msg)))

(reg-event-fx
  :user/save-consent
  db/default-interceptors
  (fn [{db :db} _]
    (let [user-id (get-in db [::user/sub-db ::user/id])]
      {:graphql {:query      graphql/update-user-mutation--consent
                 :variables  {:update_user {:id        user-id
                                            :consented (-> (cljs-time.core/now) (time-coerce/to-string))}}
                 :on-success [::save-consent-success]
                 :on-failure [::save-consent-failure]}
       :db      (-> db
                    (assoc-in [::user/sub-db ::user/save-consent-error?] false)
                    (assoc-in [::user/sub-db ::user/saving-consent?] true))})))
