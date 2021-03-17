(ns wh.company.listing.events
  (:require #?(:cljs [wh.pages.core :refer [on-page-load]])
            [clojure.string :as str]
            [wh.company.listing.db :as listing]
            [wh.db :as db]
            [wh.graphql-cache :as cache :refer [reg-query]]
            [wh.graphql.company] ;; included for fragments
            [wh.re-frame.events :refer [reg-event-fx]]
            [wh.util :as util])
  (#?(:clj :require :cljs :require-macros) [wh.graphql-macros :refer [defquery]]))

(defquery fetch-companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "search_companies"}
   :venia/variables [{:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}
                     {:variable/name "tag_string" :variable/type :String}
                     {:variable/name "search_term" :variable/type :String}
                     {:variable/name "sort" :variable/type :companies_sort}
                     {:variable/name "live_jobs" :variable/type :live_job_query_type}
                     {:variable/name "size" :variable/type :company_size}]
   :venia/queries   [[:search_companies
                      {:page_number :$page_number
                       :page_size   :$page_size
                       :tag_string  :$tag_string
                       :sort        :$sort
                       :live_jobs   :$live_jobs
                       :search_term :$search_term
                       :size        :$size}
                      [[:pagination [:total :count]]
                       [:companies :fragment/companyCardFields]]]]})

(reg-query :search_companies fetch-companies-query)

(defn initial-query [db]
  (let [qps (:wh.db/query-params db)]
    ;; TODO search term from URL?
    [:search_companies (listing/qps->query-body qps)]))

(reg-event-fx
  ::on-tag-change
  db/default-interceptors
  (fn [{db :db} [{qps-key :query-param tag-query-value :tag-query-id tag-element :tag}]]
    (when tag-element
      (let [add?  (.contains (.-classList tag-element) "tag--selected")
            size? (str/ends-with? tag-query-value ":size")
            qps   (dissoc (:wh.db/query-params db) "page")
            qps   (cond (and (not size?)
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


#?(:cljs
   (defmethod on-page-load :companies [db]
     (list (into [:graphql/query] (initial-query db))
           (when (> (:wh.db/page-moves db) 1)
             [:wh.events/scroll-to-top]))))
