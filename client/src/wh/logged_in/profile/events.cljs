(ns wh.logged-in.profile.events
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.data :as data]
            [wh.common.errors :as errors]
            [wh.common.graphql-queries :as graphql]
            [wh.common.issue :as common-issue]
            [wh.common.specs.primitives :as specs]
            [wh.common.upload :as upload]
            [wh.common.user :as common-user]
            [wh.components.forms.events :as form-events]
            [wh.db :as db]
            [wh.logged-in.profile.db :as profile]
            [wh.logged-in.profile.location-events :as location-events]
            [wh.pages.core :refer [on-page-load] :as pages]
            [wh.user.db :as user]
            [wh.util :as util]))

(def profile-interceptors (into db/default-interceptors
                                [(path ::profile/sub-db)]))

(def user-interceptors (into db/default-interceptors
                             [(path ::user/sub-db)]))

(reg-event-db
  ::initialize-db
  profile-interceptors
  (fn [db _]
    (merge db profile/default-db)))

;; FIXME: this is a mess. It is not strictly needed (::user/db is set
;; by server initially), but due to the way profile forms are
;; currently implemented, we need to reload ::user/db on cancel.

;; We need to refactor forms in such a way that they will edit a
;; composite data structure. This will result in **massive**
;; simplification of this code.

;; TODO: convert to defquery, CH4692
(def initial-data-query
  {:venia/operation {:operation/type :query
                     :operation/name "profile"}
   :venia/variables [{:variable/name "user_id" :variable/type :ID}]
   :venia/queries   [[:me [[:skills [:name :rating]]
                           [:companyPerks [:name]]
                           [:otherUrls [:url]]
                           :imageUrl
                           :id :githubId
                           :phone
                           :published
                           :name :summary
                           :email :jobSeekingStatus :roleTypes
                           :visaStatus :visaStatusOther
                           :remote
                           :percentile
                           :created
                           :lastSeen
                           :updated
                           [:cv [:link
                                 [:file [:type :name :url :hash]]]]
                           [:coverLetter [:link
                                          [:file [:type :name :url :hash]]]]
                           [:salary [:currency :min :timePeriod]]
                           [:currentLocation [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]
                           [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]
                           [:stackoverflowInfo [:reputation]]
                           [:twitterInfo [:id]]]]
                     [:blogs {:filter_type "mine"} [[:blogs [:id :title :formattedCreationDate :readingTime
                                                             :upvoteCount :published]]]]
                     [:query_issues {:user_id :$user_id}
                      [[:issues [:id :title :level
                                 [:compensation [:amount :currency]]
                                 [:company [:id :name :logo :slug]]
                                 [:repo [:primary_language]]]]]]]})

(reg-event-fx
  ::fetch-initial-data
  (fn [{db :db} _]
    {:graphql  {:query      initial-data-query
                :variables  {:user_id (user/id db)}
                :on-success [::fetch-initial-data-success]}
     :dispatch [::pages/set-loader]}))



(defn init-avatar [profile]
  (let [predefined-avatar (common-user/url->predefined-avatar (::profile/image-url profile))]
    (merge profile
           {::profile/predefined-avatar  (or predefined-avatar 1)
            ::profile/custom-avatar-mode (and (::profile/image-url profile)
                                              (not predefined-avatar))})))

(defn profile-data [user blogs issues]
  (let [predefined-avatar (common-user/url->predefined-avatar (:image-url user))]
    {::profile/predefined-avatar  (or predefined-avatar 1)
     ::profile/custom-avatar-mode (not predefined-avatar)
     ::profile/contributions      (mapv cases/->kebab-case blogs)
     ::profile/issues             (->> issues
                                       (map cases/->kebab-case issues)
                                       (mapv common-issue/gql-issue->issue))}))


(reg-event-fx
  ::fetch-initial-data-success
  db/default-interceptors
  (fn [{db :db} [{{:keys [me blogs query_issues]} :data}]]
    (let [issues (:issues query_issues)
          user (user/translate-user me)]
      {:db       (merge-with merge
                             db
                             {::user/sub-db    user
                              ::profile/sub-db (merge (profile-data user (:blogs blogs) issues)
                                                      (profile/->sub-db user))})
       :dispatch [::pages/unset-loader]})))

(reg-event-db
  ::init-profile-edit
  db/default-interceptors
  (fn [db _]
    (-> db
        (update ::profile/sub-db merge (profile/->sub-db (::user/sub-db db)))
        (update ::profile/sub-db init-avatar))))

(defn settings-from-query-params [db]
  (let [query-params (::db/query-params db)
        matching-params (select-keys query-params ["job-seeking-status"])]
    (into {}
          (map (fn [[k v]] [(keyword k) (data/job-seeking-status->name v)]))
          matching-params)))

(defmethod on-page-load :profile [db]
  [[::fetch-initial-data]
   [::pages/clear-errors]
   (when (seq (settings-from-query-params db))
     [::save-settings-from-url])])

(defmethod on-page-load :profile-edit-header [db]
  [[::init-profile-edit]])

(defmethod on-page-load :profile-edit-cv [db]
  [[::init-profile-edit]])

(defmethod on-page-load :profile-edit-private [db]
  [[::init-profile-edit]])

(defmethod on-page-load :candidate-edit-header [db]
  [[:wh.company.candidate.events/load-candidate]])

(defmethod on-page-load :candidate-edit-cv [db]
  [[:wh.company.candidate.events/load-candidate]])

(defmethod on-page-load :candidate-edit-private [db]
  [[:wh.company.candidate.events/load-candidate]])

(defmethod on-page-load :improve-recommendations [db]
  [[::fetch-initial-data]
   [::init-profile-edit]])

(defmethod on-page-load :profile-edit-company-user [db]
  [[::init-profile-edit]])

(reg-event-db
  ::edit-name
  profile-interceptors
  (fn [db [name]]
    (assoc db ::profile/name name)))

(reg-event-db
  ::edit-phone
  profile-interceptors
  (fn [db [phone]]
    (assoc db ::profile/phone phone)))

(reg-event-db
  ::edit-summary
  profile-interceptors
  (fn [db [summary]]
    (assoc db ::profile/summary summary)))

(reg-event-db
  ::edit-email
  profile-interceptors
  (fn [db [email]]
    (assoc db ::profile/email email)))

(reg-event-db
  ::edit-visa-status-other
  profile-interceptors
  (fn [db [status]]
    (assoc db ::profile/visa-status-other status)))

(reg-event-db
  ::edit-salary-min
  profile-interceptors
  (fn [db [value]]
    (assoc-in db [::profile/salary :min] value)))

(reg-event-db
  ::edit-salary-currency
  profile-interceptors
  (fn [db [currency]]
    (assoc-in db [::profile/salary :currency] currency)))

(reg-event-db
  ::edit-salary-time-period
  profile-interceptors
  (fn [db [time-period]]
    (assoc-in db [::profile/salary :time-period] time-period)))

(reg-event-db
  ::edit-job-seeking-status
  profile-interceptors
  (fn [db [status]]
    (assoc db ::profile/job-seeking-status status)))

(reg-event-db
  ::edit-url
  profile-interceptors
  (form-events/multi-edit-fn ::profile/other-urls :url))

(reg-event-db
  ::edit-perk
  profile-interceptors
  (form-events/multi-edit-fn ::profile/company-perks :name))

(reg-event-db
  ::edit-skill
  profile-interceptors
  (form-events/multi-edit-fn ::profile/skills :name))

(reg-event-db
  ::rate-skill
  profile-interceptors
  (fn [db [i rating]]
    (let [old-skills (vec (::profile/skills db))
          new-skills (cond-> old-skills
                             (= i (count old-skills)) (conj {:name ""})
                             true                     (assoc-in [i :rating] rating))]
      (assoc db ::profile/skills new-skills))))

(defn graphql-header-update
  [profile]
  (-> profile
      (select-keys [::profile/id ::profile/name ::profile/skills
                    ::profile/summary ::profile/other-urls ::profile/image-url])
      util/transform-keys))

(defn graphql-company-user-update
  [db]
  (-> (::profile/sub-db db)
      (select-keys [::profile/id ::profile/email ::profile/name])
      (util/transform-keys)))

(defn graphql-private-update
  [db]
  (-> (::profile/sub-db db)
      (select-keys [::profile/id ::profile/preferred-locations ::profile/email ::profile/job-seeking-status
                    ::profile/company-perks ::profile/visa-status ::profile/visa-status-other ::profile/role-types
                    ::profile/salary ::profile/remote ::profile/current-location ::profile/phone])
      (update ::profile/preferred-locations (partial filterv map?))
      (util/transform-keys)))

(defn graphql-recommendations-update
  [db]
  (-> (::profile/sub-db db)
      (select-keys [::profile/id ::profile/preferred-locations ::profile/skills ::profile/remote])
      (update ::profile/preferred-locations (partial filterv map?))
      (util/transform-keys)))

(defn user-private-update
  [profile]
  (as-> profile data
        (util/strip-ns-from-map-keys data)
        (select-keys data [:email :visa-status :visa-status-other :salary])
        (util/namespace-map "wh.user.db" data)))

(defn graphql-cv-update
  [db]
  (-> {:id (or (get-in db [::profile/sub-db ::profile/id])
               (get-in db [::user/sub-db ::user/id]))
       :cv (get-in db [::profile/sub-db ::profile/cv])}
      (util/transform-keys)))

(defn graphql-cover-letter-update
  [db]
  (-> {:id           (or (get-in db [::profile/sub-db ::profile/id])
                         (get-in db [::user/sub-db ::user/id]))
       :cover-letter (get-in db [::profile/sub-db ::profile/cover-letter])}
      (util/transform-keys)))

(defn validate-profile
  "Returns nil if valid, an error message if invalid.
  Quick-and-dirty but will do for now."
  [profile]
  (cond
    (not (common-user/full-name? (::profile/name profile))) "Please fill in your name (at least two words)"
    (str/blank? (::profile/email profile)) "Please provide your email."
    (->> (::profile/other-urls profile)
         (map :url)
         (some #(not (s/valid? ::specs/url %)))) "One (or more) of the website links is invalid. Please amend and try again."))

(reg-event-fx
  ::save-header
  db/default-interceptors
  (fn [{db :db} _]
    (let [profile (::profile/sub-db db)]
      (if-let [error (validate-profile profile)]
        {:dispatch [::pages/set-error error]}
        (let [profile (if (get-in db [::profile/sub-db ::profile/custom-avatar-mode])
                        profile
                        (assoc profile ::profile/image-url
                                       (common-user/avatar-url (get-in db [::profile/sub-db ::profile/predefined-avatar]))))
              new-db (assoc db ::profile/sub-db profile)]
          {:db       new-db
           :graphql  {:query      graphql/update-user-mutation--approval
                      :variables  {:update_user (graphql-header-update profile)}
                      :on-success [::save-success]
                      :on-failure [::save-failure]}
           :dispatch [::pages/set-loader]})))))

(reg-event-fx
  ::save-private
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql  {:query      graphql/update-user-mutation--approval
                :variables  {:update_user (-> db
                                              (graphql-private-update)
                                              (util/dissoc-if-empty :salary))}
                :on-success [::save-success]
                :on-failure [::save-failure]}
     :dispatch [::pages/set-loader]}))

(defn graphql-settings-from-url
  [db]
  (-> db
      settings-from-query-params
      (assoc :id (get-in db [::user/sub-db ::user/id]))
      util/transform-keys))

(reg-event-fx
  ::save-settings-from-url-success
  profile-interceptors
  (fn [{db :db} _]
    {:db       (assoc db ::profile/url-save-success true)
     :dispatch [::save-success]}))

(reg-event-db
  ::clear-url-save-success
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/url-save-success false)))

(reg-event-fx
  ::save-settings-from-url
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql  {:query      graphql/update-user-mutation--approval
                :variables  {:update_user (graphql-settings-from-url db)}
                :on-success [::save-settings-from-url-success]
                :on-failure [::save-failure]}
     :dispatch [::pages/set-loader]}))

(reg-event-fx
  ::save-company-user
  db/default-interceptors
  (fn [{db :db} _]
    (if-let [error (validate-profile (::profile/sub-db db))]
      {:dispatch [::pages/set-error error]}
      {:graphql  {:query      graphql/update-user-mutation--approval
                  :variables  {:update_user (graphql-company-user-update db)}
                  :on-success [::save-success]
                  :on-failure [::save-failure]}
       :db       (update db ::user/sub-db merge (user-private-update (::profile/sub-db db)))
       :dispatch [::pages/set-loader]})))

(reg-event-fx
  ::save-recommendations
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql  {:query      graphql/update-user-mutation--approval
                :variables  {:update_user (graphql-recommendations-update db)}
                :on-success [::save-success [:recommended]]
                :on-failure [::save-failure]}
     :db       (update db ::user/sub-db merge (user-private-update (::profile/sub-db db)))
     :dispatch [::pages/set-loader]}))

(reg-event-fx
  ::save-cv-info
  db/default-interceptors
  (fn [{db :db} [{:keys [type]}]]
    (let [url-path [::profile/sub-db ::profile/cv :link]
          cv-link (get-in db url-path)
          valid-cv-link? (or (= type :upload-cv)
                             (s/valid? ::specs/url cv-link))]
      (if valid-cv-link?
        {:graphql  {:query      graphql/update-user-mutation--approval
                    :variables  {:update_user (graphql-cv-update db)}
                    :on-success [::save-success]
                    :on-failure [::save-failure]}
         :dispatch-n [[::pages/set-loader]
                      [:error/close-global]]}

        {:dispatch
         [:error/set-global "CV link is not valid. Please amend and try again."]}))))


(reg-event-fx
 ::save-cover-letter-info
 db/default-interceptors
 (fn [{db :db} _]
   {:graphql    {:query      graphql/update-user-mutation--approval
                 :variables  {:update_user (graphql-cover-letter-update db)}
                 :on-success [::save-success]
                 :on-failure [::save-failure]}
    :dispatch-n [[::pages/set-loader]
                 [:error/close-global]]}))

(reg-event-fx
  ::save-success
  db/default-interceptors
  (fn [{db :db} res]
    {:dispatch [::pages/clear-errors]
     :navigate (cond (> (count res) 1)
                     (first res)

                     (contains? #{:candidate :candidate-edit-header
                                  :candidate-edit-cv :candidate-edit-private}
                                (:wh.db/page db))
                     [:candidate :params (:wh.db/page-params db)]

                     :else [:profile])}))

(reg-event-fx
  ::save-failure
  user-interceptors
  (fn [{db :db} [resp]]
    (let [error-key (util/gql-errors->error-key resp)]
      {:dispatch-n [(if (= error-key :account-with-email-exists)
                      [::pages/set-error (str "Account with email: " (::user/email db) " already exists.")]
                      [::pages/set-error "An error occurred while saving your data."])
                    [::pages/unset-loader]]})))

(reg-event-fx
 ::removal-success
 db/default-interceptors
 (fn [{db :db} _res]
   {:dispatch [::pages/clear-errors]
    :navigate [:profile]}))

(reg-event-fx
 ::removal-failure
 user-interceptors
 (fn [{db :db} _res]
   {:dispatch-n [[::pages/set-error "An error occurred while deleting your data."]
                 [::pages/unset-loader]]}))


(reg-event-db
  ::set-predefined-avatar
  profile-interceptors
  (fn [db [i]]
    (assoc db ::profile/predefined-avatar i)))

(reg-event-db
  ::set-custom-avatar-mode
  profile-interceptors
  (fn [db [custom]]
    (assoc db ::profile/custom-avatar-mode custom)))

(reg-event-fx
  ::image-upload
  db/default-interceptors
  upload/image-upload-fn)

(reg-event-fx
  ::cv-upload
  db/default-interceptors
  upload/cv-upload-fn)

(reg-event-db
  ::toggle-visa-status
  profile-interceptors
  (fn [db [status]]
    (update db ::profile/visa-status util/toggle status)))

(reg-event-db
  ::toggle-role-type
  profile-interceptors
  (fn [db [role-type]]
    (update db ::profile/role-types util/toggle role-type)))

(reg-event-db
  ::avatar-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/avatar-uploading? true)))

(reg-event-db
  ::avatar-upload-success
  profile-interceptors
  (fn [db [filename {:keys [url]}]]
    (assoc db
           ::profile/image-url url
           ::profile/avatar-uploading? false)))

(reg-event-fx
  ::avatar-upload-failure
  profile-interceptors
  (fn [{db :db} [resp]]
    {:db       (assoc db ::profile/avatar-uploading? false)
     :dispatch [:error/set-global (errors/image-upload-error-message (:status resp))]}))

(reg-event-fx
 ::cover-letter-upload
 db/default-interceptors
 upload/cover-letter-upload-fn)

(reg-event-db
  ::cover-letter-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/cover-letter-uploading? true)))

(reg-event-fx
  ::cover-letter-upload-success
  db/default-interceptors
  (fn [{db :db} data]
    (let [[filename {:keys [url hash]}] data]
      {:db       (-> db
                     (assoc-in [::profile/sub-db ::profile/cover-letter :file] {:url url :name filename :hash hash})
                     (assoc-in [::profile/sub-db ::profile/cover-letter-uploading?] false))
       :dispatch [::save-cover-letter-info {:type :upload-cover-letter}]})))

(reg-event-fx
  ::cover-letter-upload-failure
  profile-interceptors
  (fn [{db :db} [error]]
    {:db       (assoc db ::profile/cover-letter-uploading? false)
     :dispatch [::pages/set-error (case (:status error)
                                    413 "Your cover letter is too large, please upload a smaller file (less than 5 MB)."
                                    400 "Your cover letter is empty, please try again with a filled out cover letter."
                                    "There was an error uploading your cover letter, please try a smaller file (less than 5 MB).")]}))


(reg-event-db
  ::cv-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/cv-uploading? true)))

(reg-event-fx
  ::cv-upload-success
  db/default-interceptors
  (fn [{db :db} [filename {:keys [url hash]}]]
    {:db       (-> db
                   (assoc-in [::profile/sub-db ::profile/cv :file] {:url url :name filename :hash hash})
                   (assoc-in [::profile/sub-db ::profile/cv-uploading?] false))
     :dispatch [::save-cv-info {:type :upload-cv}]}))

(reg-event-fx
  ::cv-upload-failure
  profile-interceptors
  (fn [{db :db} [error]]
    {:db       (assoc db ::profile/cv-uploading? false)
     :dispatch [::pages/set-error (case (:status error)
                                    413 "Your CV is too large, please upload a smaller file (less than 5 MB)."
                                    400 "Your CV is empty, please try again with a filled out cv."
                                    "There was an error uploading your CV, please try a smaller file (less than 5 MB).")]}))

(reg-event-fx
  ::location-search-success
  profile-interceptors
  (fn [{db :db} [i result]]
    {:db (assoc-in db [::profile/location-suggestions i] result)}))

(reg-event-fx
  ::current-location-search-success
  profile-interceptors
  (fn [{db :db} [result]]
    {:db (assoc db ::profile/current-location-suggestions result)}))

(reg-event-fx
  ::location-search-failure
  (fn [_ _]
    {}))

(reg-event-fx
  ::remove-preferred-location
  profile-interceptors
  (fn [{profile :db} [i]]
    {:db (update profile ::profile/preferred-locations #(vec (util/drop-ith i %)))}))

(reg-event-fx
  ::edit-current-location
  profile-interceptors
  (fn [{profile :db} [loc]]
    {:db       (assoc profile ::profile/current-location-text loc)
     :dispatch [::location-events/search {:query      loc
                                          :on-success [::current-location-search-success]
                                          :on-failure [::location-search-failure]}]}))

;; Look what's happening here. While typing a location query, we
;; temporarily override an element of preferred-locations (which
;; normally contains location maps) with a query string. Later on,
;; while saving, we filter out non-map values. This means that once
;; you start typing a preferred location query, your old selection
;; at that place is forgotten. Why do we do this?
;;
;; Short answer: to enable deletion.
;;
;; Longer answer: Our current text-field implementation doesn't
;; support the action "I don't want any of the suggestions", i.e.,
;; firing [::select-suggestion nil]. If we didn't do this, it would
;; become impossible to remove a preferred location once you've added
;; it.

(reg-event-fx
  ::edit-preferred-location
  profile-interceptors
  (fn [{profile :db} [i loc]]
    {:db       (assoc-in profile [::profile/preferred-locations i] loc)
     :dispatch [::location-events/search {:query      loc
                                          :on-success [::location-search-success i]
                                          :on-failure [::location-search-failure]}]}))

(reg-event-db
  ::set-remote
  profile-interceptors
  (fn [db [remote]]
    (assoc db ::profile/remote remote)))

(reg-event-db
  ::edit-cv-link
  profile-interceptors
  (fn [db [link]]
    (assoc-in db [::profile/cv :link] link)))

(reg-event-db
  ::select-suggestion
  profile-interceptors
  (fn [profile [i item]]
    (-> profile
        (assoc-in [::profile/preferred-locations i] item)
        (assoc-in [::profile/location-suggestions i] nil))))

(reg-event-db
  ::select-current-location-suggestion
  profile-interceptors
  (fn [profile [item]]
    (assoc profile
           ::profile/current-location item
           ::profile/current-location-text nil
           ::profile/current-location-suggestions nil)))

(def remove-default-cover-letter-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "remove_default_cover_letter"}
   :venia/variables [{:variable/name "user_id"           :variable/type :ID!}
                     {:variable/name "cover_letter_hash" :variable/type :String!}]
   :venia/queries   [[:remove_default_cover_letter
                      {:user_id :$user_id
                       :hash    :$cover_letter_hash}
                      [:status]]]})

(reg-event-fx
 ::remove-cover-letter
 db/default-interceptors
 (fn [{db :db} _]
   (let [user-id      (get-in db [:wh.user.db/sub-db :wh.user.db/id])
         cover-letter (get-in db [::profile/sub-db ::profile/cover-letter])
         hash         (get-in cover-letter [:file :hash])]
     {:graphql    {:query      remove-default-cover-letter-mutation
                   :variables  {:user_id           user-id
                                :cover_letter_hash hash}
                   :on-success [::removal-success]
                   :on-failure [::removal-failure]}
      :dispatch-n [[::pages/set-loader]
                   [:error/close-global]]})))

(reg-event-fx
  ::toggle-profile-visibility-handle
  db/default-interceptors
  (fn [{db :db} [success? _]]
    (if success?
      {:db (user/toggle-published db)
       :dispatch [::pages/clear-errors]}
      {:dispatch [::pages/set-error "An error occurred while changing visibility of your profile"]})))

(reg-event-fx
  ::toggle-profile-visibility
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql    {:query      graphql/update-user-mutation--published
                  :variables  {:update_user {:id (user/id db)
                                             :published (not (user/published? db))}}
                  :on-success [::toggle-profile-visibility-handle true]
                  :on-failure [::toggle-profile-visibility-handle false]}
     :dispatch [:error/close-global]}))
