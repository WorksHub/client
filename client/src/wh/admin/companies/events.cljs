(ns wh.admin.companies.events
  (:require [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.admin.companies.db :as companies]
            [wh.common.cases :as cases]
            [wh.common.data :refer [get-manager-email]]
            [wh.db :as db]
            [wh.pages.core :as pages :refer [on-page-load]]
            [wh.util :as util]))

(def companies-interceptors (into db/default-interceptors
                                  [(path ::companies/sub-db)]))

(def companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "companies"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "search_term" :variable/type :String}
                     {:variable/name "sort"        :variable/type :companies_sort}
                     {:variable/name "managers"    :variable/type {:type/kind :list :type.list/items {:type/type :String}}}
                     {:variable/name "packages"    :variable/type {:type/kind :list :type.list/items {:type/type :package}}}
                     {:variable/name "verticals"   :variable/type {:type/kind :list :type.list/items {:type/type :vertical}}}
                     {:variable/name "no_manager"  :variable/type :Boolean}
                     {:variable/name "has_users"   :variable/type :Boolean}
                     {:variable/name "live_jobs"   :variable/type :live_job_query_type}]
   :venia/queries [[:companies
                    {:page_number :$page_number
                     :page_size   20
                     :search_term :$search_term
                     :sort        :$sort
                     :managers    :$managers
                     :packages    :$packages
                     :verticals   :$verticals
                     :no_manager  :$no_manager
                     :has_users   :$has_users
                     :live_jobs   :$live_jobs}
                    [[:pagination [:total :count]]
                       [:companies [:id :name :logo :manager :vertical :package
                                    [:applicationStateFrequencies [:state :count]]
                                    [:jobStats [:applications :views :likes]]]]]]]})

(defn companies-query-variables
  [pn tags search sort]
  (let [tags               (map (fn [tag] (some #(when (= (:tag %) tag) %) companies/tags)) tags)
        no-manager?        (some #(when (and (= :manager (:type %))
                                             (= "no manager" (:tag %))) true) tags)
        has-users?         (some #(when (= :has-users (:type %)) true) tags)
        with-live-jobs?    (some #(when (and (= :live-jobs (:type %))
                                             (= "with live jobs" (:tag %))) true) tags)
        without-live-jobs? (some #(when (and (not with-live-jobs?)
                                             (= :live-jobs (:type %))
                                             (= "without live jobs" (:tag %))) true) tags)
        managers           (not-empty (filter #(and (= :manager (:type %)) (not= "no manager" (:tag %))) tags))
        packages           (not-empty (filter #(= :package  (:type %)) tags))
        verticals          (not-empty (filter #(= :vertical (:type %)) tags))]
    (util/remove-nils
     {:page_number pn
      :search_term (when-not (str/blank? search) search)
      :sort        (when-not (str/blank? sort)   sort)
      :managers    (when managers (map (comp get-manager-email :tag) managers))
      :packages    (when packages (map #(keyword (str/replace (:tag %) #" " "_")) packages))
      :verticals   (when verticals (map #(keyword (str/replace (:tag %) #" works" "")) verticals))
      :no_manager  (when no-manager? true)
      :has_users   (when has-users? true)
      :live_jobs   (cond with-live-jobs? :some
                         without-live-jobs? :none)})))

(reg-event-db
  ::toggle-tag-search-collapsed
  companies-interceptors
  (fn [db _]
    (update db ::companies/tag-search-collapsed? not)))

(reg-event-db
  ::set-tag-search
  companies-interceptors
  (fn [db [search-term]]
    (assoc db ::companies/tag-search search-term)))

(reg-event-fx
  ::toggle-tag
  db/default-interceptors
  (fn [{db :db} [tag]]
    (let [existing-search (get-in db [::companies/sub-db ::companies/search])
          new-query-params (merge (get db ::db/query-params)
                                  (if-let [search-tag (second (re-find #"^search: \"(.+)\"" tag ))]
                                    {"search" (when (not= existing-search search-tag) search-tag)}
                                    (let [updated-tags (-> db
                                                           (get-in [::companies/sub-db ::companies/tags])
                                                           (util/toggle tag))]
                                      {"tags" (when (not-empty updated-tags) (str/join "," updated-tags))})))]
      {:navigate [:admin-companies :query-params (util/remove-nils new-query-params)]})))

(reg-event-fx
  ::fetch-companies
  companies-interceptors
  (fn [{db :db} _]
    (let [page-number (::companies/page-number db)]
      {:graphql {:query      companies-query
                 :variables  (companies-query-variables page-number
                                                        (::companies/tags db)
                                                        (::companies/search db)
                                                        (::companies/sort db))
                 :on-success [::fetch-companies-success]
                 :on-failure [::fetch-companies-failure]}})))

(defn transform-company
  [company]
  (-> company
      (cases/->kebab-case)
      (update :package keyword)
      (update :application-state-frequencies
              #(reduce (fn [a m] (assoc a (keyword (:state m)) (:count m))) {} %))))

(reg-event-fx
  ::fetch-companies-success
  companies-interceptors
  (fn [{db :db} [{{{companies :companies pagination :pagination} :companies} :data}]]
    {:db (-> db
             (dissoc db ::companies/loading-more?)
             (assoc ::companies/result-total (:total pagination)
                    ::companies/show-load-more? (and (pos? (:count pagination))
                                                     (< (+ (count companies) (count (::companies/results db))) (:total pagination))))
             (update ::companies/results concat (map transform-company companies)))}))

(reg-event-fx
  ::fetch-companies-failure
  companies-interceptors
  (fn [{db :db} _]
    {:db (dissoc db ::companies/loading-more?)
     :dispatch [:error/set-global "Something went wrong while we tried to fetch your data 😢"]}))

(reg-event-fx
  ::load-more-companies
  companies-interceptors
  (fn [{db :db} _]
    {:db (-> db
             (update ::companies/page-number inc)
             (assoc ::companies/loading-more? true))
     :dispatch [::fetch-companies]}))

(reg-event-fx
  ::select-sort
  db/default-interceptors
  (fn [{db :db} [sort]]
    (let [sort-key (some #(when (= sort (second %)) (first %)) companies/sorts)]
      {:navigate [:admin-companies :query-params
                  (merge (get db ::db/query-params) {"sort" (name sort-key)})]})))

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    (let [new-db (companies/initialize-db db)]
      {:db (assoc db ::companies/sub-db new-db)})))

(defmethod on-page-load :admin-companies [db]
  [[::initialize-db]
   [::fetch-companies]])
