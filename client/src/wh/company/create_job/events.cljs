(ns wh.company.create-job.events
  (:require
    [camel-snake-kebab.core :as c]
    [camel-snake-kebab.extras :as ce]
    [clojure.set :as set :refer [intersection]]
    [clojure.string :as str]
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.data :as data :refer [get-manager-email get-manager-name]]
    [wh.common.fx.google-maps :as google-maps]
    [wh.company.common :as common]
    [wh.company.create-job.db :as create-job]
    [wh.db :as db]
    [wh.graphql.company :refer [company-query create-job-mutation update-job-mutation update-company-mutation fetch-tags]]
    [wh.jobs.job.db :as job]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util]))

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
                     [:companies [:id :name [:integrations [[:greenhouse [:enabled [:jobs [:id :name]]]]]]]]]]]})

(defn check-descriptions
  ;; TODO PACKAGES what do we do about this?
  [job db]
  (if (user/admin? db)
    job
    (let [private-description-html (::create-job/private-description-html job)
          take-off? (= (get-in db [::user/sub-db ::user/company :package]) "take_off")]
      (assoc job
             :public-description-html
             (if take-off?
               (or (::create-job/public-description-html job) private-description-html)
               private-description-html)))))

(defn db->graphql-job
  [db]
  (let [sub-db (::create-job/sub-db db)]
    (as-> sub-db sub-db
      (update sub-db ::create-job/manager get-manager-email) ;; at the top because it might be culled
      (select-keys sub-db (create-job/relevant-fields db))
      (util/unflatten-map sub-db)
      (check-descriptions sub-db db)
      (dissoc sub-db ::create-job/company-name)
      (update sub-db ::create-job/tags (partial map :tag))
      (update sub-db ::create-job/benefits (partial map :tag))
      (update sub-db ::create-job/remuneration util/remove-nils)
      (update sub-db ::create-job/location (fn [l] (util/dissoc-selected-keys-if-blank l (set (keys l)))))
      (update sub-db ::create-job/verticals vec)
      (ce/transform-keys c/->camelCaseString sub-db))))

(defn variables
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
    (util/namespace-map "wh.company.create-job.db" job)
    (update job ::create-job/verticals set)
    (update job ::create-job/manager get-manager-name)
    (update job ::create-job/tags #(set (map (partial hash-map :selected true :tag) %)))
    (update job ::create-job/benefits #(set (map (partial hash-map :selected true :tag) %)))))

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

(reg-event-db
  ::select-company-suggestion
  create-job-interceptors
  (fn [db [id]]
    (let [company (company-by-id db id)]
      (-> db
          (assoc ::create-job/company-id id)
          (assoc ::create-job/company-name (:name company))
          (assoc ::create-job/company__integrations (:integrations company))))))

(reg-event-fx
  ::edit-company-name
  create-job-interceptors
  (fn [{db :db} [new-value]]
    {:db (assoc db
                ::create-job/company-name new-value
                ::create-job/company-id nil)
     :dispatch [::fetch-companies new-value]}))

(reg-event-db
  ::edit-ats-job-id
  create-job-interceptors
  (fn [db [new-value]]
    (assoc db ::create-job/ats-job-id new-value)))

(reg-event-db
  ::toggle-vertical
  create-job-interceptors
  (fn [db [new-value]]
    (update db ::create-job/verticals util/toggle-unless-empty new-value)))

(reg-event-fx
  ::scroll-into-view
  db/default-interceptors
  (fn [_ [id]]
    {:scroll-into-view id}))

(reg-event-fx
  ::create-job
  db/default-interceptors
  (fn [{db :db} _]
    (let [errors            (create-job/invalid-fields db)
          include-location? (some #(when (str/starts-with? (name %) "location") %) errors)]
      (if errors
        (do
          (js/console.error "Errors in form:" errors)
          (merge {:db (-> db
                          (assoc-in [::create-job/sub-db ::create-job/error] nil)
                          (assoc-in [::create-job/sub-db ::create-job/saving?] false)
                          (assoc-in [::create-job/sub-db ::create-job/form-errors] (set errors)))
                  :dispatch-debounce {:id       :scroll-to-error-after-pause
                                      :dispatch [::scroll-into-view (db/key->id (first errors))]
                                      :timeout  50}}
                 (when include-location?
                   {:dispatch [::set-editing-address true false]})))
        {:db      (-> db (assoc-in [::create-job/sub-db ::create-job/error] nil)
                      (assoc-in [::create-job/sub-db ::create-job/saving?] true))
         :graphql {:query      (if (create-job/edit? db)
                                 update-job-mutation
                                 create-job-mutation)
                   :variables  (variables db)
                   :on-success [::create-job-success]
                   :on-failure [::create-job-error]}}))))

(reg-event-fx
  ::create-job-success
  db/default-interceptors
  (fn [{db :db} [{data :data}]]
    (let [job-id (-> data vals first :id)]
      {:db       (-> db
                     (assoc-in [::job/sub-db ::job/id] "")  ;; force reload
                     (assoc-in [::create-job/sub-db ::create-job/saving?] false))
       :navigate [:job :params {:id job-id}]})))

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
    (let [new-db (create-job/initial-db db)]
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

(def job-fields
  (let [all-fields (->> create-job/fields
                        (keys)
                        (map (comp cases/->camel-case-str #(str/replace % #"__" "##") name))
                        (set))
        top-fields (->> all-fields
                        (remove (partial re-find #"##")))
        nst-fields (->> top-fields
                        (set/difference all-fields)
                        (map #(str/split % #"##" ))
                        (group-by first)
                        (reduce-kv (fn [a k v] (conj a [(keyword k) (mapv (comp keyword second) v)])) []))]
    (concat (map keyword top-fields) nst-fields)))

(defn job-query [id db]
  {:venia/queries [[:job {:id id} (concat job-fields [[:company [[:integrations [[:greenhouse [:enabled [:jobs [:id :name]]]]]]]]])]]})

(reg-event-fx
  ::load-job
  db/default-interceptors
  (fn [{db :db} [id]]
    {:dispatch [::pages/set-loader]
     :graphql  {:query      (job-query id db)
                :on-success [::load-job-success]
                :on-failure [::load-job-failure [::load-job id]]}}))

(reg-event-fx
  ::load-job-success
  db/default-interceptors
  (fn [{db :db} [{:keys [data]}]]
    (let [new-db (merge (::create-job/sub-db db) (graphql-job->sub-db (:job data)))
          save? (get (::db/query-params db) "save")]
      {:db         (assoc db ::create-job/sub-db new-db)
       :dispatch-n (concat [[::pages/unset-loader]
                            [::close-search-location-form]]
                           (when save?
                             [[::create-job]]))})))

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

(defmethod on-page-load :create-job [db]
  [[::initialize-db]
   [:google/load-maps]
   [::fetch-tags]])

(defmethod on-page-load :edit-job [db]
  (concat [[::initialize-db]
           [:google/load-maps]
           [::fetch-tags]
           [::load-job (get-in db [::db/page-params :id])]]))

(reg-event-db
  ::set-location-suggestions
  create-job-interceptors
  (fn [db [suggestions]]
    (assoc db ::create-job/location-suggestions suggestions)))

(reg-event-fx
  ::select-location-suggestion
  create-job-interceptors
  (fn [{db :db} [id]]
    {:google/place-details {:place-id   id
                            :on-success [::fetch-place-details-success]
                            :on-failure [:error/set-global "Failed to fetch data from Google."]}}))

(reg-event-fx
  ::fetch-place-details-success
  create-job-interceptors
  (fn [{db :db} [google-response]]
    (let [{:keys [street city country country-code state post-code latitude longitude]}
          (common/google-place->location google-response)]
      {:db (assoc db
                  ::create-job/location__street street
                  ::create-job/location__city city
                  ::create-job/location__country country
                  ::create-job/location__country-code country-code
                  ::create-job/location__state state
                  ::create-job/location__post-code post-code
                  ::create-job/location__latitude latitude
                  ::create-job/location__longitude longitude)
       :dispatch-n [[::set-location-suggestions []]
                    [::open-search-location-form]]})))

(reg-event-fx
  ::search-location
  create-job-interceptors
  (fn [{db :db} [retry-num]]
    {:google/place-predictions {:input      (get db ::create-job/search-address)
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
    (assoc db ::create-job/tagline (apply str (take create-job/tagline-max-length tagline)))))

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
  ::edit-location__country
  create-job-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::create-job/location__country new-value)}
      (if (seq new-value)
        {:dispatch-n [[::attempt-country-code-assignment new-value]
                      [::process-countries new-value]]}
        {:dispatch [::set-country-suggestions []]}))))

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

(reg-event-fx
  ::toggle-benefit
  create-job-interceptors
  (fn [{db :db} [tag]]
    {:db (update db ::create-job/benefits util/toggle {:tag tag :selected true})
     :dispatch [::set-benefit-search ""]}))

(reg-event-db
  ::set-benefit-search
  create-job-interceptors
  (fn [db [benefit-search]]
    (assoc db ::create-job/benefit-search benefit-search)))

;; Tags

(reg-event-db
  ::toggle-tags-collapsed
  create-job-interceptors
  (fn [db _]
    (update db ::create-job/tags-collapsed? not)))

(reg-event-fx
  ::toggle-tag
  create-job-interceptors
  (fn [{db :db} [tag]]
    {:db (update db ::create-job/tags util/toggle {:tag tag :selected true})
     :dispatch [::set-tag-search ""]}))

(reg-event-db
  ::set-tag-search
  create-job-interceptors
  (fn [db [tag-search]]
    (assoc db ::create-job/tag-search tag-search)))

(reg-event-fx
  ::fetch-tags
  create-job-interceptors
  (fn [{db :db} _]
    (when-not (get db ::create-job/available-tags)
      {:graphql (fetch-tags ::fetch-tags-success)})))

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