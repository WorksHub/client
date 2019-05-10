(ns wh.graphql
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
    [re-frame.registrar :refer [get-handler register-handler]]
    [wh.common.cases :as cases]
    [wh.db :as db]))

(defn reg-query
  [id query]
  (register-handler :graphql-query id query))

(defn state
  [db query-id variables]
  (get-in db [::cache [query-id variables] :state]))

(defn errors
  [db query-id variables]
  (get-in db [::cache [query-id variables] :errors]))

(defn result
  [db query-id variables]
  (get-in db [::cache [query-id variables] :result]))

(def cache-validity-ms (* 15 60 1000))

(defn do-query?
  [db query-id variables options]
  (let [{:keys [state timestamp]} (get-in db [::cache [query-id variables]])]
    (or (:force options)
        (not= state :success)
        (not (number? timestamp))
        (>= (- (js/Date.now) timestamp) cache-validity-ms))))

(reg-event-fx
  :graphql/query
  db/default-interceptors
  (fn [{db :db} [query-id variables options]]
    (let [options (or options {})]
      (if-not (do-query? db query-id variables options)
        {}
        {:db (update-in db [::cache [query-id variables]] merge {:state :executing})
         :graphql {:query (get-handler :graphql-query query-id true)
                   :variables variables
                   :on-success [::success query-id variables options]
                   :on-failure [::failure query-id variables options]}}))))

(reg-event-db
  :graphql/invalidate
  db/default-interceptors
  (fn [db [query-id variables]]
    (assoc-in db [::cache [query-id variables] :timestamp] nil)))

(defn invalidate-all-for-id
  [cache query-id]
  (into {}
        (for [[[k-query-id k-variables :as k] v :as entry] cache]
          (if (= query-id k-query-id)
            [k (assoc v :timestamp nil)]
            entry))))

(reg-event-db
  :graphql/invalidate-all-for-id
  db/default-interceptors
  (fn [db [query-id]]
    (update db ::cache invalidate-all-for-id query-id)))

(reg-event-fx
  ::success
  db/default-interceptors
  (fn [{db :db} [query-id variables {:keys [on-success]} {:keys [data]}]]
    (let [result (cases/->kebab-case data)]
      (cond->
          {:db (update-in db [::cache [query-id variables]] merge {:state :success, :result result, :errors nil, :timestamp (js/Date.now)})}
        on-success (assoc :dispatch on-success)))))

;; TODO: do we want to always assoc a nil result here?
(reg-event-db
  ::failure
  db/default-interceptors
  (fn [db [query-id variables options {:keys [errors]}]]
    (let [errors (cases/->kebab-case errors)]
      (update-in db [::cache [query-id variables]] merge {:state :failure, :result nil, :errors errors}))))

(reg-sub
  :graphql/state
  (fn [db [_ query-id variables]]
    (state db query-id variables)))

(reg-sub
  :graphql/executing?
  (fn [db [_ query-id variables]]
    (= (state db query-id variables) :executing)))

(reg-sub
  :graphql/success?
  (fn [db [_ query-id variables]]
    (= (state db query-id variables) :success)))

(reg-sub
  :graphql/result
  (fn [db [_ query-id variables]]
    (result db query-id variables)))

(reg-sub
  :graphql/errors
  (fn [db [_ query-id variables]]
    (errors db query-id variables)))

(reg-sub
  :graphql/error-key
  (fn [db [_ query-id variables]]
    (-> (errors db query-id variables)
        first
        :key
        keyword)))
