(ns wh.jobs.jobsboard.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.algolia]
    [wh.common.cases :as cases]
    [wh.common.job :as job]
    [wh.common.search :as search]
    [wh.db :as db]
    [wh.graphql.jobs]
    [wh.jobs.jobsboard.db :as jobsboard]
    [wh.pages.core :refer [on-page-load] :as pages]
    [wh.user.db :as user-db]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def jobsboard-interceptors (into db/default-interceptors
                                  [(path ::jobsboard/sub-db)]))

(reg-event-db
  ::initialize-db
  jobsboard-interceptors
  (fn [_ _]
    jobsboard/default-db))


(defquery jobs-query
  {:venia/operation {:operation/type :query
                     :operation/name "jobs_search"}
   :venia/variables [{:variable/name "vertical" :variable/type :vertical}
                     {:variable/name "search_term" :variable/type :String!}
                     {:variable/name "preset_search" :variable/type :String}
                     {:variable/name "page" :variable/type :Int!}
                     {:variable/name "filters" :variable/type :SearchFiltersInput}]
   :venia/queries   [[:jobs_search {:vertical      :$vertical
                                    :search_term   :$search_term
                                    :preset_search :$preset_search
                                    :page          :$page
                                    :filters       :$filters}
                      [:numberOfPages
                       :numberOfHits
                       :hitsPerPage
                       :page
                       [:facets [:attr :value :count]]
                       [:searchParams [:label
                                       :query
                                       [:filters [:remote :roleType :sponsorshipOffered :published :tags :manager
                                                  [:location [:cities :countryCodes :regions]]
                                                  [:remuneration [:min :max :currency :timePeriod]]]]]]
                       [:promoted [:fragment/jobCardFields]]
                       [:jobs [:fragment/jobCardFields]]]]
                     [:city_info [:city :country :countryCode :region]]
                     [:remuneration_ranges [:currency :timePeriod :min :max]]]})

(reg-event-fx
  ::fetch-jobs
  db/default-interceptors
  (fn [{db :db} [initial-load?]]
    (let [all-jobs? (and (str/blank? (::db/search-term db))
                         (empty? (::db/query-params db)))
          cities (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/cities]) ;; for preset-search
          countries (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/countries]) ;; for preset-search
          remote? (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/remote]) ;; for preset-search
          tags (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/tags]) ;; for preset search
          preset-search? (= :pre-set-search (::db/page db))
          preset-search (if initial-load? (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/preset-search])
                                          (str/replace (::db/uri db) #"/" ""))
          db-filters (merge (if (and initial-load? preset-search?)
                              (cond-> {}
                                      (seq tags) (assoc :tags tags)
                                      (seq cities) (update-in [:location :cities] (fnil concat []) cities)
                                      (seq countries) (update-in [:location :country-codes] (fnil concat []) countries)
                                      remote? (assoc :remote true))
                              {})
                            (search/query-params->filters (::db/query-params db)))
          page-from-params (int (get-in db [::db/query-params "page"]))
          page (if (zero? page-from-params)
                 (inc page-from-params)
                 page-from-params)
          filters (cases/->camel-case db-filters)]
      {:db      (assoc-in db [::jobsboard/sub-db ::jobsboard/current-page-number] page)
       :graphql {:query      jobs-query
                 :variables  {:search_term   (or (get-in db [::db/query-params "search"]) "")
                              :preset_search (when preset-search? preset-search)
                              :page          page
                              :filters       filters
                              :vertical      (:wh.db/vertical db)}
                 :on-success [::fetch-jobs-success all-jobs? page]}})))

(reg-event-fx
  ::fetch-jobs-success
  db/default-interceptors
  (fn [{{:keys [::jobsboard/sub-db] :as db} :db} [all-jobs? page-number {{jobs-search :jobs_search city-info :city_info remuneration-ranges :remuneration_ranges} :data}]]
    (let [facets (group-by :attr (:facets jobs-search))
          search-params (:searchParams jobs-search)
          user-email (get-in db [:wh.user.db/sub-db :wh.user.db/email])
          salary-type-mapping {"Daily" :day "Yearly" :year}
          min-salary (get-in search-params [:filters :remuneration :min])
          max-salary (get-in search-params [:filters :remuneration :max])
          published (get-in search-params [:filters :published])
          role-type (get-in search-params [:filters :roleType])
          search-data (cond->
                        #:wh.search{:available-role-types (facets "role-type")
                                    :tags                 (set (get-in search-params [:filters :tags]))
                                    :cities               (set (get-in search-params [:filters :location :cities]))
                                    :countries            (set (get-in search-params [:filters :location :countryCodes]))
                                    :wh-regions           (set (map keyword (get-in search-params [:filters :location :regions])))
                                    :remote               (boolean (get-in search-params [:filters :remote]))
                                    :sponsorship          (boolean (get-in search-params [:filters :sponsorshipOffered]))
                                    :only-mine            (boolean (and user-email (= user-email (get-in search-params [:filters :manager]))))
                                    :role-types           (if role-type (set [role-type]) ;; TODO now we only pass one, should we get more?
                                                                        (:role-types jobsboard/default-db))
                                    :published            (if published (set [published])
                                                                        (:published jobsboard/default-db))
                                    :currency             (or (get-in search-params [:filters :remuneration :currency])
                                                              (:wh.search/currency jobsboard/default-db))
                                    :salary-type          (get salary-type-mapping (get-in search-params [:filters :remuneration :timePeriod]))
                                    :salary-range         (when (and min-salary max-salary) [min-salary max-salary])
                                    :query                (:query search-params)
                                    :available-tags       (facets "tags")
                                    :available-wh-regions (facets "wh-region")
                                    :available-cities     (facets "location.city")
                                    :available-countries  (facets "location.country-code")
                                    :remote-count         (->> (facets "remote") (filter #(= (:value %) "true")) first :count)
                                    :sponsorship-count    (->> (facets "sponsorship-offered") (filter #(= (:value %) "true")) first :count)}
                        (user-db/admin? db) (assoc :wh.search/mine-count (->> (facets "manager") (filter #(= (:value %) user-email)) first :count)
                                                   :wh.search/published-count (facets "published"))
                        city-info (assoc :wh.search/city-info
                                         (mapv (fn [loc]
                                                 (as-> loc loc
                                                       (cases/->kebab-case loc)
                                                       (update loc :region keyword)))
                                               city-info)
                                         :wh.search/salary-ranges
                                         (mapv cases/->kebab-case remuneration-ranges)))
          jobsboard-db #:wh.jobs.jobsboard.db{:jobs                     (if (= page-number 1)
                                                                          (mapv job/translate-job (:jobs jobs-search))
                                                                          (concat (::jobsboard/jobs sub-db) (mapv job/translate-job (:jobs jobs-search))))
                                              :promoted-jobs            (mapv job/translate-job (:promoted jobs-search))
                                              :number-of-search-results (:numberOfHits jobs-search)
                                              :current-page             (:page jobs-search)
                                              :total-pages              (:numberOfPages jobs-search)
                                              :search-term-for-results  (:query search-params)}]
      {:db         (-> db
                       (update ::jobsboard/sub-db (fn [current-jobsboard]
                                                    (-> current-jobsboard
                                                        (assoc ::jobsboard/all-jobs? all-jobs?)
                                                        (merge jobsboard-db)
                                                        (update ::jobsboard/search merge search-data))))
                       (merge {::db/search-term  (:query search-params)
                               ::db/search-label (:label search-params)}))
       :dispatch-n [[:wh.search/in-progress false]
                    [::pages/unset-loader]]})))

(reg-event-db
  ::toggle-filter
  db/default-interceptors
  (fn [db _]
    (update db ::db/filter-shown? not)))

(reg-event-db
  :wh.search/in-progress
  db/default-interceptors
  (fn [db [searching]]
    (assoc-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/searching] searching)))

(reg-event-db
  :wh.search/set-query
  jobsboard-interceptors
  (fn [db [query]]
    (assoc-in db [::jobsboard/search :wh.search/query] query)))

(reg-event-db
  :wh.search/set-tag-part
  jobsboard-interceptors
  (fn [db [value]]
    (assoc-in db [::jobsboard/search :wh.search/tag-part] value)))

(reg-event-fx
  :wh.search/toggle-remote
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update-in db [::jobsboard/search :wh.search/remote] not)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-city
  jobsboard-interceptors
  (fn [{db :db} [city]]
    {:db       (update-in db [::jobsboard/search :wh.search/cities] util/toggle city)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-country
  jobsboard-interceptors
  (fn [{db :db} [country]]
    {:db       (update-in db [::jobsboard/search :wh.search/countries] util/toggle country)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-wh-region
  jobsboard-interceptors
  (fn [{db :db} [wh-region]]
    {:db       (update-in db [::jobsboard/search :wh.search/wh-regions] util/toggle wh-region)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-sponsorship
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update-in db [::jobsboard/search :wh.search/sponsorship] not)
     :dispatch [:wh.search/search]}))

(reg-event-db
  :wh.search/toggle-tags-collapsed
  jobsboard-interceptors
  (fn [db _]
    (update-in db [::jobsboard/search :wh.search/tags-collapsed?] not)))

(reg-event-fx
  :wh.search/toggle-role-type
  jobsboard-interceptors
  (fn [{db :db} [role-type]]
    {:db       (update-in db [::jobsboard/search :wh.search/role-types] util/toggle role-type)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/search-by-tag
  jobsboard-interceptors
  (fn [{db :db} [tag toggle?]]
    {:db       (update-in db [::jobsboard/search :wh.search/tags]
                          (if toggle? util/toggle conj)
                          tag)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/clear-locations
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update db ::jobsboard/search merge
                       {:wh.search/wh-regions #{}
                        :wh.search/cities     #{}
                        :wh.search/countries  #{}})
     :dispatch [:wh.search/search]}))

(reg-event-db
  :wh.search/initialize-widgets
  db/default-interceptors
  (fn [db _]
    (let [params (:wh.db/query-params db)
          val-set (fn [k] (set (when-let [v (params k)] (str/split v #";"))))
          val-bool (fn [k] (= (params k) "true"))]
      (-> db
          (assoc :wh.db/search-term nil) ; if we came here from pre-set search, we reset the term because we have the query in url
          (update-in [::jobsboard/sub-db ::jobsboard/search] merge
                     {:wh.search/query       (params "search")
                      :wh.search/tags        (val-set "tags")
                      :wh.search/role-types  (val-set "role-type")
                      :wh.search/cities      (val-set "location.city")
                      :wh.search/countries   (val-set "location.country-code")
                      :wh.search/wh-regions  (set (map keyword (val-set "wh-region")))
                      :wh.search/remote      (val-bool "remote")
                      :wh.search/sponsorship (val-bool "sponsorship-offered")
                      :wh.search/salary      (js/parseInt (params "remuneration.min"))
                      :wh.search/salary-type ({"Yearly" :year, "Daily" :day} (params "remuneration.time-period"))})))))

(defn- salary-params
  [{[min max] :wh.search/salary-range, type :wh.search/salary-type, currency :wh.search/currency}]
  (cond-> {:remuneration.time-period ({:year "Yearly", :day "Daily"} type)}
          min (assoc :remuneration.min min)
          max (assoc :remuneration.max max)
          currency (assoc :remuneration.currency currency)))

(defn- search-params
  [criteria query-only? app-db]
  (let [criteria (cond-> criteria query-only? (select-keys [:wh.search/query]))
        {:keys [:wh.search/query :wh.search/tags :wh.search/role-types
                :wh.search/remote :wh.search/sponsorship
                :wh.search/wh-regions :wh.search/cities :wh.search/countries
                :wh.search/salary-type :wh.search/only-mine :wh.search/published]} criteria]
    [:jobsboard
     :query-params (cond-> {}
                           (not (str/blank? query)) (assoc :search query)
                           remote (assoc :remote "true")
                           sponsorship (assoc :sponsorship-offered "true")
                           (seq tags) (assoc :tags (str/join ";" tags))
                           (seq wh-regions) (assoc :wh-region (str/join ";" (map name wh-regions)))
                           (seq cities) (assoc :location.city (str/join ";" cities))
                           (seq countries) (assoc :location.country-code (str/join ";" countries))
                           (seq role-types) (assoc :role-type (str/join ";" role-types))
                           salary-type (merge (salary-params criteria))
                           only-mine (assoc :manager (get-in app-db [:wh.user.db/sub-db :wh.user.db/email]))
                           (and published (= (count published) 1)) (assoc :published (first published)))]))

(reg-event-fx
  :wh.search/search
  db/default-interceptors
  (fn [{db :db} [query-only?]]
    {:navigate (search-params (get-in db [::jobsboard/sub-db ::jobsboard/search]) query-only? db)}))

(defmethod on-page-load :jobsboard [db]
  (conj (if (and (::db/server-side-rendered? db)
                 (::db/initial-load? db))
          []
          [[:wh.search/in-progress true]])
        [::fetch-jobs (::db/initial-load? db)] ;; TODO put this back in the if statement when filters are finished
        [:wh.search/initialize-widgets]
        [:wh.events/scroll-to-top]))

(defmethod on-page-load :pre-set-search [db]
  (conj (if (and (::db/server-side-rendered? db)
                 (::db/initial-load? db))
          []
          [[:wh.search/in-progress true]])
        [::fetch-jobs (::db/initial-load? db)] ;; TODO put this back in the if statement when filters are finished
        [:wh.events/scroll-to-top]))

(defn- update-min-max
  [{:keys [wh.search/salary-type wh.search/currency wh.search/salary-ranges] :as search}]
  (if-not salary-type
    search
    (let [grouped-ranges (group-by (juxt :currency :time-period) salary-ranges)
          range (first (grouped-ranges [currency ({:year "Yearly", :day "Daily"} salary-type)]))]
      (if-not range
        search
        (assoc search
          :wh.search/salary-range [(:min range) (:max range)])))))

(reg-event-fx
  :wh.search/set-salary-type
  jobsboard-interceptors
  (fn [{db :db} [type]]
    {:db       (-> db
                   (update-in [::jobsboard/search :wh.search/salary-type] #(if (= %1 type) nil type))
                   (update ::jobsboard/search update-min-max))
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/set-currency
  jobsboard-interceptors
  (fn [{db :db} [currency]]
    {:db       (-> db
                   (assoc-in [::jobsboard/search :wh.search/currency] currency)
                   (update ::jobsboard/search update-min-max))
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/set-salary-range
  jobsboard-interceptors
  (fn [{db :db} [salary-range]]
    {:db                (-> db
                            (assoc-in [::jobsboard/search :wh.search/salary-range] salary-range)
                            (update-in [::jobsboard/search :wh.search/salary-type] #(if % % :year)))
     :dispatch-debounce {:id       :search-after-setting-salary-range
                         :dispatch [:wh.search/search]
                         :timeout  100}}))

(reg-event-fx
  :wh.search/toggle-only-mine
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update-in db [::jobsboard/search :wh.search/only-mine] not)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-published
  jobsboard-interceptors
  (fn [{db :db} [value]]
    {:db       (update-in db [::jobsboard/search :wh.search/published] util/toggle value)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/reset-locations
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update db ::jobsboard/search merge
                       {:wh.search/cities     #{}
                        :wh.search/countries  #{}
                        :wh.search/wh-regions #{}})
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/reset-salary
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update db ::jobsboard/search merge
                       {:wh.search/salary-range nil
                        :wh.search/salary-type  nil})
     :dispatch [:wh.search/search]}))
