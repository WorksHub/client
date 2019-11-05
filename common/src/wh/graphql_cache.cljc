(ns wh.graphql-cache
  (:require
    #?(:clj [com.walmartlabs.lacinia.util :as lac-util])
    #?(:clj [taoensso.timbre :refer [errorf]])
    #?(:clj [wh.graphql :as graphql])
    [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
    [re-frame.registrar :refer [get-handler register-handler]]
    [wh.common.cases :as cases]
    [wh.db :as db]
    [wh.util :as util]))

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
        (>= (- (util/now) timestamp)
            cache-validity-ms))))

;; options:
;; - :on-success  - event to be fired when graphql query succeeds
;; - :on-complete - event to be fired when results are available (either new or cached)
(reg-event-fx
  :graphql/query
  db/default-interceptors
  (fn [{db :db} [query-id variables options]]
    (let [options (or options {})]
      (if-not (do-query? db query-id variables options)
        (if (:on-complete options)
          {:dispatch (:on-complete options)}
          {})
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

(defn store-result
  "Store the query's successful result in app-db."
  [db query-id variables {:keys [data errors]}]
  (let [state (if errors :failure :success)]
    (update-in db [::cache [query-id variables]] merge
               {:state     (if errors :failure :success)
                :result    (when (= state :success)
                             (cases/->kebab-case data))
                :errors    (when (= state :failure)
                             (cases/->kebab-case errors))}
               (when (= state :success)
                 {:timestamp (util/now)}))))

(reg-event-db
  :graphql/update-entry
  db/default-interceptors
  (fn [db [query-id variables method data]]
    (case method
      :merge     (-> db
                     (update-in [::cache [query-id variables] :result] #(merge-with merge % (cases/->kebab-case data)))
                     (assoc-in  [::cache [query-id variables] :state] :success))
      :overwrite (-> db
                     (assoc-in [::cache [query-id variables] :result] (cases/->kebab-case data))
                     (assoc-in [::cache [query-id variables] :state] :success)))))

(reg-event-fx
  ::success
  db/default-interceptors
  (fn [{db :db} [query-id variables {:keys [on-success on-complete]} response]]
    (let [follow-up-events (cond-> []
                                   on-success  (conj on-success)
                                   on-complete (conj on-complete)
                                   :always     not-empty)]
      (cond-> {:db (store-result db query-id variables response)}
              follow-up-events (assoc :dispatch-n follow-up-events)))))

(reg-event-db
  ::failure
  db/default-interceptors
  (fn [db [query-id variables options response]]
    (store-result db query-id variables response)))

(defn pre-execute [db query-id variables ctx]
  #?(:clj
     (let [query (get-handler :graphql-query query-id true)
           result (try
                    (graphql/execute-query query variables ctx)
                    (catch Exception e
                      (errorf "An exception was thrown whilst pre-executing %s (%s) - %s %s"
                              query variables (graphql/error-map e query variables) (.getMessage e))
                      {:errors [(graphql/error-map e query variables)]}))]
       (when (:errors result)
         (errorf "An error occurred whilst pre-executing %s (%s) - %s" query variables (pr-str (:errors result))))
       (store-result db query-id variables result))
     :cljs
     (throw (js/Error. "pre-execute is not supported in ClojureScript."))))

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
    (let [first-error (first (errors db query-id variables))]
      (or (-> first-error :key keyword)
          (-> first-error :message keyword)))))
