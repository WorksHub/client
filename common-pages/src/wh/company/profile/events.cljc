(ns wh.company.profile.events
  (:require
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    #?(:cljs [wh.user.db :as user])
    #?(:cljs [goog.Uri :as uri])
    #?(:cljs [ajax.json :as ajax-json])
    #?(:cljs [wh.common.fx.google-maps :as google-maps])
    #?(:cljs [wh.common.location :as location])
    #?(:cljs [cljs.spec.alpha :as s]
       :clj  [clojure.spec.alpha :as s])
    [re-frame.core :refer [path]]
    [wh.company.profile.db :as profile]
    [wh.db :as db]
    [wh.components.tag :as tag]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.company :refer [update-company-mutation update-company-mutation-with-fields publish-company-profile-mutation]]
    [wh.graphql.jobs]
    [wh.graphql.tag :refer [tag-query]]
    [wh.graphql.tag :as tag-gql]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.errors :as errors]
    [wh.util :as util]
    [clojure.string :as str])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(def profile-interceptors (into db/default-interceptors
                                [(path ::profile/sub-db)]))

(defquery fetch-company-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}]
   :venia/queries [[:company {:slug :$slug}
                    [:id :slug :name :logo :profileEnabled :descriptionHtml :size
                     :foundedYear :howWeWork :additionalTechInfo :hasPublishedProfile
                     [:techScales [:testing :ops :timeToDeploy]]
                     [:locations [:city :country :countryCode :region :subRegion :state]]
                     [:tags [:id :type :label :slug :subtype]]
                     [:videos [:youtubeId :thumbnail :description]]
                     [:images [:url :width :height]]]]]})

(defquery fetch-company-blogs-and-issues-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}]
   :venia/queries [[:company {:slug :$slug}
                    [[:blogs {:pageSize 2 :pageNumber 1}
                      [[:blogs
                        [:id :title :feature :author :formattedCreationDate :readingTime
                         :upvoteCount :tags :creator :published]]
                       [:pagination [:total]]]]
                     [:jobs {:pageSize 2 :pageNumber 1}
                      [[:jobs
                        [:fragment/jobCardFields]]
                       [:pagination [:total]]]]
                     [:issues {:pageSize 2 :pageNumber 1 :published true}
                      [[:issues
                        [:id :url :number :body :title :pr_count :level :status :published :created_at
                         [:compensation [:amount :currency]]
                         [:contributors [:id]]
                         [:labels [:name]]
                         [:repo [:name :owner :primary_language]]]]
                       [:pagination [:total]]]]
                     [:repos {:pageSize 10 :pageNumber 1}
                      [[:repos
                        [:github_id :name :description :primary_language :owner]]]]]]]})

(defquery fetch-all-company-jobs-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "slug"
                      :variable/type :String!}
                     {:variable/name "total"
                      :variable/type :Int!}]
   :venia/queries [[:company {:slug :$slug}
                    [[:jobs {:pageSize :$total :pageNumber 1}
                      [[:jobs
                        [:fragment/jobCardFields]]
                       [:pagination [:total]]]]]]]})

(defquery analytics-query
  {:venia/operation {:operation/name "jobAnalytics"
                     :operation/type :query}
   :venia/variables [{:variable/name "company_id"
                      :variable/type :ID}]
   :venia/queries [[:job_analytics {:company_id :$company_id
                                    :granularity 0
                                    :num_periods 0}
                    [:granularity
                     [:applications [:date :count]]
                     [:profileViews [:date :count]]]]]})

(reg-query :company fetch-company-query)
(reg-query :company-issues-and-blogs fetch-company-blogs-and-issues-query)
(reg-query :all-company-jobs fetch-all-company-jobs-query)
(reg-query :company-stats analytics-query)

(defn company-slug
  [db]
  (get-in db [:wh.db/page-params :slug]))

(defn initial-query [db]
  [:company {:slug (company-slug db)}])

(defn extra-data-query [db]
  [:company-issues-and-blogs {:slug (get-in db [:wh.db/page-params :slug])}])

(defn all-jobs-query [db total-jobs]
  [:all-company-jobs {:slug (get-in db [:wh.db/page-params :slug])
                      :total total-jobs}])

(defn company-stats-query [company-id]
  [:company-stats {:company_id company-id}])

(defn cached-company
  [db]
  (->> (cache/result db :company {:slug (company-slug db)})
       :company
       (profile/->company)))

(defn cached-company-extra-data
  [db]
  (->> (cache/result db :company-issues-and-blogs {:slug (company-slug db)})
       :company))

#?(:cljs
   (defmethod on-page-load :company [db]
     (list (into [:graphql/query] (conj (initial-query db) {:on-success [::fetch-stats]}) )
           [::load-photoswipe]
           [:google/load-maps]
           (into [:graphql/query] (extra-data-query db))
           (when (or (user/admin? db)
                     (user/owner? db (:id (cached-company db)))
                     (user/owner-by-slug? db (company-slug db)))
             [::fetch-all-tags])))) ;; TODO for now we get all tags and filter client-side

#?(:cljs
   (reg-event-fx
     ::load-photoswipe
     db/default-interceptors
     (fn [_ _]
       (when-not (exists? js/PhotoSwipe)
         (js/loadJavaScript "pswp/photoswipe.min.js")
         (js/loadJavaScript "pswp/photoswipe-ui-default.min.js")))))

#?(:cljs
   (reg-event-fx
     ::photo-upload
     db/default-interceptors
     upload/image-upload-fn))

#?(:cljs
   (reg-event-fx
     ::fetch-stats
     db/default-interceptors
     (fn [{db :db} _]
       {:dispatch (into [:graphql/query] (-> db
                                             (cached-company)
                                             :id
                                             (company-stats-query )))})))

(reg-event-db
  ::photo-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/photo-uploading? true)))

(reg-event-fx
  ::photo-upload-success
  profile-interceptors
  (fn [{db :db} [_ resp]]
    {:dispatch [::add-photo resp]}))

(reg-event-fx
  ::photo-upload-failure
  profile-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::profile/photo-uploading? false)
     :dispatch [:error/set-global "There was an error adding the photo"]}))

(reg-event-fx
  ::add-photo
  db/default-interceptors
  (fn [{db :db} [image]]
    (let [company (cached-company db)]
      {:db (assoc-in db [::profile/sub-db ::profile/photo-uploading?] false)
       :graphql {:query (update-company-mutation-with-fields [[:images [:url :width :height]] :id :slug])
                 :variables {:update_company
                             {:id (:id company)
                              :images (conj (:images company) (select-keys image [:url :width :height]))}}
                 :on-success [::add-photo-success]
                 :on-failure [::photo-upload-failure]}})))

(reg-event-fx
  ::add-photo-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [company (:update_company (:data resp))]
      {:dispatch [:graphql/update-entry :company {:slug (:slug company)} :merge {:company company}]})))

(reg-event-fx
  ::delete-photo
  db/default-interceptors
  (fn [{db :db} [image]]
    (let [company (cached-company db)
          updated-company (update company :images #(remove #{image} %))]
      {:dispatch [:graphql/update-entry :company {:slug (:slug company)} :overwrite {:company updated-company}]
       :graphql {:query (update-company-mutation-with-fields [[:images [:url :width :height]] :id :slug])
                 :variables {:update_company
                             {:id (:id company)
                              :images (:images updated-company)}}
                 :on-failure [:error/set-global "There was an error deleting the photo"]}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn yt-url->yt-id
     "Caters for YT urls of different flavors:
    - \"https://www.youtube.com/watch?v=Pe0jFDPHkzo\"
    - \"https://youtu.be/Pe0jFDPHkzo\"
    - \"https://www.youtube.com/v/Pe0jFDPHkzo\""
     [yt-url]
     (let [uri (uri/parse yt-url)]
       (when (or (str/ends-with? (.getDomain uri) "youtube.com")
                 (str/ends-with? (.getDomain uri) "youtu.be"))
         (or (.getParameterValue uri "v")
             (subs (.getPath uri) (inc (.lastIndexOf (.getPath uri) "/"))))))))

(defn remove-video
  [videos youtube-id]
  (filter #(not= youtube-id (:youtube-id %)) videos))

#?(:cljs
   (reg-event-fx
     ::fetch-video-title
     profile-interceptors
     (fn [{db :db} [youtube-id]]
       {:http-xhrio
        {:method           :get
         :uri              (str "https://noembed.com/embed?url=https://www.youtube.com/watch?v=" youtube-id)
         :response-format  (ajax-json/json-response-format {:keywords? true})
         :timeout          10000
         :on-success       [::fetch-video-title-success youtube-id]
         :on-failure       [::fetch-video-title-failure youtube-id]}})))

(reg-event-fx
  ::fetch-video-title-success
  db/default-interceptors
  (fn [{db :db} [yt-id resp]]
    (let [company (cached-company db)]
      {:dispatch [::update-video (some (fn [v] (when (= yt-id (:youtube-id v))
                                                 (-> v
                                                     (assoc :description (:title resp))
                                                     (dissoc :loading?)))) (:videos company))]})))

(reg-event-fx
  ::fetch-video-title-failure
  db/default-interceptors
  (fn [{db :db} [yt-id _resp]]
    (let [company (cached-company db)
          updated-videos (map (fn [v] (if (= yt-id (:youtube-id v))
                                        (dissoc v :loading?)
                                        v)) (:videos company))]
      {:dispatch-n [[:graphql/update-entry :company {:slug (:slug company)}
                     :merge {:company {:videos updated-videos}}]
                    [:error/set-global "This video title could not be fetched"]]})))

#?(:cljs
   (reg-event-fx
     ::add-video
     db/default-interceptors
     (fn [{db :db} [yt-url]]
       (if-let [yt-id (yt-url->yt-id yt-url)]
         (let [company (cached-company db)
               existing-videos (:videos company)]
           (if (some #(= yt-id (:youtube-id %)) existing-videos)
             {:db db} ;; id already exists so do nothing
             (let [thumbnail (str "https://img.youtube.com/vi/" yt-id "/hqdefault.jpg")
                   videos (conj existing-videos {:youtube-id yt-id
                                                 :thumbnail thumbnail})
                   local-videos (conj existing-videos {:youtube-id yt-id
                                                       :thumbnail thumbnail
                                                       :loading? true})]
               {:graphql {:query (update-company-mutation-with-fields [[:videos [:youtubeId :thumbnail :description]]])
                          :variables {:update_company
                                      {:id (:id company)
                                       :videos (cases/->camel-case videos)}}
                          :on-failure [::add-video-failure yt-id]}
                :dispatch-n [[:graphql/update-entry :company {:slug (:slug company)}
                              :merge {:company {:videos local-videos}}]
                             [::fetch-video-title yt-id]]})))
         {:db (assoc db ::profile/video-error :invalid-url)}))))

(reg-event-fx
  ::add-video-failure
  db/default-interceptors
  (fn [{db :db} [yt-id _resp]]
    (let [company (cached-company db)
          videos(remove-video (:videos company) yt-id)]
      {:dispatch-n [[:graphql/update-entry :company {:slug (:slug company)}
                     :merge {:company {:videos videos}}]
                    [:error/set-global "This video could not be added"]]})))

(reg-event-fx
  ::update-video
  db/default-interceptors
  (fn [{db :db} [{:keys [youtube-id thumbnail description] :as video}]]
    (let [company (cached-company db)
          videos (map (fn [v] (if (= (:youtube-id v) youtube-id)
                                video
                                v)) (:videos company))]
      {:dispatch [:graphql/update-entry :company {:slug (:slug company)}
                  :merge {:company {:videos videos}}]
       :graphql {:query (update-company-mutation-with-fields [[:videos [:youtubeId :thumbnail :description]]])
                 :variables {:update_company
                             {:id (:id company)
                              :videos (cases/->camel-case videos)}}
                 :on-failure [:error/set-global "There was an error updating your video"]}})))

(reg-event-fx
  ::delete-video
  db/default-interceptors
  (fn [{db :db} [youtube-id]]
    (let [company (cached-company db)
          videos (remove-video (:videos company) youtube-id)]
      {:dispatch [:graphql/update-entry :company {:slug (:slug company)}
                  :merge {:company {:videos videos}}]
       :graphql {:query (update-company-mutation-with-fields [[:videos [:youtubeId :thumbnail :description]]])
                 :variables {:update_company
                             {:id (:id company)
                              :videos (cases/->camel-case videos)}}
                 :on-failure [:error/set-global "There was an error deleting your video"]}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn company-changes->cache
  [db changes]
  (cond-> changes
          (:tag-ids changes)
          (-> (assoc :tags (->> (cache/result db :tags {})
                                :list-tags
                                :tags
                                (filter #(contains? (set (:tag-ids changes)) (:id %)))
                                (map tag/->tag)))
              (dissoc :tag-ids))))

(reg-event-fx
  ::update-company
  db/default-interceptors
  (fn [{db :db} [changes scroll-to]]
    (let [company (cached-company db)]
      {:db (assoc-in db [::profile/sub-db ::profile/updating?] true)
       :dispatch [:graphql/update-entry :company {:slug (:slug company)}
                  :merge {:company (company-changes->cache db changes)}]
       :graphql {:query (update-company-mutation-with-fields [:id :slug :profileEnabled
                                                              [:tags [:id :label :slug :type :subtype]]])
                 :variables {:update_company
                             (merge {:id (:id company)}
                                    (cases/->camel-case changes))}
                 :on-success [::update-company-success]
                 :on-failure [::update-company-failure changes company]}
       :scroll-into-view scroll-to})))

(reg-event-fx
  ::update-company-failure
  profile-interceptors
  (fn [{db :db} [changes old-company]]
    {:db (assoc db ::profile/updating? false)
     :dispatch-n [[:graphql/update-entry :company {:slug (:slug old-company)} :overwrite {:company old-company}]
                  [:error/set-global "There was an error updating the company"
                   [::update-company changes]]]}))

(reg-event-fx
  ::update-company-success
  profile-interceptors
  (fn [{db :db} [resp]]
    (let [company (get-in resp [:data :update_company])]
      {:dispatch [:graphql/update-entry :company {:slug (:slug company)}
                  :merge {:company company}]
       :db (assoc db ::profile/updating? false)})))

(reg-event-db
  ::set-tag-search
  profile-interceptors
  (fn [db [tag-type tag-subtype tag-search]]
    (assoc-in db [::profile/tag-search tag-type tag-subtype] tag-search)))

(reg-event-fx
  ::fetch-all-tags
  (fn [{db :db} _]
    {:dispatch (into [:graphql/query] (tag-query nil))}))

(reg-event-db
  ::reset-selected-tag-ids
  db/default-interceptors
  (fn [db [tag-type tag-subtype]]
    (assoc-in db [::profile/sub-db ::profile/selected-tag-ids tag-type tag-subtype]
              (->> (cached-company db)
                   :tags
                   (filter #(and (= tag-type (:type %))
                                 (= tag-subtype (:subtype %))))
                   (map :id)
                   (set)))))

(reg-event-fx
  ::toggle-selected-tag-id
  db/default-interceptors
  (fn [{db :db} [tag-type tag-subtype id]]
    (let [company (cached-company db)
          updated-db (update-in db [::profile/sub-db ::profile/selected-tag-ids tag-type tag-subtype] (fnil util/toggle #{}) id)
          tag-ids (get-in updated-db [::profile/sub-db ::profile/selected-tag-ids tag-type])]
      (merge
        {:db updated-db}
        (when-not (:has-published-profile company) ;; if were in 'create profile' mode, check the fields
          (cond (= :tech tag-type)
                {:dispatch [::check-field {:tech-tags tag-ids}]}
                (= :benefit tag-type)
                {:dispatch [::check-field {:benefit-tags tag-ids}]}))))))
(reg-event-fx
  ::create-new-tag
  profile-interceptors
  (fn [{db :db} [label tag-type tag-subtype]]
    {:db (assoc db ::profile/creating-tag? true)
     :graphql {:query tag-gql/create-tag-mutation
               :variables (merge {:label label :type tag-type}
                                 (when tag-subtype
                                   {:subtype tag-subtype}))
               :on-success [::create-new-tag-success tag-type tag-subtype]
               :on-failure [::create-new-tag-failure]}}))

(reg-event-fx
  ::create-new-tag-success
  db/default-interceptors
  (fn [{db :db} [tag-type tag-subtype resp]]
    (let [new-tag (get-in resp [:data :create_tag])
          all-tags (cache/result db :tags {})]
      (merge {:db (-> db
                      (assoc-in  [::profile/sub-db ::profile/creating-tag?] false)
                      (assoc-in  [::profile/sub-db ::profile/tag-search tag-type] nil))
              :dispatch-n [[::toggle-selected-tag-id tag-type tag-subtype (:id new-tag)]
                           (when (not (some #(= (:id new-tag) (:id %)) (get-in all-tags [:list-tags :tags]))) ;; check the tag isn't an existing tag
                             [:graphql/update-entry :tags {}
                              :overwrite (update-in all-tags [:list-tags :tags] conj new-tag)])]}))))

(reg-event-fx
  ::create-new-tag-failure
  profile-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::profile/creating-tag? false)
     :dispatch [:error/set-global "There was an error adding the tag"]}))

(reg-event-fx
  ::update-location-suggestions
  profile-interceptors
  (fn [{db :db} [search]]
    (if (str/blank? search)
      {:db (dissoc db ::profile/location-search ::profile/location-suggestions)}
      {:db (assoc db ::profile/location-search search)
       :google/place-predictions {:input      search
                                  :on-success [::set-location-suggestions]
                                  :on-failure [:error/set-global "Failed to fetch address suggestions"]}})))

(reg-event-db
  ::set-location-suggestions
  profile-interceptors
  (fn [db [suggestions]]
    (assoc db ::profile/location-suggestions (or suggestions []))))

(reg-event-fx
  ::select-location
  profile-interceptors
  (fn [{db :db} [location]]
    (if-let [details (some #(when (= location (:description %)) %) (::profile/location-suggestions db))]
      {:google/place-details {:place-id   (:place_id details)
                              :on-success [::fetch-place-details-success]
                              :on-failure [:error/set-global "Failed to find address"]}
       :db (-> db
               (assoc ::profile/location-search (:description details))
               (dissoc ::profile/location-suggestions))}
      {:db (dissoc db ::profile/location-search ::profile/location-suggestions)})))

#?(:cljs
   (reg-event-db
     ::fetch-place-details-success
     profile-interceptors
     (fn [db [google-response]]
       (let [location (location/google-place->location google-response)]
         (assoc db ::profile/pending-location location)))))

(reg-event-db
  ::reset-location-search
  profile-interceptors
  (fn [db _]
    (dissoc db
            ::profile/pending-location
            ::profile/location-suggestions
            ::profile/location-search)))

(reg-event-db
  ::logo-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/logo-uploading? true)))

(reg-event-fx
  ::logo-upload-success
  profile-interceptors
  (fn [{db :db} [_ {:keys [url]}]]
    {:db (assoc db
                ::profile/pending-logo url
                ::profile/logo-uploading? false)}))

(reg-event-db
  ::reset-pending-logo
  profile-interceptors
  (fn [db _]
    (dissoc db ::profile/pending-logo)))

(reg-event-db
  ::logo-upload-failure
  profile-interceptors
  (fn [{db :db} _]
    (assoc db ::profile/logo-uploading? false)))

(reg-event-fx
  ::publish-profile
  db/default-interceptors
  (fn [{db :db} _]
    (let [company (cached-company db)
          new-value (not (:profile-enabled company))]
      {:db      (assoc-in db [::profile/sub-db ::profile/publishing?] true)
       :graphql {:query      publish-company-profile-mutation
                 :variables  {:id              (:id company)
                              :profile_enabled new-value}
                 :on-success [::publish-profile-success (company-slug db)]
                 :on-failure [::publish-profile-failure]}})))

(reg-event-fx
  ::publish-profile-failure
  profile-interceptors
  (fn [{db :db} [resp]]
    (let [parsed-error (-> resp
                           util/gql-errors->error-key
                           errors/error-map)
          error-msg (or parsed-error "There was an error publishing/unpublishing the company.")]
      {:db       (assoc db ::profile/publishing? false)
       :dispatch [:error/set-global error-msg
                  [::publish-profile]]})))

(reg-event-fx
  ::publish-profile-success
  profile-interceptors
  (fn [{db :db} [slug resp]]
    (let [enabled? (get-in resp [:data :publish_profile :profile_enabled])]
      {:dispatch [:graphql/update-entry :company {:slug slug}
                  :merge {:company {:profile-enabled enabled?}}]
       :db (assoc db ::profile/publishing? false)})))

(reg-event-fx
  ::fetch-all-jobs
  db/default-interceptors
  (fn [{db :db} _]
    (let [total (-> db (cached-company-extra-data) :jobs :pagination :total )]
      {:dispatch (into [:graphql/query] (all-jobs-query db total))})))

(reg-event-db
  ::set-show-sticky?
  profile-interceptors
  (fn [db [show?]]
    (assoc db :show-sticky? show?)))

(defn get-all-tag-ids
  [db]
  (let [selected-tag-ids  (get-in db [::profile/sub-db ::profile/selected-tag-ids])]
    (reduce #(concat %1 (reduce concat (vals %2))) [] (vals selected-tag-ids))))

(defn create-new-profile-data->minimum-data-form
  [db {:keys [industry-tag funding-tag name description size founded-year] :as form-data}]
  (let [company          (cached-company db)
        selected-tag-ids (->> [industry-tag funding-tag]
                              (concat (get-all-tag-ids db))
                              (concat (map :id (:tags company)))
                              (remove nil?)
                              (set))
        selected-tags    (->> (cache/result db :tags {})
                              :list-tags
                              :tags
                              (filter #(contains? selected-tag-ids (:id %)))
                              (map tag/->tag))]
    (-> form-data
        (assoc :industry-tag (or (some #(when (= industry-tag (:id %)) (dissoc % :subtype)) selected-tags)
                                 (some #(when (= :industry (:type %))  (dissoc % :subtype)) selected-tags))
               :funding-tag  (or (some #(when (= funding-tag (:id %)) (dissoc % :subtype)) selected-tags)
                                 (some #(when (= :funding (:type %))  (dissoc % :subtype)) selected-tags))
               :tech-tags    (filter #(= :tech (:type %)) selected-tags)
               :benefit-tags (filter #(= :benefit (:type %)) selected-tags)
               :logo         (or (get-in db [::profile/sub-db ::profile/pending-logo]) (:logo company))
               :name         (or name (:name company))
               :size         (or size (:size company))
               :founded-year (or founded-year (:founded-year company))
               :description  (or description (:description-html company))))))

(defn form-data->company-data
  [db {:keys [industry-tag funding-tag name description size founded-year] :as form-data}]
  (let [company          (cached-company db)
        selected-tag-ids (->> [industry-tag funding-tag]
                              (concat (get-all-tag-ids db))
                              (concat (map :id (:tags company)))
                              (remove nil?)
                              (set))]
    (-> form-data
        (dissoc :industry-tag :funding-tag :description)
        (assoc :tag-ids          selected-tag-ids
               :logo             (or (get-in db [::profile/sub-db ::profile/pending-logo]) (:logo company))
               :name             (or name (:name company))
               :size             (or size (:size company))
               :founded-year     (or founded-year (:founded-year company))
               :description-html (or description (:description-html company))
               :profile-enabled  true))))

(defmulti error-pred->message (fn [k v] k))

(defmethod error-pred->message :default
  [_ _]
  "There is a problem with this field")

(defmethod error-pred->message :name
  [_ _]
  "This field cannot be left blank.")

(defmethod error-pred->message :logo
  [_ _]
  "Please upload a logo for your company.")

(defmethod error-pred->message :description
  [_ _]
  "Please provide a brief description for your company.")

(defmethod error-pred->message :industry-tag
  [_ _]
  "Please select an industry for your company.")

(defmethod error-pred->message :funding-tag
  [_ _]
  "Please select a funding type that applies to your company.")

(defmethod error-pred->message :size
  [_ _]
  "Please select a size for your company.")

(defmethod error-pred->message :founded-year
  [_ _]
  "Please provide a founding year for your company.")

(defmethod error-pred->message :tech-tags
  [_ _]
  "Please select at least one Technology tag.")

(defmethod error-pred->message :benefit-tags
  [_ _]
  "Please select at least one Benefit tag.")

(defn error-data->error-map
  [error-data]
  (let [error-map (->> (::s/problems error-data)
                       (map (juxt (comp last :path) :pred))
                       (filter first)
                       (into {})
                       (not-empty))]
    (reduce-kv (fn [a k v] (assoc a k (error-pred->message k v)) ) {} error-map)))

(defn generate-error-map
  [db m]
  (let [data (create-new-profile-data->minimum-data-form db m)
        error-data (s/explain-data ::profile/form data)]
    (error-data->error-map error-data)))

(reg-event-db
  ::check-field
  db/default-interceptors
  (fn [db [m]]
    (let [em (-> db
                 (generate-error-map m)
                 (select-keys (keys m)))]
      (if (not-empty em)
        (update-in db [::profile/sub-db ::profile/error-map] merge em)
        (update-in db [::profile/sub-db ::profile/error-map] #(apply dissoc % (keys m)))))))

(reg-event-fx
  ::create-new-profile
  db/default-interceptors
  (fn [{db :db} [form-data]]
    (let [error-map (generate-error-map db form-data)
          updated-db (assoc-in db [::profile/sub-db ::profile/error-map] error-map)]
      (merge {:db updated-db}
             (if-not (not-empty error-map)
               {:dispatch [::update-company (form-data->company-data db form-data) nil]
                :dispatch-debounce {:id       :scroll-to-top-after-completing-profile
                                    :dispatch [:wh.events/scroll-to-top]
                                    :timeout  500}}
               {:scroll-into-view (profile/section->id (apply min (map profile/field->section (keys error-map))))})))))
