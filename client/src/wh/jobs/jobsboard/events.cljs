(ns wh.jobs.jobsboard.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.algolia]
            [wh.common.cases :as cases]
            [wh.common.search :as search]
            [wh.common.url :as url]
            [wh.common.user :as user-common]
            [wh.db :as db]
            [wh.graphql-cache :as gqlc :refer [reg-query]]
            [wh.graphql.jobs]
            [wh.jobs.jobsboard.db :as jobsboard]
            [wh.jobs.jobsboard.queries :as queries]
            [wh.jobs.jobsboard.search-results :as search-results]
            [wh.jobsboard.db :as jobsboard-ssr]
            [wh.landing-new.events :as landing-events]
            [wh.pages.core :as pages]
            [wh.util :as util]))

(def jobsboard-interceptors (into db/default-interceptors
                                  [(path ::jobsboard/sub-db)]))

(reg-event-db
  ::initialize-db
  jobsboard-interceptors
  (fn [_ _]
    jobsboard/default-db))


(defn get-db-filters [{:keys [preset-search? db]}]
  (let [cities    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/cities])
        regions   (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/regions])
        countries (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/countries])
        remote?   (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/remote])
        tags      (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/tags])]
    (->
      (if preset-search?
        (cond-> {}
                (seq tags)      (assoc :tags tags)
                (seq cities)    (update-in [:location :cities] (fnil concat []) cities)
                (seq regions)   (update-in [:location :regions] (fnil concat []) regions)
                (seq countries) (update-in [:location :country-codes] (fnil concat []) countries)
                remote?         (assoc :remote true))
        {})
      (merge (search/query-params->filters (::db/query-params db)))
      (util/update* "sponsorship-offered" util/string->boolean)
      (util/update* "remote" util/string->boolean)
      (util/update-in* [:remuneration :min] js/parseInt)
      (util/update-in* [:remuneration :max] js/parseInt)
      (util/update-in* [:remuneration :competitive] util/string->boolean))))


(defn get-query-variables [db]
  (let [initial-load?    (::db/initial-load? db)
        preset-search?   (= :pre-set-search (::db/page db))
        preset-search    (if initial-load?
                           (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/preset-search])
                           (url/strip-query-params (str/replace (::db/uri db) #"/" "")))
        db-filters       (get-db-filters {:db             db
                                          :initial-load?  initial-load?
                                          :preset-search? preset-search?})
        page-from-params (int (get-in db [::db/query-params "page"]))
        page             (if (zero? page-from-params)
                           (inc page-from-params)
                           page-from-params)
        filters          (cases/->camel-case db-filters)]
    {:search_term     (or (get-in db [::db/query-params "search"]) "")
     :preset_search   (when preset-search? preset-search)
     :page            page
     :filters         filters
     :promoted_amount 2
     :vertical        (:wh.db/vertical db)}))

(reg-query :jobs queries/jobs-query)

(defn jobs [db]
  [:jobs (get-query-variables db)])

(reg-event-fx
  ::set-jobs-query
  db/default-interceptors
  (fn [{db :db} _]
    (let [query-variables (get-query-variables db)]
      (when-not (= query-variables (::jobsboard/query-variables db))
        {:scroll-to-top true
         :db            (assoc db ::jobsboard/query-variables query-variables)
         :dispatch      (into [:graphql/query] (conj (jobs db) {:on-success [::fetch-jobs-success]}))}))))

(reg-event-db
  ::fetch-jobs-success
  db/default-interceptors
  (fn [db _]
    (let [result  (gqlc/cache-results jobs db [])
          filters (get-in db [::jobsboard/search :wh.search/filters])]
      ;; we want to update filters at first possible opportunity, and do not update them anymore
      (if (and result (not filters))
        (assoc-in db [::jobsboard/search :wh.search/filters] (search-results/get-search-filters result))
        db))))

(reg-event-db
  :wh.search/set-query
  jobsboard-interceptors
  (fn [db [query]]
    (assoc-in db [::jobsboard/search :wh.search/query] query)))

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
  :wh.search/toggle-region
  jobsboard-interceptors
  (fn [{db :db} [region]]
    {:db       (update-in db [::jobsboard/search :wh.search/regions] util/toggle region)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-country
  jobsboard-interceptors
  (fn [{db :db} [country]]
    {:db       (update-in db [::jobsboard/search :wh.search/countries] util/toggle country)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-sponsorship
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update-in db [::jobsboard/search :wh.search/sponsorship] not)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/toggle-role-type
  jobsboard-interceptors
  (fn [{db :db} [role-type]]
    {:db       (update-in db [::jobsboard/search :wh.search/role-types] util/toggle role-type)
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/clear-role-types
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (assoc-in db [::jobsboard/search :wh.search/role-types] #{})
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
  :wh.search/clear-tags
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (assoc-in db [::jobsboard/search :wh.search/tags] #{})
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/clear-locations-and-perks
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update db ::jobsboard/search merge
                       {:wh.search/cities      #{}
                        :wh.search/countries   #{}
                        :wh.search/sponsorship false
                        :wh.search/remote      false})
     :dispatch [:wh.search/search]}))

(reg-event-db
  :wh.search/initialize-widgets
  db/default-interceptors
  (fn [db _]
    (let [params                (:wh.db/query-params db)
          val                   (fn [k] (some-> (params k) (str/split #";") (first)))
          val-int               (fn [k] (some-> (val k) (js/parseInt)))
          val-set               (fn [k] (set (when-let [v (params k)] (str/split v #";"))))
          val-bool              (fn [k] (= (params k) "true"))
          val-bool-default-true (fn [k] (not= (params k) "false"))
          rem-min               (val-int "remuneration.min")
          rem-max               (val-int "remuneration.max")]
      (-> db
          ;; if we came here from pre-set search, we reset the term because we have the query in url
          (assoc :wh.db/search-term nil)
          (update-in [::jobsboard/sub-db ::jobsboard/search] merge
                     {:wh.search/query        (params "search")
                      :wh.search/tags         (val-set "tags")
                      :wh.search/role-types   (val-set "role-type")
                      :wh.search/cities       (val-set "location.city")
                      :wh.search/regions      (val-set "location.region")
                      :wh.search/countries    (val-set "location.country-code")
                      :wh.search/remote       (val-bool "remote")
                      :wh.search/sponsorship  (val-bool "sponsorship-offered")
                      :wh.search/competitive  (val-bool-default-true "remuneration.competitive")
                      :wh.search/salary       (js/parseInt (params "remuneration.min"))
                      :wh.search/salary-type  (get {"Yearly" :year, "Daily" :day}
                                                   (params "remuneration.time-period"))
                      :wh.search/salary-range (when (and rem-min rem-max)
                                                [rem-min rem-max])
                      :wh.search/salary-from  rem-min
                      :wh.search/currency     (val "remuneration.currency")})))))

(defn- salary-params
  "helps to compose a query params object to use when we build a url to redirect to"
  [competitive? {[min max]   :wh.search/salary-range
                 type        :wh.search/salary-type
                 currency    :wh.search/currency
                 salary-from :wh.search/salary-from}]
  (cond-> {}
          type (assoc :remuneration.time-period ({:year "Yearly", :day "Daily"} type))
          min (assoc :remuneration.min (js/parseInt min))
          salary-from (assoc :remuneration.min (js/parseInt salary-from))
          max (assoc :remuneration.max (js/parseInt max))
          currency (assoc :remuneration.currency currency)
          (not competitive?) (assoc :remuneration.competitive false)))

(defn- search-params
  [criteria query-only? app-db]
  (let [criteria                         (cond-> criteria query-only? (select-keys [:wh.search/query]))
        {:keys [:wh.search/query :wh.search/tags :wh.search/role-types
                :wh.search/remote :wh.search/sponsorship :wh.search/currency
                :wh.search/cities :wh.search/countries :wh.search/regions
                :wh.search/salary-type :wh.search/only-mine :wh.search/published
                :wh.search/competitive]} criteria
        view-type                        (get-in app-db [:wh.db/query-params jobsboard-ssr/view-type-param])]
    [:jobsboard
     :query-params (cond-> {}
                           (not (str/blank? query))                (assoc :search query)
                           remote                                  (assoc :remote "true")
                           sponsorship                             (assoc :sponsorship-offered "true")
                           (seq tags)                              (assoc :tags (str/join ";" tags))
                           (seq cities)                            (assoc :location.city (str/join ";" cities))
                           (seq regions)                           (assoc :location.region (str/join ";" regions))
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


(defmethod pages/on-page-load :jobsboard [db]
  (concat [[:wh.search/initialize-widgets]
           [::set-jobs-query]
           (into [:graphql/query]
                 (if (user-common/candidate? db)
                   (landing-events/recommended-jobs db)
                   (landing-events/recent-jobs db)))]))

(defmethod pages/on-page-load :pre-set-search [_db]
  [[::set-jobs-query]])

(defn- update-min-max
  "updates possible values for salary selector"
  ;; TODO, ch5631: fix reframe database, see details in story description
  [{:keys [wh.search/salary-type wh.search/currency wh.search/salary-ranges] :as search}]
  (if-not salary-type
    search
    (let [grouped-ranges (group-by (juxt :currency :time-period) salary-ranges)
          range (first (grouped-ranges [currency ({:year "Yearly", :day "Daily"} salary-type)]))]
      (if-not range
        search
        (assoc search :wh.search/salary-range [(:min range) (:max range)]
                      :wh.search/salary-from (:min range))))))

(reg-event-fx
  :wh.search/set-salary-type
  jobsboard-interceptors
  (fn [{db :db} [type]]
    {:db       (-> db
                   (update-in [::jobsboard/search :wh.search/salary-type] #(if (= %1 type) nil type))
                   (assoc-in [::jobsboard/search :wh.search/salary-range] nil)
                   (assoc-in [::jobsboard/search :wh.search/salary-from] nil)
                   (update ::jobsboard/search update-min-max))
     :dispatch [:wh.search/search]}))

(defn assoc-currency [db currency]
  (if-not (= "*" currency)
    (assoc-in db [::jobsboard/search :wh.search/currency] currency)
    (update db ::jobsboard/search #(dissoc % :wh.search/currency))))

(reg-event-fx
  :wh.search/set-currency
  jobsboard-interceptors
  (fn [{db :db} [currency]]
    {:db       (-> db
                   (assoc-currency currency)
                   (assoc-in [::jobsboard/search :wh.search/salary-range] nil)
                   (assoc-in [::jobsboard/search :wh.search/salary-from] nil)
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
  :wh.search/set-salary-from
  jobsboard-interceptors
  (fn [{db :db} [salary-from]]
    {:db                (-> db
                            (assoc-in [::jobsboard/search :wh.search/salary-from] salary-from)
                            (update-in [::jobsboard/search :wh.search/salary-type] #(if % % :year)))
     :dispatch-debounce {:id       :search-after-setting-salary-from
                         :dispatch [:wh.search/search]
                         :timeout  400}}))

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
                        :wh.search/countries  #{}})
     :dispatch [:wh.search/search]}))

(reg-event-fx
  :wh.search/reset-salary
  jobsboard-interceptors
  (fn [{db :db} _]
    {:db       (update db ::jobsboard/search merge
                       {:wh.search/salary-range nil
                        :wh.search/salary-type  nil
                        :wh.search/currency     nil})
     :dispatch [:wh.search/search]}))
