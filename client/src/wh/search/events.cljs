(ns wh.search.events
  (:require ["algoliasearch" :as algoliasearch]
            [clojure.walk :as walk]
            [re-frame.core :refer [dispatch reg-event-fx reg-fx]]
            [wh.common.search :as search]
            [wh.db :as db]
            [wh.pages.core :refer [on-page-browser-nav on-page-load] :as pages]))

(def client (algoliasearch "MVK698T35T" "bb5e4b1d90411ced5d34fc33c3a6e747"))

(reg-event-fx
  ::go-to-tab
  (fn [{_db :db} element]
    {:scroll-into-view element}))

(reg-event-fx
  ::fetch-data
  (fn [{_db :db} [_ query prefix]]
    {:search [query prefix]}))

(reg-event-fx
  ::fetch-data-success
  db/default-interceptors
  (fn [{db :db} [data]]
    {:db       (assoc-in db [:wh.search/data] data)
     :dispatch [::pages/unset-loader]}))

(reg-event-fx
  ::fetch-data-failure
  db/default-interceptors
  (fn [{db :db} _]
    {:db       (assoc-in db [:wh.search/data] {})
     :dispatch [::pages/unset-loader]}))

(def jobs-index "jobs")
(def companies-index "companies")
(def articles-index "articles")
(def issues-index "issues")

(defn index->query [prefix]
  (let [jobs      (str prefix jobs-index)
        companies (str prefix companies-index)
        articles  (str prefix articles-index)
        issues    (str prefix issues-index)]
    {jobs
     (fn [value]
       {:indexName jobs
        :query     value
        :filters   "published:true"
        :params    {:hitsPerPage 10}})

     companies
     (fn [value]
       {:indexName companies
        :query     value
        :filters   "'profile-enabled':true"
        :params    {:hitsPerPage 10}})

     articles
     (fn [value]
       {:indexName articles
        :query     value
        :filters   "published:true"
        :params    {:hitsPerPage 10}})

     issues
     (fn [value]
       {:indexName issues
        :query     value
        :filters   "status:open"
        :params    {:hitsPerPage 10}})}))

(defn make-query-for-index [prefix index value]
  (((index->query prefix) index) value))

(defn search-indices [prefix]
  {(str prefix jobs-index)      :jobs
   (str prefix articles-index)  :articles
   (str prefix issues-index)    :issues
   (str prefix companies-index) :companies})

(defn fetch-search-results [query prefix]
  (->> (search-indices prefix)
       keys
       (map #(make-query-for-index prefix % query))
       clj->js
       (.multipleQueries client)))

(defn assoc-section-name [prefix {:keys [index] :as datum}]
  [((search-indices prefix) index) datum])


(defn- handle-success [prefix search-results]
  (let [{:keys [results]} (walk/keywordize-keys (js->clj search-results))]
    (dispatch [::fetch-data-success
               (into {} (map (partial assoc-section-name prefix) results))])))

(defn- handle-failure []
  (dispatch [::fetch-data-failure]))

(reg-fx
  :search
  (fn [[value prefix]]
    (-> (fetch-search-results value prefix)
        (.then (partial handle-success prefix))
        (.catch handle-failure))))

(defn- search-related-events [db scroll-to-top?]
  (let [query  (search/->search-term (:wh.db/page-params db) (:wh.db/query-params db))
        prefix (get db :wh.settings/algolia-index-prefix)]
    [(when scroll-to-top? [:wh.events/scroll-to-top])
     [::fetch-data query prefix]
     [:wh.components.navbar.events/set-search-value query]]))

;; NB: This event is necessary to keep the `wh.components.navbar.navbar`
;;     search box value in sync w/ the URI-specified search term, if any,
;;     upon the browser navigation inside the universal search page.
(reg-event-fx
  ::trigger-search
  db/default-interceptors
  (fn [{db :db} [scroll-to-top?]]
    {:dispatch-n (search-related-events db scroll-to-top?)}))

(defmethod on-page-browser-nav :search [_db]
  [::trigger-search false])

(defmethod on-page-load :search [db]
  (search-related-events db true))
