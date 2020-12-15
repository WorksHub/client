(ns wh.promotions.create-promotion.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [wh.blogs.blog.events]
            [wh.graphql-cache :refer [reg-query]]
            [wh.graphql.company]
            [wh.graphql.issues]
            [wh.graphql.jobs]
            [wh.pages.core :refer [on-page-load]]
            [wh.promotions.create-promotion.db :as db]))

(defn job-preview [{:keys [wh.db/page-params] :as db}]
  ;; query registerd in wh.graphql.jobs
  [:job {:id (:id page-params)}])

(defn company-preview [{:keys [wh.db/page-params] :as db}]
  ;; query registerd in wh.graphql.company
  [:company {:id (:id page-params)}])

(defn issue-preview [{:keys [wh.db/page-params] :as db}]
  ;; query registerd in wh.graphql.issues
  [:issue {:id (:id page-params)}])

(defn blog-preview [{:keys [wh.db/page-params] :as db}]
  ;; query registerd in wh.blogs.blog.events
  [:blog {:id (:id page-params)}])

(defn unkown-query []
  (js/console.error "Unkown object type given!")
  [:unkown-query-error])

(defn preview-query [type]
  (case type
    :article blog-preview
    :issue   issue-preview
    :company company-preview
    :job     job-preview
    unkown-query))

(defmethod on-page-load :create-promotion [{:keys [wh.db/page-params] :as db}]
  (let [type     (keyword (:type page-params))
        query-fn (preview-query type)]
    [(into [:graphql/query] (query-fn db))]))

(reg-event-db
  ::edit-description
  db/default-interceptors
  (fn [db [description]]
    (assoc db ::db/description description)))

(reg-event-fx
  ::send-promotion!
  db/default-interceptors
  (fn [{db :db} _]
    {:db db}))
