(ns wh.login.github-callback.events
  (:require
    [re-frame.core :refer [reg-event-fx reg-event-db inject-cofx]]
    [wh.common.fx.persistent-state]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.login.github-callback.db :as github-callback]
    [wh.login.shared :as login-shared]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(reg-event-fx
  ::gh-auth-success
  db/default-interceptors
  (fn [{db :db} [{{{:keys [email new] :as user} :githubUserSession} :data}]]
    (let [db (-> db
                 (update ::user/sub-db merge (-> user
                                                 login-shared/initialize-associated-jobs-and-blogs
                                                 util/remove-nils
                                                 user/translate-user))
                 (assoc-in [::github-callback/sub-db :callback-status] :success))
          base-dispatch (cond-> [[::pages/unset-loader]]
                                new (conj [:register/track-account-created {:source :github :email email}])
                                (and new (:register/track-context db)) (conj [:register/track-start (:register/track-context db)]))]
      {:db         db
       :analytics/identify db
       :dispatch-n (into base-dispatch (login/redirect-post-login-or-registration db))})))

(reg-event-fx
  ::gh-auth-fail
  db/default-interceptors
  (fn [{db :db} [{:keys [errors] :as _resp}]]
    {:dispatch [::pages/unset-loader]
     :db       (-> db
                   (assoc-in [::github-callback/sub-db :error] (first errors))
                   (assoc-in [::github-callback/sub-db :callback-status] :fail)
                   (assoc ::db/page :github-callback))}))

(defquery github-auth-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "GithubSession"}
   :venia/variables [{:variable/name "githubCode"
                      :variable/type :String!}]
   :venia/queries   [[:githubUserSession {:githubCode :$githubCode}
                      [:id :name :visaStatus :visaStatusOther
                       [:approval [:status]] :type :email :new :githubId
                       :consented :imageUrl [:skills [:name]]
                       [:githubInfo [:name [:skills [:name]]]]
                       [:twitterInfo [:id]]
                       [:cv [:link
                             [:file [:type :name :url]]]]
                       [:salary [:min :currency]]
                       [:likes [:__typename
                                [:fragment/likedJobId]
                                [:fragment/likedBlogId]]]
                       [:applied [:jobId]]
                       [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]]]})


(reg-event-fx
  ::github-callback
  [(inject-cofx :persistent-state)]
  (fn [cofx [_ code]]
    (let [db (if (empty? (:persistent-state cofx))
               ;; if persistent-state was empty, that means persisting
               ;; state didn't work (e.g. in incognito mode). Let's use
               ;; existing app-db – we lose pre-login context, but better
               ;; that than rendering nothing at all.
               ;; Note that in a company context we don't even need
               ;; persisting app-db.
               (:db cofx)
               (merge (select-keys (:db cofx) [::db/page-mapping])
                      (:persistent-state cofx)))]
      (if (= (get-in db [::github-callback/sub-db :callback-status]) :sent)
        {:db db}
        {:db            (assoc-in db [::github-callback/sub-db :callback-status] :sent)
         :graphql       {:query      github-auth-mutation
                         :variables  {:githubCode code}
                         :on-success [::gh-auth-success]
                         :on-failure [::gh-auth-fail]}
         :persist-state {}}))))

(reg-event-db
  ::init-db
  db/default-interceptors
  (fn [db _]
    (update db ::github-callback/sub-db merge github-callback/default-db)))

(defmethod on-page-load :github-callback [db]
  (let [qp (::db/query-params db)]
    [[::init-db]
     [::github-callback (qp "code")]]))
