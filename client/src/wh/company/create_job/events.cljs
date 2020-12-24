(ns wh.company.create-job.events
  (:require [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as ce]
            [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.data :as data :refer [get-manager-email get-manager-name]]
            [wh.common.errors :as common-errors]
            [wh.common.keywords :as keywords]
            [wh.common.location :as location]
            [wh.common.user :as user-common]
            [wh.company.create-job.db :as create-job]
            [wh.components.tag :as tag]
            [wh.db :as db]
            [wh.graphql.company
             :refer [create-job-mutation
                     update-job-mutation update-company-mutation]
             :as company-queries]
            [wh.graphql.tag :refer [tag-query]]
            [wh.job.db :as job]
            [wh.pages.core :as pages :refer [on-page-load]]
            [wh.user.db :as user]
            [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(def create-job-interceptors (into db/default-interceptors
                                   [(path ::create-job/sub-db)]))

(def companies-query-page-size 10)

(defn companies-query
  [search]
  {:venia/queries [[:companies
                    {:search_term search
                     :page_number 1
                     :page_size companies-query-page-size}
                    [[:pagination [:total :count :pageNumber]]
                     [:companies [:id :name
                                  [:integrations
                                   [[:greenhouse [:enabled [:jobs [:id :name]]]]
                                    [:workable [:enabled [:jobs [:id :name]]]]]]]]]]]})

(defn db->graphql-job
  [db]
  (let [sub-db (::create-job/sub-db db)]
    (as-> sub-db sub-db
          (update sub-db ::create-job/manager get-manager-email) ;; at the top because it might be culled
          (select-keys sub-db (create-job/relevant-fields db))
          (util/unflatten-map sub-db)
          (dissoc sub-db ::create-job/company)
          (assoc sub-db :tag-ids (::create-job/tags sub-db))
          (dissoc sub-db ::create-job/tags)
          (update sub-db ::create-job/remuneration util/remove-nils)
          (update sub-db ::create-job/location (fn [l] (util/dissoc-selected-keys-if-blank l (set (keys l)))))
          (update sub-db ::create-job/verticals vec)
          (ce/transform-keys c/->camelCaseString sub-db))))

(defn create-mutation-variables
  [db]
  (let [job (db->graphql-job db)]
    (if (create-job/edit? db)
      {:update_job (assoc job "id" (get-in db [::db/page-params :id]))}
      {:create_job job})))

(defn graphql-job->sub-db
  [job]
  (as-> job job
        (util/remove-nils job)
        (cases/->kebab-case job)
        (util/flatten-map job)
        (keywords/namespace-map "wh.company.create-job.db" job)
        (update job ::create-job/verticals set)
        (update job ::create-job/manager get-manager-name)
        (update job ::create-job/tags #(set (map :id %)))))

(defn company-by-id [db id]
  (->> db ::create-job/companies (filter #(= (:id %) id)) first))

(doseq [[field {:keys [event?] :or {event? true}}] create-job/fields
        :when event?
        :let [event-name (keyword "wh.company.create-job.events" (str "edit-" (name field)))
              db-field (keyword "wh.company.create-job.db" (name field))]]
  (reg-event-db event-name
                create-job-interceptors
                (fn [db [new-value]]
                  (assoc db db-field new-value))))

(reg-event-fx
  ::select-company-suggestion
  create-job-interceptors
  (fn [{db :db} [id]]
    (let [company (company-by-id db id)]
      {:db (-> db
               (assoc ::create-job/company-id id)
               (assoc ::create-job/company__name (:name company))
               (assoc ::create-job/company__integrations (:integrations company)))
       :dispatch [::fetch-company id]})))

(reg-event-fx
  ::edit-company__name
  create-job-interceptors
  (fn [{db :db} [new-value]]
    {:db (assoc db
                ::create-job/company__name new-value
                ::create-job/company-id nil)
     :dispatch [::fetch-companies new-value]}))

(reg-event-db
  ::edit-ats-job-id
  create-job-interceptors
  (fn [db [new-value]]
    (assoc db ::create-job/ats-job-id new-value)))

(reg-event-db
  ::edit-workable-subdomain
  create-job-interceptors
  (fn [db [new-value]]
    (assoc db ::create-job/workable-subdomain new-value)))

(reg-event-db
  ::toggle-vertical
  create-job-interceptors
  (fn [db [new-value]]
    (let  [new-db (update db ::create-job/verticals util/toggle-unless-empty new-value)
           remote-vertical? (contains? (::create-job/verticals new-db) "remote")]
      (assoc new-db ::create-job/remote remote-vertical?))))

(reg-event-db
  ::edit-remote
  create-job-interceptors
  (fn [db [new-value]]
    (-> db
        (update ::create-job/verticals (if new-value conj disj) "remote")
        (assoc ::create-job/remote new-value))))

(reg-event-fx
  ::scroll-into-view
  db/default-interceptors
  (fn [_ [id]]
    {:scroll-into-view id}))

(reg-event-fx
  ::save-company
  db/default-interceptors
  (fn [{db :db} [nav-path]]
    (let [company (create-job/db->gql-company db)]
      {:graphql {:query update-company-mutation
                 :variables {:update_company company}
                 :on-success [::save-company-success nav-path]
                 :on-failure [::create-job-error]}})))

(reg-event-fx
  ::save-company-success
  db/default-interceptors
  (fn [{db :db} [nav-path resp]]
    {:db       (assoc-in db [::create-job/sub-db ::create-job/saving?] false)
     :navigate nav-path}))

(reg-event-fx
  ::create-job
  db/default-interceptors
  (fn [{db :db} _]
    (let [errors            (not-empty
                              (concat (create-job/invalid-fields db)
                                      (create-job/invalid-company-fields db)))
          include-location? (some #(when (str/starts-with? (name %) "location") %) errors)
          variables         (create-mutation-variables db)]
      (if errors
        (do
          (js/console.error "Errors in form:" (clj->js errors))
          (merge {:db                (-> db
                                         (assoc-in [::create-job/sub-db ::create-job/error] nil)
                                         (assoc-in [::create-job/sub-db ::create-job/saving?] false)
                                         (assoc-in [::create-job/sub-db ::create-job/form-errors] (set errors)))
                  :dispatch-debounce {:id       :scroll-to-error-after-pause
                                      :dispatch [::scroll-into-view (db/key->id (first errors))]
                                      :timeout  50}}
                 (when include-location?
                   {:dispatch [::set-editing-address true false]})))

        {:db      (-> db
                      (assoc-in [::create-job/sub-db ::create-job/error] nil)
                      (assoc-in [::create-job/sub-db ::create-job/saving?] true))
         :graphql {:query      (if (create-job/edit? db)
                                 update-job-mutation
                                 create-job-mutation)
                   :variables  variables
                   :on-success [::create-job-success]
                   :on-failure [::create-job-error]}}))))

(reg-event-fx
  ::save-workable-account
  db/default-interceptors
  (fn [{db :db} _]
    (let [sub-db (::create-job/sub-db db)
          company {:id (::create-job/company-id sub-db)
                   :integrations {:workable {:accountSubdomain (::create-job/workable-subdomain sub-db)}}}]
      {:db      (assoc-in db [::create-job/sub-db ::create-job/saving-workable-account?] true)
       :graphql {:query update-company-mutation
                 :variables {:update_company company}
                 :on-success [::save-workable-account-success]
                 :on-failure [::save-workable-account-error]}})))

(reg-event-db
  ::save-workable-account-success
  create-job-interceptors
  (fn [db _]
    (-> db
        (assoc ::create-job/saving? false)
        (assoc-in [::create-job/company__integrations :workable :account-subdomain] (::create-job/workable-subdomain db)))))

(reg-event-db
  ::save-workable-account-error
  create-job-interceptors
  (fn [db [resp]]
    (assoc db ::create-job/saving? false
           ::create-job/error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::create-job-success
  db/default-interceptors
  (fn [{db :db} [{data :data}]]
    (let [slug (-> data vals first :slug)]
      {:db       (assoc-in db [::job/sub-db ::job/slug] "")
       :dispatch [::save-company [:job :params {:slug slug}]]})))

(reg-event-db
  ::create-job-error
  create-job-interceptors
  (fn [db [resp]]
    (assoc db ::create-job/saving? false
           ::create-job/error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::unpublish-job
  db/default-interceptors
  (fn [{db :db} _]
    {:db      (-> db (assoc-in [::create-job/sub-db ::create-job/error] nil)
                  (assoc-in [::create-job/sub-db ::create-job/saving?] true))
     :graphql {:query      update-job-mutation
               :variables  {:update_job {:id (get-in db [::db/page-params :id])
                                         :published false}}
               :on-success [::create-job-success]
               :on-failure [::create-job-error]}}))

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    (let [page (:wh.db/page db)
          new-db (create-job/initial-db db (= page :edit-job))]
      {:db (assoc db ::create-job/sub-db new-db)})))

(defn add-keyset-to-field
  [field-to-find keyset field]
  (if (= field field-to-find)
    [field keyset]
    field))

(defn add-nested-job-fields
  [fields]
  (mapv
    (fn [field]
      (->> field
           (add-keyset-to-field :location [:street :city :country :countryCode :state :postCode :longitude :latitude])
           (add-keyset-to-field :remuneration [:competitive :currency :timePeriod :min :max :equity])))
    fields))

(defquery job-query
  {:venia/operation {:operation/type :query
                     :operation/name "fetch_job"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:job {:id :$id}
                    [:companyId :promoted :tagline
                     :approved :sponsorshipOffered
                     [:tags :fragment/tagFields]
                     :remote :published :verticals
                     :descriptionHtml :title :roleType
                     :manager :atsJobId
                     [:location
                      [:state
                       :city :street :countryCode :country
                       :latitude :longitude :postCode]]
                     [:remuneration
                      [:competitive :equity :currency :min :timePeriod :max]]
                     [:company [:name]]]]]})

(reg-event-fx
  ::load-job
  db/default-interceptors
  (fn [{db :db} [id]]
    {:dispatch [::pages/set-loader]
     :graphql  {:query      job-query
                :variables  {:id id}
                :on-success [::load-job-success]
                :on-failure [::load-job-failure [::load-job id]]}}))

(reg-event-fx
  ::load-job-success
  db/default-interceptors
  (fn [{db :db} [{:keys [data]}]]
    (let [new-db (merge (::create-job/sub-db db) (graphql-job->sub-db (:job data)))
          save?  (get (::db/query-params db) "save")]
      {:db         (assoc db ::create-job/sub-db new-db)
       :dispatch-n (list [::pages/unset-loader]
                         [::close-search-location-form]
                         [::fetch-company (::create-job/company-id new-db)]
                         (when save?
                           [::create-job]))})))

(reg-event-fx
  ::load-job-failure
  create-job-interceptors
  (fn [{db :db} [re-try-event]]
    {:dispatch-n [[::pages/unset-loader]
                  [:error/set-global
                   "An error occurred while fetching the job details."
                   re-try-event]]}))

(reg-event-fx
  ::fetch-companies
  create-job-interceptors
  (fn [_ [search]]
    {:graphql {:query      (companies-query search)
               :on-success [::fetch-companies-success]
               :on-failure [::fetch-companies-failure]}}))

(reg-event-db
  ::fetch-companies-success
  create-job-interceptors
  (fn [db [{{{companies :companies} :companies} :data}]]
    (assoc db ::create-job/companies companies)))

(reg-event-db
  ::fetch-companies-failure
  create-job-interceptors
  (fn [db _] db))

(reg-event-db
  ::fetch-company-success
  create-job-interceptors
  (fn [db [company-id {{company :company} :data}]]
    (let [company (-> company
                      (cases/->kebab-case)
                      (update :tags (partial map tag/->tag)))]
      (assoc db
             ::create-job/company (dissoc company :slug)
             ::create-job/company-id company-id
             ::create-job/company-slug (:slug company)
             ::create-job/company-loading? false
             ::create-job/company__integrations (:integrations company)
             ;;
             ::create-job/selected-benefit-tag-ids (->> (:tags company)
                                                        (filter (fn [t] (= :benefit (:type t))))
                                                        (map :id)
                                                        (set))))))

(reg-event-fx
  ::fetch-company-failure
  create-job-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::create-job/company-loading? false)
     :dispatch [:error/set-global "Failed to fetch the user's company"]}))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} [company-id]]
    {:db (assoc-in db [::create-job/sub-db ::create-job/company-loading?] true)
     :graphql {:query company-queries/edit-job-company-query
               :variables {:company_id company-id}
               :on-success [::fetch-company-success company-id]
               :on-failure [::fetch-company-failure]}}))

(defmethod on-page-load :create-job [db]
  (if (job/can-publish-jobs? db)
    (list [::initialize-db]
          [:google/load-maps]
          [::fetch-tags]
          [::fetch-benefit-tags]
          (when (user-common/company? db)
            [::fetch-company (user/company-id db)]))

    (list [::cant-create-job-redirect])))

(defmethod on-page-load :edit-job [db]
  (if (job/can-edit-jobs? db)
    (list [::initialize-db]
          [:google/load-maps]
          [::fetch-tags]
          [::load-job (get-in db [::db/page-params :id])]
          [::fetch-benefit-tags])

    (list [::cant-edit-job-redirect])))

(reg-event-db
  ::set-location-suggestions
  create-job-interceptors
  (fn [db [suggestions]]
    (assoc db ::create-job/location-suggestions suggestions)))

(reg-event-fx
  ::select-location-suggestion
  create-job-interceptors
  (fn [{_db :db} [id]]
    {:google/place-details
     {:place-id   id
      :on-success [::fetch-place-details-success]
      :on-failure [:error/set-global "Failed to fetch data from Google."]}}))

(reg-event-fx
  ::fetch-place-details-success
  create-job-interceptors
  (fn [{db :db} [google-response]]
    (let [{:keys [street city country country-code state post-code latitude longitude]}
          (location/google-place->location google-response)]
      {:db (merge db
                  (util/remove-nil-blank-or-empty
                    {::create-job/location__street street
                     ::create-job/location__city city
                     ::create-job/location__country country
                     ::create-job/location__country-code country-code
                     ::create-job/location__state state
                     ::create-job/location__post-code post-code
                     ::create-job/location__latitude latitude
                     ::create-job/location__longitude longitude}))
       :dispatch-n [[::set-location-suggestions []]
                    [::open-search-location-form]]})))

(reg-event-fx
  ::search-location
  create-job-interceptors
  (fn [{db :db} [retry-num]]
    {:google/place-predictions
     {:input      (get db ::create-job/search-address)
      :on-success [::process-search-location]
      :on-failure [:error/set-global "Failed to fetch data from Google."]}}))

(reg-event-db
  ::process-search-location
  create-job-interceptors
  (fn [db [results]]
    (->> results
         (mapv (fn [{:keys [description place_id]}]
                 {:id    place_id
                  :label description}))
         (assoc db
                ::create-job/location-search-error nil
                ::create-job/location-suggestions))))

(reg-event-fx
  :search-location-bad-response
  create-job-interceptors
  (fn [{db :db} [retry-attempt result]]
    (js/console.error "Retry attempt:" retry-attempt)
    (js/console.error "Location search failed:" result)
    (if (and retry-attempt (> retry-attempt 2))
      {:db (assoc db ::create-job/location-search-error "Error searching location, please try again later")}
      (let [attempt (if-not retry-attempt 1 (inc retry-attempt))]
        {:dispatch [::search-location attempt]}))))

(reg-event-db
  ::edit-tagline
  create-job-interceptors
  (fn [db [tagline]]
    (assoc db ::create-job/tagline
           (apply str (take create-job/tagline-max-length tagline)))))

(reg-event-fx
  ::edit-search-address
  create-job-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::create-job/search-address new-value)}
      (if (seq new-value)
        {:dispatch [::search-location]}
        {:dispatch [::set-location-suggestions []]}))))

(reg-event-db
  ::set-city-suggestions
  create-job-interceptors
  (fn [db [suggestions]]
    (assoc db ::create-job/city-suggestions suggestions)))

(reg-event-db
  ::set-country-suggestions
  create-job-interceptors
  (fn [db [suggestions]]
    (assoc db ::create-job/country-suggestions suggestions)))

(reg-event-db
  ::set-state-suggestions
  create-job-interceptors
  (fn [db [suggestions]]
    (assoc db ::create-job/state-suggestions suggestions)))

(reg-event-db
  ::process-cities
  create-job-interceptors
  (fn [db [new-value]]
    (->> data/cities
         (filter (fn [city]
                   (str/includes? (str/lower-case city) (str/lower-case new-value))))
         (mapv (fn [city] {:id    city
                           :label city}))
         (assoc db ::create-job/city-suggestions))))

(reg-event-db
  ::process-countries
  create-job-interceptors
  (fn [db [new-value]]
    (->> data/countries
         (filter (fn [country]
                   (str/includes? (str/lower-case country) (str/lower-case new-value))))
         (mapv (fn [country] {:id country
                              :label country}))
         (assoc db ::create-job/country-suggestions))))

(reg-event-db
  ::process-states
  create-job-interceptors
  (fn [db [new-value]]
    (->> data/us-states
         (filter (fn [[state-full-name]]
                   (str/includes? (str/lower-case state-full-name) (str/lower-case new-value))))
         (mapv (fn [[state-full-name state-short-name]]
                 {:id state-short-name
                  :label (str state-full-name " - " state-short-name)}))
         (assoc db ::create-job/state-suggestions))))

(reg-event-db
  ::attempt-country-code-assignment
  create-job-interceptors
  (fn [db [country]]
    (if-let [country-code (get data/country->country-code country)]
      (assoc db ::create-job/location__country-code country-code)
      db)))

(reg-event-fx
  ::select-city-suggestion
  create-job-interceptors
  (fn [{db :db} [suggestion]]
    {:db       (assoc db ::create-job/location__city suggestion)
     :dispatch [::set-city-suggestions []]}))

(reg-event-fx
  ::edit-location__city
  create-job-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::create-job/location__city new-value)}
      (if (seq new-value)
        {:dispatch [::process-cities new-value]}
        {:dispatch [::set-city-suggestions []]}))))

(reg-event-fx
  ::select-country-suggestion
  create-job-interceptors
  (fn [{db :db} [suggestion]]
    {:db       (assoc db ::create-job/location__country suggestion)
     :dispatch-n [[::attempt-country-code-assignment suggestion]
                  [::set-country-suggestions []]]}))

(reg-event-fx
  ::select-state-suggestion
  create-job-interceptors
  (fn [{db :db} [suggestion]]
    {:db       (assoc db ::create-job/location__state suggestion)
     :dispatch [::set-state-suggestions []]}))

(reg-event-fx
  ::edit-location__country
  create-job-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::create-job/location__country new-value)}
      (if (seq new-value)
        {:dispatch-n [[::attempt-country-code-assignment new-value]
                      [::process-countries new-value]]}
        {:dispatch [::set-country-suggestions []]}))))

(reg-event-fx
  ::edit-location__state
  create-job-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::create-job/location__state new-value)
       :dispatch (if (seq new-value)
                   [::process-states new-value]
                   [::set-state-suggestions []])})))

(reg-event-db
  ::open-search-location-form
  create-job-interceptors
  (fn [db _]
    (assoc db ::create-job/search-location-form-open? true)))

(reg-event-db
  ::close-search-location-form
  create-job-interceptors
  (fn [db _]
    (assoc db ::create-job/search-location-form-open? false)))

(reg-event-db
  ::clear-salary-remuneration-values
  create-job-interceptors
  (fn [db _]
    (-> db
        (assoc ::create-job/remuneration__min nil)
        (assoc ::create-job/remuneration__max nil)
        (assoc ::create-job/remuneration__currency nil)
        (assoc ::create-job/remuneration__time-period (first data/time-periods))
        (assoc ::create-job/remuneration__equity false))))

(reg-event-fx
  ::edit-remuneration__competitive
  create-job-interceptors
  (fn [{db :db} [competitive]]
    {:db (assoc db ::create-job/remuneration__competitive competitive)
     :dispatch [::clear-salary-remuneration-values]}))

(reg-event-db
  ::edit-remuneration__currency
  create-job-interceptors
  (fn [db [currency]]
    (let [tp (::create-job/remuneration__time-period db)]
      (assoc db
             ::create-job/remuneration__currency currency
             ::create-job/remuneration__min (data/get-min-salary currency tp)
             ::create-job/remuneration__max (data/get-max-salary currency tp)))))

(reg-event-db
  ::edit-remuneration__time-period
  create-job-interceptors
  (fn [db [tp]]
    (let [currency (::create-job/remuneration__currency db)]
      (assoc db
             ::create-job/remuneration__time-period tp
             ::create-job/remuneration__min (data/get-min-salary currency tp)
             ::create-job/remuneration__max (data/get-max-salary currency tp)))))

(reg-event-db
  ::set-salary-range
  create-job-interceptors
  (fn [db [[salary-min salary-max]]]
    (assoc db
           ::create-job/remuneration__min salary-min
           ::create-job/remuneration__max salary-max)))

(reg-event-fx
  ::set-editing-address
  create-job-interceptors
  (fn [{db :db} [value scroll?]]
    (merge {:db (assoc db ::create-job/editing-address? value)}
           (when-not (false? scroll?) ;; default to true
             {:scroll-into-view (db/key->id ::search-address)}))))

;; Benefits

(reg-event-db
  ::toggle-benefits-collapsed
  create-job-interceptors
  (fn [db _]
    (update db ::create-job/benefits-collapsed? not)))

;; Tags

(reg-event-fx
  ::fetch-tags
  (fn [_ _]
    {:dispatch (into [:graphql/query] [:tags {:type :tech}])}))

(reg-event-db
  ::toggle-tags-collapsed
  create-job-interceptors
  (fn [db _]
    (update db ::create-job/tags-collapsed? not)))

(reg-event-fx
  ::toggle-tag
  create-job-interceptors
  (fn [{db :db} [tag]]
    {:db       (update db ::create-job/tags
                       (fnil util/toggle #{}) (:id tag))
     :dispatch [::set-tag-search ""]}))

(reg-event-db
  ::set-tag-search
  create-job-interceptors
  (fn [db [tag-search]]
    (assoc db ::create-job/tag-search tag-search)))

(reg-event-db
  ::fetch-tags-success
  create-job-interceptors
  (fn [db [results]]
    (let [results (group-by :attr (get-in results [:data :jobs_search :facets]))
          results (->> (get results "tags")
                       (sort-by :count)
                       (map #(hash-map :tag (:value %)))
                       (reverse))]
      (assoc db ::create-job/available-tags results))))

(reg-event-fx
  ::select-manager
  create-job-interceptors
  (fn [{db :db} [manager]]
    {:db (assoc db ::create-job/manager manager)}))

;; Company

(reg-event-db
  ::logo-upload-start
  create-job-interceptors
  (fn [db _]
    (assoc db ::create-job/logo-uploading? true)))

(reg-event-fx
  ::logo-upload-success
  create-job-interceptors
  (fn [{db :db} [_ {:keys [url]}]]
    {:db (-> db
             (assoc
               ::create-job/pending-logo url
               ::create-job/logo-uploading? false)
             (update ::create-job/form-errors
                     (fnil disj #{}) :wh.company.profile/logo))}))

(reg-event-fx
  ::logo-upload-failure
  create-job-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::create-job/logo-uploading? false)
     :dispatch [:error/set-global
                (common-errors/image-upload-error-message (:status resp))]}))

(reg-event-db
  ::set-benefits-search
  create-job-interceptors
  (fn [db [tag-search]]
    (assoc db ::create-job/benefits-search tag-search)))

(reg-event-fx
  ::fetch-benefit-tags
  (fn [{_db :db} _]
    {:dispatch (into [:graphql/query] (tag-query :benefit))}))

(reg-event-db
  ::toggle-selected-benefit-tag-id
  create-job-interceptors
  (fn [db [id]]
    (update-in db [::create-job/selected-benefit-tag-ids] (fnil util/toggle #{}) id)))

(reg-event-db
  ::set-pending-company-description
  create-job-interceptors
  (fn [db [desc]]
    (assoc db ::create-job/pending-company-description desc)))

(reg-event-fx
  ::cant-edit-job-redirect
  create-job-interceptors
  (fn [{_db :db} _]
    {:navigate [:payment-setup :params {:step :select-package}]}))

(reg-event-fx
  ::cant-create-job-redirect
  create-job-interceptors
  (fn [{_db :db} _]
    {:navigate [:payment-setup
                :params {:step :select-package}
                :query-params {:action "publish"}]}))
