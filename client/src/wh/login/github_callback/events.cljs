(ns wh.login.github-callback.events
  (:require
    [re-frame.core :refer [reg-event-fx inject-cofx]]
    [wh.common.fx.persistent-state]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.login.github-callback.db :as github-callback]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(defn continue-to-registration [db base-dispatch user new]
  ;; Even though by the time we're here the register module is not
  ;; loaded, we have an app-db that contains the relevant sub-db,
  ;; because it's been fetched from local storage.
  (let [old-step (get-in db [:wh.register.db/sub-db :wh.register.db/step])
        new-step (case old-step
                   :skills :skills
                   :verify :verify
                   :email)]
    {:db db
     :dispatch-n base-dispatch
     :navigate [:register :params {:step new-step}]}))

(defn complete-login [db base-dispatch]
  {:db         db
   :analytics/identify db
   :dispatch-n (into base-dispatch (login/redirect-post-login-or-registration db))})

(defn- initialize-associated-jobs
  [{:keys [likes applied] :as user}]
  (-> user
      (assoc :liked-jobs (set (map :id likes))
             :applied-jobs (set (map :id applied)))
      (dissoc :likes :applied)))

(reg-event-fx
  ::gh-auth-success
  db/default-interceptors
  (fn [{db :db} [{{{:keys [email new] :as user} :githubUserSession} :data}]]
    (let [db (update db ::user/sub-db #(merge % (-> user
                                                    initialize-associated-jobs
                                                    util/remove-nils
                                                    user/translate-user)))
          base-dispatch (cond-> [[::pages/unset-loader]]
                          new (conj [:register/track-account-created {:source :github :email email}])
                          (and new (:register/track-context db)) (conj [:register/track-start (:register/track-context db)]))]
      (if new
        (continue-to-registration db base-dispatch user new)
        (complete-login db base-dispatch)))))

(reg-event-fx
  ::gh-connect-success
  db/default-interceptors
  (fn [{db :db} [_]]
    {:navigate [:manage-issues]}))

(reg-event-fx
  ::gh-auth-fail
  db/default-interceptors
  (fn [{db :db} [_]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::github-callback/sub-db ::github-callback/github-error?] true)
                   (assoc ::db/page :github-callback))}))

(reg-event-fx
  ::gh-connect-fail
  db/default-interceptors
  (fn [{db :db} [_]]
    {:dispatch [:error/set-global
                "Something went wrong while we tried to connect your GitHub account 😢"
                [:github/call nil :company]]
     :navigate [:edit-company]}))

(defquery github-auth-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "GithubSession"}
   :venia/variables [{:variable/name "githubCode"
                      :variable/type :String!}]
   :venia/queries   [[:githubUserSession {:githubCode :$githubCode}
                      [:id :name :visaStatus :visaStatusOther [:approval [:status]] :type :email :new :githubId
                       :consented
                       [:githubInfo [:name [:skills [:name]]]]
                       [:cv [:link
                             [:file [:type :name :url]]]]
                       [:salary [:min :currency]]
                       [:likes [:id]]
                       [:applied [:id]]
                       [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]]]})

(defquery connect-github-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "ConnectGithub"}
   :venia/variables [{:variable/name "githubCode"
                      :variable/type :String!}]
   :venia/queries   [[:connectGithub {:githubCode :$githubCode}]]})

(defn github-query [code user-type]
  (case user-type
    "company"   {:query      connect-github-mutation
                 :variables  {:githubCode code}
                 :on-success [::gh-connect-success]
                 :on-failure [::gh-connect-fail]}
    "candidate" {:query      github-auth-mutation
                 :variables  {:githubCode code}
                 :on-success [::gh-auth-success]
                 :on-failure [::gh-auth-fail]}))

(reg-event-fx
  ::github-callback
  [(inject-cofx :persistent-state)]
  (fn [cofx [_ code user-type]]
    (let [db (if (or (= user-type "company")
                     (empty? (:persistent-state cofx)))
               ;; if persistent-state was empty, that means persisting
               ;; state didn't work (e.g. in incognito mode). Let's use
               ;; existing app-db – we lose pre-login context, but better
               ;; that than rendering nothing at all.
               ;; Note that in a company context we don't even need
               ;; persisting app-db.
               (:db cofx)
               (merge (select-keys (:db cofx) [::db/page-mapping])
                      (:persistent-state cofx)))]
      {:db            db
       :graphql       (github-query code user-type)
       :persist-state {}})))

(defmethod on-page-load :github-callback [db]
  (let [qp (::db/query-params db)]
    [[::github-callback (qp "code") (qp "user-type")]]))