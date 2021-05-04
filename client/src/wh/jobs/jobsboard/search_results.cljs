(ns wh.jobs.jobsboard.search-results
  (:require [clojure.string :as str]
            [wh.algolia]
            [wh.common.cases :as cases]
            [wh.common.job :as job]
            [wh.graphql.jobs]
            [wh.jobs.jobsboard.db :as jobsboard]))

(defn get-search-data
  [admin? {:keys [facets max-salary min-salary published role-type
                  salary-type-mapping search-params user-email]}]
  (cond->
    #:wh.search{:remote            (boolean (get-in search-params [:filters :remote]))
                :sponsorship       (boolean (get-in search-params [:filters :sponsorshipOffered]))
                :only-mine         (boolean (and user-email (= user-email (get-in search-params [:filters :manager]))))
                :role-types        (if role-type
                                     (set [role-type]) ;; TODO now we only pass one, should we get more?
                                     (get-in jobsboard/default-db [::jobsboard/search :wh.search/role-types]))
                :published         (if published
                                     (set [published])
                                     (get-in jobsboard/default-db [::jobsboard/search :wh.search/published]))
                :currency          (or (get-in search-params [:filters :remuneration :currency])
                                       (get-in jobsboard/default-db [::jobsboard/search :wh.search/currency]))
                :salary-type       (or (get salary-type-mapping (get-in search-params [:filters :remuneration :timePeriod]))
                                       (get-in jobsboard/default-db [::jobsboard/search :wh.search/salary-type]))
                :salary-range      (when (and min-salary max-salary) [min-salary max-salary])
                :query             (:query search-params)
                :remote-count      (->> (facets "remote") (filter #(= (:value %) "true")) first :count)
                :sponsorship-count (->> (facets "sponsorship-offered") (filter #(= (:value %) "true")) first :count)}
    admin? (assoc :wh.search/mine-count (->> (facets "manager") (filter #(= (:value %) user-email)) first :count)
                  :wh.search/published-count (facets "published"))))

(defn organize-search-params [user-email jobs-search search-params]
  (let [facets (group-by :attr (:facets jobs-search))]
    {:facets              facets
     :max-salary          (get-in search-params [:filters :remuneration :max])
     :min-salary          (get-in search-params [:filters :remuneration :min])
     :published           (get-in search-params [:filters :published])
     :role-type           (get-in search-params [:filters :role-type])
     :salary-type-mapping {"Daily" :day "Yearly" :year}
     :search-params       search-params
     :user-email          user-email}))


(defn get-jobsboard-db [jobs-search search-params]
  #:wh.jobs.jobsboard.db{:jobs                     (mapv job/translate-job
                                                         (:jobs jobs-search))
                         :promoted-jobs            (mapv job/translate-job
                                                         (:promoted jobs-search))
                         :number-of-search-results (:number-of-hits jobs-search)
                         :current-page             (:page jobs-search)
                         :total-pages              (:number-of-pages jobs-search)})


(defn ui-tags [tags]
  (map (fn [{:keys [value count]}]
         {:attr "tags" :value (str/lower-case value) :count count}) tags))

(defn get-search-filters [jobs-search]
  (let [city-info     (mapv (fn [loc]
                              (as-> loc loc
                                    (cases/->kebab-case loc)
                                    (update loc :region keyword)))
                            (:city-info jobs-search))
        salary-ranges (mapv cases/->kebab-case (:remuneration-ranges jobs-search))
        facets        (group-by :attr (:facets (:jobs-search jobs-search)))]
    #:wh.search{:available-role-types (facets "role-type")
                :available-tags       (ui-tags (facets "tags.label"))
                :available-cities     (facets "location.city")
                :available-countries  (facets "location.country-code")
                :available-regions    (facets "location.region")
                :city-info            city-info
                :salary-ranges        salary-ranges}))
