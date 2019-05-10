(ns wh.events
  (:require
    [cljs.reader :as r]
    [com.smxemail.re-frame-cookie-fx]
    [re-frame.core :refer [dispatch reg-fx reg-event-db reg-event-fx reg-cofx inject-cofx]]
    [re-frame.db :refer [app-db]]
    [wh.common.fx]
    [wh.common.url :as url]
    [wh.components.auth-popup.events]
    [wh.components.error.events]
    [wh.db :as db]
    [wh.logged-in.apply.common-events]
    [wh.pages.core :as pages]
    [wh.util :as util])
  (:require-macros
    [clojure.core.strint :refer [<<]]
    [wh.graphql-macros :refer [defquery]]))

;; This is for external URLs only (e.g., GitHub callouts). For
;; internal navigation, see :navigate.
(reg-fx
  :set-url
  (fn [url]
    (set! js/window.location url)))

(reg-event-fx
  :graphql/success
  (fn [_ [_ response]]
    (js/console.log "GraphQL query success:" response)
    {:dispatch [::pages/unset-loader]}))

(reg-event-fx
  :graphql/failure
  (fn [_ [_ response]]
    (js/console.error "GraphQL query failed:" response)
    {:dispatch [::pages/unset-loader]}))

(reg-event-db
  :graphql/version-mismatch
  db/default-interceptors
  (fn [db _]
    (assoc db ::db/version-mismatch true)))

(reg-event-fx
  ::agree-to-tracking-success
  db/default-interceptors
  (fn [{db :db} _]
    {:db             (assoc db ::db/tracking-consent? true)
     :analytics/load nil}))

(reg-event-fx
  ::agree-to-tracking-failure
  (fn [_ _]
    (js/console.error "Failed to set cookie to store tracking agreement." )))

(reg-event-fx
  ::agree-to-tracking
  db/default-interceptors
  (fn [_ _]
    {:cookie/set {:name       (:tracking-consent url/wh-cookie-names)
                  :value      "true"
                  :max-age    31536000                     ;;1 year
                  :on-success [::agree-to-tracking-success]
                  :on-failure [::agree-to-tracking-failure]}
     :analytics/init-tracking nil}))

(reg-event-fx
  :init
  (conj db/default-interceptors (inject-cofx :cookie/get [:wh_tracking_consent :wh_aid]))
  (fn [{{consent :wh_tracking_consent a-id :wh_aid} :cookie/get, db :db} _]
    (when (empty? db)
      (let [tracking-consent? (= "true" consent)
            server-side-db (-> (.getElementById js/document "data-init")
                               (.-text)
                               r/read-string)
            query-params (pages/parse-query-params)
            db (merge
                (db/default-db server-side-db)
                {::db/query-params      query-params
                 ::db/initial-utm-tags  (select-keys query-params ["utm_source" "utm_medium" "utm_campaign" "utm_term" "utm_content"])
                 ::db/initial-referrer  (aget js/document "referrer")
                 ::db/tracking-consent? tracking-consent?})]
        (cond-> {:db         db
                 :dispatch-n [[:user/init]
                              [:wh.pages.router/initialize-page-mapping]]}
                (and tracking-consent? (not a-id)) (assoc :analytics/init-tracking nil)
                tracking-consent? (assoc :analytics/load nil))))))

(reg-event-db
  ::set-initial-load
  db/default-interceptors
  (fn [db [value]]
    (assoc db ::db/initial-load? value)))

(defquery like-job-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "LikeJob"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String!}
                     {:variable/name "add"
                      :variable/type :Boolean!}]
   :venia/queries [[:mark_job {:job_id :$id, :add :$add, :job_mark_action :like}
                    [:marked]]]})

(reg-event-db
  ::toggle-job-like-success
  db/default-interceptors
  (fn [db [id]]
    (update-in db [:wh.user.db/sub-db :wh.user.db/liked-jobs] util/toggle id)))

(reg-event-fx
  ::toggle-job-like
  db/default-interceptors
  (fn [{db :db} [{:keys [id] :as job}]]
    (let [liked (contains? (get-in db [:wh.user.db/sub-db :wh.user.db/liked-jobs]) id)
          event-name (str "Job " (if liked "Removed Like" "Liked"))]
      {:graphql         {:query      like-job-mutation
                         :variables  {:id id, :add (not liked)}
                         :on-success [::toggle-job-like-success id]}
       :analytics/track [event-name job]})))

(defquery blacklist-job-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "BlacklistJob"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String!}]
   :venia/queries [[:mark_job {:job_id :$id, :add true, :job_mark_action :blacklist}
                    [:marked]]]})

;; TODO: move it away
(reg-event-fx
  ::blacklist-job
  db/default-interceptors
  (fn [{db :db} [{:keys [id] :as job} action]]
    {:graphql         {:query      blacklist-job-mutation
                       :variables  {:id id}
                       :on-success (if (= action :reload-recommended)
                                     [:personalised-jobs/fetch-jobs-by-type :recommended 1]
                                     ;; need to refer by full name since it's in another module
                                     [:wh.logged-in.dashboard.events/fetch-recommended-jobs])}
     :analytics/track ["Job Blacklisted" job]}))

(reg-event-fx
  ::nav
  db/default-interceptors
  (fn [{db :db} args]
    {:navigate args}))

(reg-event-fx
 ::contribute
 db/default-interceptors
 (fn [{db :db} _]
   {:dispatch-n [[:auth/show-popup {:type :homepage-contribute}]]}))

(reg-event-fx
  :register/get-started
  db/default-interceptors
  (fn [{db :db} [track-data]]
    (merge {:navigate [:register :params {:step :email}]}
           {:dispatch-n (cond-> []
                          (contains? db :wh.register.db/sub-db) (conj [:wh.register.events/initialize-db true])
                          track-data (conj [:register/track-start track-data]))})))

;; Defined here rather than in wh.register.events because we need to trigger this before loading register module.
(reg-event-fx
  :register/track-start
  db/default-interceptors
  (fn [_ [context]]
    {:analytics/track ["Register Button Clicked" context]}))

(reg-event-fx
  :register/track-account-created
  db/default-interceptors
  (fn [_ [data]]
    {:analytics/track ["Account Created" data]
     :reddit/conversion-pixel nil}))

(reg-event-fx
  :wh.events/scroll-to-top
  db/default-interceptors
  (fn [_ _]
    {:scroll-to-top true}))

(reg-event-db ; homepage needs it
  :login/set-redirect
  db/default-interceptors
  (fn [db [& path]]
    (assoc-in db [:wh.login.db/sub-db :wh.login.db/redirect] path)))

(defn user-db->identify
  [{:keys [:wh.user.db/email :wh.user.db/name :wh.user.db/visa-status :wh.user.db/visa-status-other
           :wh.user.db/skills :wh.user.db/id :wh.user.db/approval
           :wh.user.db/salary]}]
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
    (let [{:keys [id] :as identify-user} (-> (:wh.user.db/sub-db db)
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
