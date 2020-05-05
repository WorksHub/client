(ns wh.jobs.jobsboard.events.fetch-jobs-success
  (:require [wh.algolia]
            [wh.common.cases :as cases]
            [wh.common.job :as job]
            [wh.db :as db]
            [wh.graphql.jobs]
            [wh.jobs.jobsboard.db :as jobsboard]
            [wh.pages.core :as pages]
            [wh.user.db :as user-db]
            [clojure.string :as str]))

(defn- ui-tags [tags]
  (map (fn [{:keys [value count]}]
         {:attr "tags" :value (str/lower-case value) :count count}) tags))

(defn- get-search-data
  [db {:keys [city-info facets max-salary min-salary published remuneration-ranges role-type
              salary-type-mapping search-params user-email]}]
  (cond->
      #:wh.search{:available-role-types (facets "role-type")
                  :tags                 (set (get-in search-params [:filters :tags]))
                  :cities               (set (get-in search-params [:filters :location :cities]))
                  :countries            (set (get-in search-params [:filters :location :countryCodes]))
                  :wh-regions           (set (map keyword (get-in search-params [:filters :location :regions])))
                  :remote               (boolean (get-in search-params [:filters :remote]))
                  :sponsorship          (boolean (get-in search-params [:filters :sponsorshipOffered]))
                  :only-mine            (boolean (and user-email (= user-email (get-in search-params [:filters :manager]))))
                  :role-types           (if role-type
                                          (set [role-type]) ;; TODO now we only pass one, should we get more?
                                          (get-in jobsboard/default-db [::jobsboard/search :wh.search/role-types]))
                  :published            (if published
                                          (set [published])
                                          (get-in jobsboard/default-db [::jobsboard/search :wh.search/published]))
                  :currency             (or (get-in search-params [:filters :remuneration :currency])
                                            (get-in jobsboard/default-db [::jobsboard/search :wh.search/currency]))
                  :salary-type          (or (get salary-type-mapping (get-in search-params [:filters :remuneration :timePeriod]))
                                            (get-in jobsboard/default-db [::jobsboard/search :wh.search/salary-type]))
                  :salary-range         (when (and min-salary max-salary) [min-salary max-salary])
                  :query                (:query search-params)
                  :available-tags       (ui-tags (facets "tags.label"))
                  :available-wh-regions (facets "wh-region")
                  :available-cities     (facets "location.city")
                  :available-countries  (facets "location.country-code")
                  :remote-count         (->> (facets "remote") (filter #(= (:value %) "true")) first :count)
                  :sponsorship-count    (->> (facets "sponsorship-offered") (filter #(= (:value %) "true")) first :count)}
    (user-db/admin? db) (assoc :wh.search/mine-count (->> (facets "manager") (filter #(= (:value %) user-email)) first :count)
                               :wh.search/published-count (facets "published"))
    city-info           (assoc :wh.search/city-info
                               (mapv (fn [loc]
                                       (as-> loc loc
                                         (cases/->kebab-case loc)
                                         (update loc :region keyword)))
                                     city-info)
                               :wh.search/salary-ranges
                               (mapv cases/->kebab-case remuneration-ranges))))

(defn- organize-search-params [db jobs-search search-params remuneration-ranges city-info]
  (let [facets (group-by :attr (:facets jobs-search))]
    {:city-info           city-info
     :facets              facets
     :max-salary          (get-in search-params [:filters :remuneration :max])
     :min-salary          (get-in search-params [:filters :remuneration :min])
     :published           (get-in search-params [:filters :published])
     :remuneration-ranges remuneration-ranges
     :role-type           (get-in search-params [:filters :roleType])
     :salary-type-mapping {"Daily" :day "Yearly" :year}
     :search-params       search-params
     :user-email          (get-in db [:wh.user.db/sub-db :wh.user.db/email])}))

(defn handler
  [{{:keys [::jobsboard/sub-db] :as db} :db}
   [all-jobs? page-number {{jobs-search :jobs_search city-info :city_info remuneration-ranges :remuneration_ranges} :data}]]
  (let [search-params (:searchParams jobs-search)
        search-data   (get-search-data
                       db (organize-search-params db jobs-search search-params remuneration-ranges city-info))
        jobsboard-db  #:wh.jobs.jobsboard.db{:jobs                     (mapv job/translate-job (:jobs jobs-search))
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
                  [::pages/unset-loader]]}))
