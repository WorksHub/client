(ns wh.search.events
  (:require ["algoliasearch" :as algoliasearch]
            [clojure.walk :as walk]
            [re-frame.core :refer [reg-event-db reg-event-fx dispatch reg-fx]]
            [wh.db :as db]))

(def client (algoliasearch "MVK698T35T" "11cd904aa52288a239214960e2433f97"))

(reg-event-fx
  ::go-to-tab
  (fn [{_db :db} element]
    {:scroll-into-view element}))

(reg-event-fx
  ::fetch-data
  (fn [{db :db} _]
    (let [query (get-in db [:wh.db/query-params "query"])]
      {:search query})))

(reg-event-fx
  ::fetch-data-success
  db/default-interceptors
  (fn [{db :db} [data]]
    {:db       (assoc-in db [:wh.search/data] data)
     :dispatch [:wh.pages.core/unset-loader]}))

(reg-event-fx
  ::fetch-data-failure
  db/default-interceptors
  (fn [{db :db} _]
    {:db       (assoc-in db [:wh.search/data] {})
     :dispatch [:wh.pages.core/unset-loader]}))


;; TODO: use env variables through shadow-cljs to set proper
;; index-prefix, based on environment [ch4693]
(def index-prefix "")

(def jobs-index (str index-prefix "jobs"))
(def companies-index (str index-prefix "companies"))
(def articles-index (str index-prefix "articles"))
(def issues-index (str index-prefix "issues"))

(def index->query
  {jobs-index
   (fn [value]
     {:indexName jobs-index
      :query     value
      :filters   "published:true"
      :params    {:hitsPerPage 10}})

   companies-index
   (fn [value]
     {:indexName companies-index
      :query     value
      :filters   "'profile-enabled':true"
      :params    {:hitsPerPage 10}})

   articles-index
   (fn [value]
     {:indexName articles-index
      :query     value
      :filters   "published:true"
      :params    {:hitsPerPage 10}})

   issues-index
   (fn [value]
     {:indexName issues-index
      :query     value
      :filters   "status:open"
      :params    {:hitsPerPage 10}})})

(defn make-query-for-index [index value]
  ((index->query index) value))

(def search-indices
  {jobs-index      :jobs
   articles-index  :articles
   issues-index    :issues
   companies-index :companies})

(defn fetch-search-results [query]
  (->> search-indices
       keys
       (map #(make-query-for-index % query))
       clj->js
       (.multipleQueries client)))

(defn assoc-section-name [{:keys [index] :as datum}]
  [(search-indices index) datum])


(defn- handle-success [search-results]
  (let [{:keys [results]} (walk/keywordize-keys (js->clj search-results))]
    (dispatch [::fetch-data-success
               (into {} (map assoc-section-name results))])))

(defn- handle-failure []
  (dispatch [::fetch-data-failure]))

(reg-fx
  :search
  (fn [value]
    (-> (fetch-search-results value)
        (.then handle-success)
        (.catch handle-failure))))
