(ns wh.jobs.jobsboard.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
            [wh.algolia]
            [wh.common.cases :as cases]
            [wh.common.job :as job]
            [wh.common.search :as search]
            [wh.common.url :as url]
            [wh.db :as db]
            [wh.graphql.jobs]
            [wh.jobs.jobsboard.db :as jobsboard]
            [wh.jobs.jobsboard.events.fetch-jobs-success :as fetch-jobs-success]
            [wh.jobs.jobsboard.queries :as queries]
            [wh.jobsboard-ssr.db :as jobsboard-ssr]
            [wh.pages.core :refer [on-page-load] :as pages]
            [wh.user.db :as user-db]
            [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(def jobsboard-interceptors (into db/default-interceptors
                                  [(path ::jobsboard/sub-db)]))

(reg-event-db
  ::initialize-db
  jobsboard-interceptors
  (fn [_ _]
    jobsboard/default-db))

(reg-event-fx
  ::fetch-jobs
  db/default-interceptors
  (fn [{db :db} [initial-load? query-variables page all-jobs?]]
    {:db      (assoc-in db [::jobsboard/sub-db ::jobsboard/current-page-number] page)
     :graphql {:query      queries/jobs-query
               :variables  query-variables
               :on-success [::fetch-jobs-success all-jobs? page]}}))

(reg-event-fx
  ::set-jobs-query
  db/default-interceptors
  (fn [{db :db} [initial-load?]]
    (let [all-jobs?        (and (str/blank? (::db/search-term db))
                                (empty? (::db/query-params db)))
          cities           (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/cities])    ;; for preset-search
          countries        (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/countries]) ;; for preset-search
          remote?          (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/remote])    ;; for preset-search
          tags             (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/tags])      ;; for preset search
          preset-search?   (= :pre-set-search (::db/page db))
          preset-search    (if initial-load?
                             (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/preset-search])
                             (url/strip-query-params (str/replace (::db/uri db) #"/" "")))
          db-filters       (->
                             (merge (if (and initial-load? preset-search?)
                                      (cond-> {}
                                              (seq tags)      (assoc :tags tags)
                                              (seq cities)    (update-in [:location :cities] (fnil concat []) cities)
                                              (seq countries) (update-in [:location :country-codes] (fnil concat []) countries)
                                              remote?         (assoc :remote true))
                                      {})
                                    (search/query-params->filters (::db/query-params db)))
                             (util/update* "sponsorship-offered" util/string->boolean)
                             (util/update* "remote" util/string->boolean)
                             (util/update-in* [:remuneration :min] js/parseInt)
                             (util/update-in* [:remuneration :max] js/parseInt)
                             (util/update-in* [:remuneration :competitive] util/string->boolean))
          page-from-params (int (get-in db [::db/query-params "page"]))
          page             (if (zero? page-from-params)
                             (inc page-from-params)
                             page-from-params)
          filters          (cases/->camel-case db-filters)
          query-variables  {:search_term   (or (get-in db [::db/query-params "search"]) "")
                            :preset_search (when preset-search? preset-search)
                            :page          page
                            :filters       filters
                            :vertical      (:wh.db/vertical db)}]
      (when-not (= query-variables (::jobsboard/query-variables db))
        {:db         (assoc db ::jobsboard/query-variables query-variables)
         :dispatch-n [[::fetch-jobs initial-load? query-variables page all-jobs?]
                      [:wh.search/in-progress true]]}))))


(reg-event-fx
  ::fetch-jobs-success
  db/default-interceptors
  fetch-jobs-success/handler)

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
  :wh.search/toggle-show-competitive
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update-in db [::jobsboard/search :wh.search/competitive] not)
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
    (let [params                (:wh.db/query-params db)
          val-set               (fn [k] (set (when-let [v (params k)] (str/split v #";"))))
          val-bool              (fn [k] (= (params k) "true"))
          val-bool-default-true (fn [k] (not= (params k) "false"))]
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
                      :wh.search/competitive (val-bool-default-true "remuneration.competitive")
                      :wh.search/salary      (js/parseInt (params "remuneration.min"))
                      :wh.search/salary-type ({"Yearly" :year, "Daily" :day} (params "remuneration.time-period"))})))))

(defn- salary-params
  [competitive? {[min max] :wh.search/salary-range, type :wh.search/salary-type, currency :wh.search/currency}]
  (cond-> {}
          type               (assoc :remuneration.time-period ({:year "Yearly", :day "Daily"} type))
          min                (assoc :remuneration.min (js/parseInt min))
          max                (assoc :remuneration.max (js/parseInt max))
          currency           (assoc :remuneration.currency currency)
          (not competitive?) (assoc :remuneration.competitive false)))

(defn- search-params
  [criteria query-only? app-db]
  (let [criteria                         (cond-> criteria query-only? (select-keys [:wh.search/query]))
        {:keys [:wh.search/query :wh.search/tags :wh.search/role-types
                :wh.search/remote :wh.search/sponsorship :wh.search/currency
                :wh.search/wh-regions :wh.search/cities :wh.search/countries
                :wh.search/salary-type :wh.search/only-mine :wh.search/published
                :wh.search/competitive]} criteria
        view-type                        (get-in app-db [:wh.db/query-params jobsboard-ssr/view-type-param])]
    [:jobsboard
     :query-params (cond-> {}
                           (not (str/blank? query))                (assoc :search query)
                           remote                                  (assoc :remote "true")
                           sponsorship                             (assoc :sponsorship-offered "true")
                           (seq tags)                              (assoc :tags (str/join ";" tags))
                           (seq wh-regions)                        (assoc :wh-region (str/join ";" (map name wh-regions)))
                           (seq cities)                            (assoc :location.city (str/join ";" cities))
                           (seq countries)                         (assoc :location.country-code (str/join ";" countries))
                           (seq role-types)                        (assoc :role-type (str/join ";" role-types))
                           (or (and (not= "*" currency) salary-type)
                               (not competitive))                  (merge (salary-params competitive criteria))
                           only-mine                               (assoc :manager (get-in app-db [:wh.user.db/sub-db :wh.user.db/email]))
                           (and published (= (count published) 1)) (assoc :published (first published))
                           view-type                               (assoc :view-type (name view-type)))]))

(reg-event-fx
  :wh.search/search
  db/default-interceptors
  (fn [{db :db} [query-only?]]
    {:navigate (search-params
                (get-in db [::jobsboard/sub-db ::jobsboard/search])
                query-only? db)}))

(defmethod on-page-load :jobsboard [db]
  (conj (if (and (::db/server-side-rendered? db)
                 (::db/initial-load? db))
          []
          [[::set-jobs-query (::db/initial-load? db)]])
        [:wh.search/initialize-widgets]
        [:wh.events/scroll-to-top]))

(defmethod on-page-load :pre-set-search [db]
  (conj (if (and (::db/server-side-rendered? db)
                 (::db/initial-load? db))
          []
          [[:wh.search/in-progress true]])
        [::set-jobs-query (::db/initial-load? db)] ;; TODO put this back in the if statement when filters are finished
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
