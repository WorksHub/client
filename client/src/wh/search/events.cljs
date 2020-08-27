(ns wh.search.events
  (:require ["algoliasearch" :as algoliasearch]
            [clojure.walk :as walk]
            [re-frame.core :refer [reg-event-db reg-event-fx dispatch reg-fx]]
            [wh.db :as db]))

;; TODO: make sure to use proper access keys [4682]
(def client (algoliasearch "MVK698T35T" "3b2c040e2342a3993ff04ad787fa6442"))

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


(defn make-query-for-index [index value]
  {:indexName index
   :query     value
   ;; TODO: turn on "published" filter [4682]
   ;; it's turned off for development purposes
   ;; :filters   "published:true"
   :params    {:hitsPerPage 10}})

;; TODO: use proper indices, instead of dev, before going to prod [4682]
(def search-indices
  {"dev_jobs"      :jobs
   "dev_articles"  :articles
   "dev_issues"    :issues
   "dev_companies" :companies})

(defn fetch-search-results [query]
  (->> search-indices
       keys
       (map #(make-query-for-index % query))
       clj->js
       (.multipleQueries client)))

(defn assoc-section-name [{:keys [index] :as datum}]
  [(search-indices index) datum])

(reg-fx
  :search
  (fn [value]
    (-> (fetch-search-results value)
        (.then
          (fn [search-results]
            (let [{:keys [results]} (walk/keywordize-keys (js->clj search-results))]
              (dispatch [::fetch-data-success
                         (into {} (map assoc-section-name results))]))))
        (.catch (fn []
                  (dispatch [::fetch-data-failure]))))))
