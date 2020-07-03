(ns wh.login.events
  (:require
    [ajax.formats :refer [text-request-format raw-response-format]]
    [bidi.bidi :as bidi]
    [cljs.reader :as r]
    [clojure.string :as str]
    [goog.Uri :as uri]
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
  ::set-magic-email
  login-interceptors
  (fn [db [email]]
    (assoc db ::login/magic-email email ::login/magic-status :not-posted)))

(reg-event-fx
  ::send-magic-email
  (fn [{db :db} _]
    (let [email (str/trim (get-in db [::login/sub-db ::login/magic-email]))]
      ;; NOTE set this condition to `false` if you want to send magic emails in dev
      (if (= (:wh.settings/environment db) :dev)
        {:dispatch [::login-as email]}
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
  [client-id env vertical]
  (let [base (<< "https://github.com/login/oauth/authorize?client_id=~{client-id}&scope=user:email")
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
    {:dispatch-n [[::pages/set-loader]
                  [::go-github]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

;; copy of integration.stackoverflow/stackoverflow-redirect-url
(defn stackoverflow-redirect-url [url env vertical]
  (let [pr-number (re-find #"-\d+" url)]
    (case env
      :prod  (str "https://" vertical ".works-hub.com/stackoverflow-callback")
      :stage (str "https://works-hub-stage.herokuapp.com/stackoverflow-dispatch/" vertical pr-number)
      :dev   (str "http://" vertical ".localdomain:3449/stackoverflow-callback"))))

(defn stackoverflow-authorize-url
  [client-id env vertical]
  (let [base "https://stackoverflow.com/oauth"
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
  ::go-profile
  db/default-interceptors
  (fn [_ _]
    {:navigate [:profile]}))

(reg-event-fx
  :stackoverflow/call
  db/default-interceptors
  (fn [{db :db} [track-context]]
    {:dispatch-n [[::pages/set-loader]
                  [::go-stackoverflow]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

(reg-event-fx
  ::go-twitter
  db/default-interceptors
  (fn [{db :db} _]
    {:set-url (routes/path :oauth-twitter)}))

(reg-event-fx
  :twitter/call
  db/default-interceptors
  (fn [{db :db} [track-context]]
    {:dispatch-n [[::pages/set-loader]
                  [::go-twitter]]
     :persist-state (cond-> db
                            track-context (assoc :register/track-context track-context)
                            true (assoc ::db/loading? true
                                        :google/maps-loaded? false) ; reload google maps when we return here
                            true (dissoc ::db/page-mapping))}))

(reg-event-fx
  ::login-as-success
  db/default-interceptors
  (fn [{db :db} [response]]
    (let [user-db (r/read-string response)]
      {:db      (update db :wh.user.db/sub-db merge user-db)
       :set-url "/"})))

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
                  :on-success       [::login-as-success]}}))

(defn query-redirect->path
  [query-redirect]
  (when-let [route (->> query-redirect
                        (bidi/url-decode)
                        (bidi/match-route routes/routes))]
    (let [query-params (let [params (-> query-redirect uri/parse .getQueryData)]
                         (zipmap (.getKeys params) (.getValues params)))]
      ;; url params can be 'route-params' or just 'params' depending on how
      ;; they are specified in routes (see ':company' vs ':payment-setup')
      (cond-> (vector (:handler route))
              (or (not-empty (:route-params route))
                  (not-empty (:params route)))
              (concat [:params (merge (:route-params route)
                                      (:params route))])
              (not-empty query-params)
              (concat [:query-params query-params])))))

(reg-event-fx
  ::redirect-to-twitter-callback
  db/default-interceptors
  (fn [{db :db}]
    {:navigate   [:twitter-callback
                  :query-params (::db/query-params db)]}))

(defmethod on-page-load :login
  [db]
  (list
    [:wh.events/scroll-to-top]
    ;; if there's an auth redirect we pop and set it
    (when-let [cached-redirect (js/popAuthRedirect)]
      (vec (concat [:login/set-redirect] (r/read-string cached-redirect))))
    ;; if there's a redirect query param
    (when-let [query-redirect (get-in db [:wh.db/query-params "redirect"])]
      (vec (concat [:login/set-redirect] (query-redirect->path query-redirect))))
    (when (= :github (get-in db [::db/page-params :step]))
      [:github/call {:type :login-page}])
    (when (= :stackoverflow (get-in db [::db/page-params :step]))
      [:stackoverflow/call {:type :login-page}])
    (when (= :twitter (get-in db [::db/page-params :step]))
      [:twitter/call {:type :login-page}])))
