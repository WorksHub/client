(ns wh.login.events
  (:require
    [cljs.reader :as r]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.routes :as routes]
    [wh.util :as util])
  (:require-macros
    [clojure.core.strint :refer [<<]]
    [wh.graphql-macros :refer [defquery]]))

(def login-interceptors (into db/default-interceptors
                              [(path ::login/sub-db)]))

(reg-event-db
  ::initialize-db
  login-interceptors
  (fn [db _]
    (merge login/default-db db)))

(defquery magic-link-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "CreateMagicLink"}
   :venia/variables [{:variable/name "email"
                      :variable/type :String!}
                     {:variable/name "redirect"
                      :variable/type :String}]
   :venia/queries [[:create_magic_link {:email :$email :redirect :$redirect}
                    [:created]]]})

(reg-event-db
  ::set-magic-email
  login-interceptors
  (fn [db [email]]
    (assoc db ::login/magic-email email ::login/magic-status :not-posted)))

(reg-event-fx
  ::send-magic-email
  (fn [{db :db} _]
    (let [email (str/trim (get-in db [::login/sub-db ::login/magic-email]))]
      (if (= (:wh.settings/environment db) :dev)
        {:navigate [:homepage :query-params {"login-as" email}]}
        {:dispatch [::pages/set-loader]
         :graphql {:query magic-link-mutation
                   :variables {:email (str/trim email)
                               :redirect (when-let [path (get-in db [::login/sub-db ::login/redirect])]
                                           (apply routes/path path))}
                   :on-success [::magic-link-handler true]
                   :on-failure [::magic-link-handler false]}}))))

(reg-event-fx
  ::magic-link-handler
  login-interceptors
  (fn [{db :db} [success? resp]]
    {:db (assoc db ::login/magic-status (if success?
                                          :success
                                          (util/gql-errors->error-key resp)))
     :dispatch [::pages/unset-loader]}))

;; Github login

(defn github-authorize-url
  [client-id env vertical user-type]
  (let [scope (if (= user-type :company)
                "admin:org%20public_repo"
                "user:email")
        base (<< "https://github.com/login/oauth/authorize?client_id=~{client-id}&scope=~{scope}")
        pr-number (re-find #"-\d+" js/window.location.href)
        user-type-name (name user-type)]
    (case env
      :prod  (<< "~{base}&redirect_uri=https://functional.works-hub.com/github-dispatch/~{user-type-name}/~{vertical}")
      :stage (<< "~{base}&redirect_uri=https://works-hub-stage.herokuapp.com/github-dispatch/~{user-type-name}/~{vertical}~{pr-number}")
      :dev   (<< "~{base}&redirect_uri=http://functional.localdomain:3449/github-dispatch/~{user-type-name}/~{vertical}"))))

(reg-event-fx
  ::go-github
  db/default-interceptors
  (fn [{db :db} [user-type]]
    {:set-url (github-authorize-url (:wh.settings/github-client-id db)
                                    (:wh.settings/environment db)
                                    (::db/vertical db)
                                    user-type)}))

(reg-event-fx
  :github/call
  db/default-interceptors
  (fn [{db :db} [track-context user-type]]
    (let [user-type (or user-type :candidate)] ; it's called with nil second arg from most candidate contexts :see_no_evil:
      (cond->
        {:dispatch-n [[::pages/set-loader]
                      [::go-github user-type]]}
        (= user-type :candidate)
        (assoc :persist-state (cond-> db
                                      track-context (assoc :register/track-context track-context)
                                      true (assoc ::db/loading? true
                                                  :google/maps-loaded? false) ; reload google maps when we return here
                                      true (dissoc ::db/page-mapping)))))))

(defmethod on-page-load :login
  [db]
  (list
   [:wh.events/scroll-to-top]
   ;; if there's an auth redirect we pop and set it
   (when-let [cached-redirect (js/popAuthRedirect)]
     (vec (concat [:login/set-redirect] (r/read-string cached-redirect))))
   (when (= :github (get-in db [::db/page-params :step]))
     [:github/call :login-page nil])))
