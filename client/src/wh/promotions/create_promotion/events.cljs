(ns wh.promotions.create-promotion.events
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [wh.blogs.blog.events]
            [wh.db :as db]
            [wh.graphql-cache :refer [reg-query]]
            [wh.graphql.company]
            [wh.graphql.issues]
            [wh.graphql.jobs]
            [wh.pages.core :refer [on-page-load]]
            [wh.promotions.create-promotion.db :as create-promotion])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

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
    [(into [:graphql/query] (query-fn db))
     [::init-db]]))

(reg-event-db
  ::edit-description
  db/default-interceptors
  (fn [db [description]]
    (assoc db ::create-promotion/description description)))

(reg-event-db
  ::init-db
  db/default-interceptors
  (fn [db _]
    (-> db
        (assoc ::create-promotion/promotion-status {})
        (assoc ::create-promotion/description ""))))


(defquery create-promotion-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_promotion"}
   :venia/variables [{:variable/name "object_type"
                      :variable/type :object_type!}
                     {:variable/name "object_id"
                      :variable/type :String!}
                     {:variable/name "channel"
                      :variable/type :channel!}
                     {:variable/name "start_date"
                      :variable/type :date!}
                     {:variable/name "description"
                      :variable/type :String}]
   :venia/queries   [[:create_promotion {:object_type :$object_type
                                         :object_id   :$object_id
                                         :channel     :$channel
                                         :start_date  :$start_date
                                         :description :$description}
                      [:id]]]})

(reg-event-fx
  ::send-promotion!
  db/default-interceptors
  (fn [{db :db} [{:keys [channel object-type object-id start-date description] :as args}]]
    {:db      (assoc-in db [::create-promotion/promotion-status channel] :sending)
     :graphql {:query      create-promotion-mutation
               :variables  (cond->
                             {:object_type object-type
                              :object_id   object-id
                              :channel     channel
                              :start_date  (tf/unparse (tf/formatters :date-time) (t/now))}
                             description (merge {:description description}))
               :on-success [::send-promotion-success channel]
               :on-failure [::send-promotion-failure channel]}}))

(reg-event-db
  ::send-promotion-success
  db/default-interceptors
  (fn [db [channel]]
    (assoc-in db [::create-promotion/promotion-status channel] :success)))

(reg-event-db
  ::send-promotion-failure
  db/default-interceptors
  (fn [db [channel]]
    (assoc-in db [::create-promotion/promotion-status channel] :failure)))

(reg-event-db
  ::select-channel
  db/default-interceptors
  (fn [db [channel]]
    (assoc db ::create-promotion/selected-channel channel)))
