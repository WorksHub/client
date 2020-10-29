(ns wh.admin.candidates.events
  (:require
    [cljs-time.coerce :as tc]
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [clojure.string :as str]
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.admin.candidates.db :as candidates]
    [wh.admin.queries :as admin-queries]
    [wh.common.cases :as cases]
    [wh.db :as db]
    [wh.pages.core :refer [on-page-load]]
    [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def candidates-interceptors (into db/default-interceptors
                                   [(path ::candidates/sub-db)]))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (assoc db ::candidates/sub-db (candidates/default-db db))))

(reg-event-fx
  ::set-search-term
  candidates-interceptors
  (fn [{db :db} [search-term]]
    {:db       (assoc db ::candidates/search-term search-term) ;TODO reset page to 0
     :dispatch [::navigate-candidates]}))

(reg-event-fx
  ::search-bad-response
  candidates-interceptors
  (fn [{db :db} [retry-attempt result]]
    (if (and retry-attempt (> retry-attempt 2))
      (do
        (js/console.error "Search failed:" result)
        {:db (assoc db ::candidates/loading-state :error)})
      (let [attempt (if-not retry-attempt 1 (inc retry-attempt))]
        {:dispatch [::search-candidates attempt]}))))

(defn generate-filters
  "Example: '(productType:book OR productType:dvd) AND genre:\"sci-fi\" AND price < 10'"
  [db]
  (let
    [vertical-str (->> (::candidates/verticals db)
                       (map #(str "board:" %))
                       (str/join " OR "))
     approvals (->> (::candidates/approval-statuses db)
                    (map #(str "approval.status:" %))
                    (str/join " OR "))]
    (<< "(~{vertical-str}) AND (~{approvals})")))

(reg-event-fx
  ::navigate-candidates
  candidates-interceptors
  (fn [{db :db} []]
    {:navigate [:candidates :query-params (candidates/candidates-query-params db)]
     :dispatch [:error/close-global]}))

(reg-event-fx
  ::toggle-vertical
  candidates-interceptors
  (fn [{db :db} [new-value]]
    {:db       (update db ::candidates/verticals util/toggle-unless-empty new-value)
     :dispatch [::navigate-candidates]}))

(reg-event-fx
  ::toggle-approval-statuses
  candidates-interceptors
  (fn [{db :db} [new-value]]
    {:db       (update db ::candidates/approval-statuses util/toggle-unless-empty new-value)
     :dispatch [::navigate-candidates]}))

(reg-event-fx
  ::search-candidates
  db/default-interceptors
  (fn [{db :db} [retry-num]]
    (let [candidate-db (::candidates/sub-db db)]
      {:algolia {:index      :candidates
                 :params     {:query       (::candidates/search-term candidate-db)
                              :filters     (generate-filters candidate-db)
                              :page        (dec (::candidates/page candidate-db))
                              :hitsPerPage 50
                              :facets      "[\"approval.status\",\"board\"]"}
                 :on-success [::search-candidates-success]
                 :on-failure [::search-bad-response retry-num]}
       :db      (assoc-in db [::candidates/sub-db ::candidates/loading-state] :loading)})))

(defn format-candidate-updated [date-in-long]
  "Formats updated date as following:
  For today: Today at 09:42
  For all other dates: 17. 03. 15 at 10:15"
  (let [dt (tc/from-long date-in-long)]
    (cond
      (t/after? dt (t/today)) (str "Today at " (tf/unparse (tf/formatters :hour-minute) dt))
      :else (tf/unparse (tf/formatter "dd.MM.yy hh:mm") dt))))

(defn map-candidate [candidate]
  (-> candidate
      cases/->kebab-case
      (update :updated format-candidate-updated)))

(reg-event-db
  ::search-candidates-success
  candidates-interceptors
  (fn [db [{:keys [hits facets page nbPages]}]]
    (assoc db ::candidates/search-results (mapv map-candidate hits)
              ::candidates/facets-counts facets
              ::candidates/results-counts {:current-page (inc page) :total-pages nbPages} ;; TODO uniform with pagination
              ::candidates/loading-state (if (seq hits) :success :no-results))))

(def delete-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "delete_user"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:delete_user {:id :$id}]]})

(defn toggle-updating? [candidates id updating]
  (map (fn [{:keys [object-id] :as candidate}]
         (if (= id object-id)
           (assoc candidate :updating updating)
           candidate)) candidates))

(reg-event-fx
  :candidates/set-approval-status
  candidates-interceptors
  (fn [{db :db} [id email status close-error]]
    {:db      (update db ::candidates/search-results toggle-updating? id status)
     :graphql {:query      admin-queries/set-approval-status-mutation
               :variables  {:id id :status status}
               :timeout    30000
               :on-success [::update-user-status-success]
               :on-failure [:error/set-global (str "Failed to set status to " status " for " email)
                            [:candidates/set-approval-status id email status]]}}))

(reg-event-fx
  :candidates/delete-user
  candidates-interceptors
  (fn [{db :db} [id email]]
    {:db      (update db ::candidates/search-results toggle-updating? id "delete")
     :graphql {:query      delete-user-mutation
               :variables  {:id id}
               :timeout    30000
               :on-success [::update-user-status-success]
               :on-failure [:error/set-global
                            (str "Failed to delete " email)
                            [:candidates/delete-user id email]]}}))

(reg-event-fx
  ::update-user-status-success
  candidates-interceptors
  (fn [{db :db} []]
    {:dispatch-n [[:error/close-global] [::search-candidates]]}))

(defmethod on-page-load :candidates [db]
  [[::initialize-db]
   [::search-candidates]])
