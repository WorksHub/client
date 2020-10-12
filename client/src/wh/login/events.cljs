(ns wh.login.events
  (:require
    [ajax.formats :refer [text-request-format raw-response-format]]
    [cljs.reader :as r]
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
   :venia/queries [[:create_magic_link {:email :$email :redirect :$redirect}]]})

(reg-event-db
  ::set-email
  login-interceptors
  (fn [db [email]]
    (login/set-email db email)))



(reg-event-fx
  ::send-login-link
  (fn [{db :db} _]
    (let [email (login/email db)]
      ;; NOTE set this condition to `false` if you want to send magic emails in dev
      #_{:navigate [:login :params {:step :email} :query-params {:sent true}]}
      (cond
        (not (login/valid-email? email)) {:db (login/set-error db :invalid-arguments)}
        (login/is-dev-env? db)           {:dispatch [::login-as email]}
        :always                          {:db      (login/set-submitting db)
                                          :graphql {:query      magic-link-mutation
                                                    :variables  {:email    email
                                                                 :redirect (login/redirect-url db)}
                                                    :on-success [::send-login-link-handler true]
                                                    :on-failure [::send-login-link-handler false]}}))))

(reg-event-fx
  ::send-login-link-handler
  (fn [{db :db} [success? resp]]
    (if success?
      {:navigate [:login :params {:step :email} :query-params {:sent true}]
       :db       (login/unset-submitting db)}
      {:db       (-> db
                     login/unset-submitting
                     (login/set-error (util/gql-errors->error-key resp)))
       :dispatch [::pages/unset-loader]})))

(reg-event-fx
  ::go-profile
  db/default-interceptors
  (fn [_ _]
    {:navigate [:profile]}))

;; DEVELOPMENT LOGIN

(reg-event-fx
  ::login-as
  db/default-interceptors
  (fn [{db :db} [email]]
    {:dispatch   [::pages/set-loader]
     :http-xhrio {:method           :post
                  :headers          {"Accept" "application/edn"}
                  :uri              (str (::db/api-server db)
                                         (routes/path :login-as :query-params {"email" email}))
                  :format           (text-request-format)
                  :response-format  (raw-response-format)
                  :with-credentials true
                  :timeout          10000
                  :on-success       [::login-as-handler true]
                  :on-failure       [::login-as-handler false]}}))

(reg-event-fx
  ::login-as-handler
  db/default-interceptors
  (fn [{db :db} [success? response]]
    (if success?
      (let [user-db (r/read-string response)]
        {:db      (update db :wh.user.db/sub-db merge user-db)
         :set-url "/"})
      {:db (login/set-error db :invalid-arguments)})))

;; STACKOVERFLOW

;; copy of integration.stackoverflow/stackoverflow-redirect-url
(defn stackoverflow-redirect-url [url env vertical]
  (let [pr-number (re-find #"-\d+" url)]
    (case env
      :prod  (str "https://" vertical ".works-hub.com/stackoverflow-callback")
      :stage (str "https://works-hub-stage.herokuapp.com/stackoverflow-dispatch/" vertical pr-number)
      :dev   (str "http://" vertical ".localdomain:3449/stackoverflow-callback"))))

(defn stackoverflow-authorize-url
  [client-id env vertical]
  (let [base         "https://stackoverflow.com/oauth"
        redirect-uri (stackoverflow-redirect-url js/window.location.href env vertical)]
    (str base
         "?client_id=" client-id
         "&scope=no_expiry"
         "&redirect_uri=" redirect-uri)))

(reg-event-fx
  ::go-stackoverflow
  db/default-interceptors
  (fn [{db :db} _]
    {:set-url (stackoverflow-authorize-url (:wh.settings/stackoverflow-client-id db)
                                           (:wh.settings/environment db)
                                           (::db/vertical db))}))

(reg-event-fx
  :stackoverflow/call
  db/default-interceptors
  (fn [{db :db} [track-context]]
    {:dispatch-n    [[::pages/set-loader]
                     [::go-stackoverflow]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

;; GITHUB

(defn github-authorize-url
  [client-id env vertical]
  (let [base      (<< "https://github.com/login/oauth/authorize?client_id=~{client-id}&scope=user:email")
        pr-number (re-find #"-\d+" js/window.location.href)]
    (case env
      :prod  (<< "~{base}&redirect_uri=https://functional.works-hub.com/github-dispatch/~{vertical}")
      :stage (<< "~{base}&redirect_uri=https://works-hub-stage.herokuapp.com/github-dispatch/~{vertical}~{pr-number}")
      :dev   (<< "~{base}&redirect_uri=http://functional.localdomain:3449/github-dispatch/~{vertical}"))))

(reg-event-fx
  ::go-github
  db/default-interceptors
  (fn [{db :db} _]
    {:set-url (github-authorize-url (:wh.settings/github-client-id db)
                                    (:wh.settings/environment db)
                                    (::db/vertical db))}))

(reg-event-fx
  :github/call
  db/default-interceptors
  (fn [{db :db} [track-context]]
    {:dispatch-n    [[::pages/set-loader]
                     [::go-github]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

;; TWITTER

(reg-event-fx
  ::go-twitter
  db/default-interceptors
  (fn [{db :db} _]
    {:set-url (routes/path :oauth-twitter)}))

(reg-event-fx
  :twitter/call
  db/default-interceptors
  (fn [{db :db} [track-context]]
    {:dispatch-n    [[::pages/set-loader]
                     [::go-twitter]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

(reg-event-fx
  ::redirect-to-twitter-callback
  db/default-interceptors
  (fn [{db :db}]
    {:navigate [:twitter-callback
                :query-params (::db/query-params db)]}))

(defmethod on-page-load :login
  [db]
  (list
    [:wh.events/scroll-to-top]
    (when (login/is-step? db :github)
      [:github/call {:type :login-page}])
    (when (login/is-step? db :stackoverflow)
      [:stackoverflow/call {:type :login-page}])
    (when (login/is-step? db :twitter)
      [:twitter/call {:type :login-page}])))
