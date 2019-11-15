(ns wh.company.listing.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load]])
    #?(:cljs [reagent.core :as r])
    [clojure.string :as str]
    [wh.common.text :as text]
    [wh.company.listing.db :as listing]
    [wh.company.listing.subs :as subs]
    [wh.components.pagination :as pagination]
    [wh.db :as db]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.company] ;; included for fragments
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery fetch-companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "search_companies"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}
                     {:variable/name "tag_string" :variable/type :String}
                     {:variable/name "sort" :variable/type :companies_sort}
                     {:variable/name "live_jobs" :variable/type :live_job_query_type}
                     {:variable/name "size" :variable/type :company_size}]
   :venia/queries   [[:search_companies
                      {:page_number :$page_number
                       :page_size   :$page_size
                       :tag_string  :$tag_string
                       :sort        :$sort
                       :live_jobs   :$live_jobs
                       :size        :$size}
                      [[:pagination [:total :count]]
                       [:companies :fragment/companyCardFields]]]]})

(defquery fetch-company-filter-tags-list
  {:venia/operation {:operation/type :query
                     :operation/name "company_filter_tags_list"}
   :venia/queries   [[:company_filter_tags_list
                      [[:tags :fragment/tagFields]]]]})

(reg-query :search_companies fetch-companies-query)
(reg-query :company-filter-tags-list fetch-company-filter-tags-list)

(defn initial-query [db]
  (let [qps (:wh.db/query-params db)]
    ;; TODO search term from URL?
    [:search_companies (listing/qps->query-body qps)]))

(defn company-filter-tags-list-query []
  [:company-filter-tags-list {}])

(reg-event-fx
  ::on-tag-change
  db/default-interceptors
  (fn [{db :db} [qps-key tag-query-value tag-element]]
    (when tag-element
      (let [add? (.contains (.-classList tag-element) "tag--selected")
            size? (str/ends-with? tag-query-value ":size")
            qps (dissoc (:wh.db/query-params db) "page")
            qps (cond (and (not size?)
                           (not add?)
                           (or (= 1 (count (get qps qps-key)))
                               (not (coll? (get qps qps-key)))))
                      (dissoc qps qps-key)
                      (and size? add?)
                      (assoc qps "size" (first (str/split tag-query-value #":")))
                      (and size? (not add?))
                      (dissoc qps "size")
                      (and (not add?))
                      (update qps qps-key #(disj (set (util/->vec %)) tag-query-value))
                      add?
                      (update qps qps-key #(if %
                                             (conj (set (util/->vec %)) tag-query-value)
                                             tag-query-value)))]
        #?(:cljs
           (let [tagbox (js/document.getElementById listing/tag-field-id)]
             (when size?
               (let [size-value (first (str/split tag-query-value #":"))]
                 (run! (fn [e]
                         (when (and e (not (str/includes? (.toString e.classList) (str "tag--slug-" size-value))))
                           (.remove e.classList "tag--selected")))
                       (array-seq (.getElementsByClassName tagbox "tag--type-size tag--selected")))))
             (js/resetTagsElementVisibility tagbox)))
        {:dispatch [:wh.events/nav--query-params qps]}))))

(defn tags
  [db]
  (->> (cache/result db :company-filter-tags-list {})
       :company-filter-tags-list
       :tags))

(defn load-tags-into-js
  [company-tags]
  #?(:cljs
     (do
       (set! js/whTags (clj->js company-tags))
       (js/initTags (js/document.getElementById listing/tag-field-id))))
  nil)

(reg-event-fx
  ::init-tags
  db/default-interceptors
  (fn [{db :db} _]
    (let [company-tags (not-empty (tags db))]
      #?(:cljs
         (cond (and company-tags js/whTags)
               nil
               (and company-tags (not js/whTags))
               (load-tags-into-js company-tags)
               (and (not company-tags) js/whTags)
               {:dispatch [:graphql/update-entry :company-filter-tags-list {} :overwrite
                           {:company-filter-tags-list
                            {:tags
                             (mapv js->clj js/whTags)}}]}
               (and (not company-tags) (not js/whTags))
               ;;:else
               {:dispatch (into [:graphql/query] (conj (company-filter-tags-list-query)
                                                       {:on-success [::init-tags]}))})))))

#?(:cljs
   (defmethod on-page-load :companies [db]
     (list (into [:graphql/query] (initial-query db))
           [::init-tags]
           (when (> (:wh.db/page-moves db) 1)
             [:wh.events/scroll-to-top]))))
