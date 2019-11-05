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
                     :operation/name "companies"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}
                     {:variable/name "search_term" :variable/type :String}
                     {:variable/name "sort" :variable/type :companies_sort}
                     {:variable/name "tag_string" :variable/type :String}]
   :venia/queries   [[:companies
                      {:page_number :$page_number
                       :page_size   :$page_size
                       :search_term :$search_term
                       :sort        :$sort
                       :tag_string  :$tag_string}
                      [[:pagination [:total :count]]
                       [:companies :fragment/companyCardFields]]]]})

(defquery fetch-company-filter-tags-list
  {:venia/operation {:operation/type :query
                     :operation/name "company_filter_tags_list"}
   :venia/queries   [[:company_filter_tags_list
                      [[:tags :fragment/tagFields]]]]})

(reg-query :companies fetch-companies-query)
(reg-query :company-filter-tags-list fetch-company-filter-tags-list)

(defn initial-query [db]
  (let [qps (:wh.db/query-params db)]
    ;; TODO search term from URL?
    [:companies (merge {:page_number (pagination/qps->page-number qps)
                        :page_size   listing/page-size
                        :sort        (listing/company-sort qps)}
                       (when-let [tag-or-tags (get qps "tag")]
                         {:tag_string (str/join "," (util/->vec tag-or-tags))}))]))

(defn company-filter-tags-list-query []
  [:company-filter-tags-list {}])

(reg-event-fx
  ::on-tag-change
  db/default-interceptors
  (fn [{db :db} [tag-query-value tag-element]]
    (when tag-element
      (let [add? (.contains (.-classList tag-element) "tag--selected")
            qps (:wh.db/query-params db)
            qps (cond (and (not add?) (or (= 1 (count (get qps "tag")))
                                          (not (coll? (get qps "tag")))))
                      (dissoc qps "tag")
                      (and (not add?))
                      (update qps "tag" #(disj (set (util/->vec %)) tag-query-value))
                      add?
                      (update qps "tag" #(if %
                                           (conj (set (util/->vec %)) tag-query-value)
                                           tag-query-value)))]
        #?(:cljs
           (js/resetTagsElementVisibility (js/document.getElementById listing/tag-field-id)))
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
           [::init-tags])))
