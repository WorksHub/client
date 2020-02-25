(ns wh.blogs.learn.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.blogs.learn.db :as learn]
    [wh.db :as db]
    [wh.graphql-cache :refer [reg-query]]
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages]))
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defquery blogs-query
          {:venia/operation {:operation/type :query
                             :operation/name "blogs"}
           :venia/variables [{:variable/name "page_number"
                              :variable/type :Int}
                             {:variable/name "page_size"
                              :variable/type :Int}
                             {:variable/name "tag"
                              :variable/type :String}
                             {:variable/name "vertical_blogs"
                              :variable/type :vertical}]
           :venia/queries   [[:blogs {:tag             :$tag
                                      :page_size       :$page_size
                                      :page_number     :$page_number
                                      :vertical        :$vertical_blogs}
                              [[:pagination [:total]]
                               [:blogs [:id :title :feature :author
                                        :formattedCreationDate :readingTime
                                        :creator :upvoteCount :published
                                        [:tags :fragment/tagFields]]]]]]})
(reg-query :blogs blogs-query)
(def std-blogs-path [:blogs :blogs])

(defquery search-blogs-query
          {:venia/operation {:operation/type :query
                             :operation/name "search_blogs"}
           :venia/variables [{:variable/name "search_term"
                              :variable/type :String}
                             {:variable/name "page_size"
                              :variable/type :Int}
                             {:variable/name "page_number"
                              :variable/type :Int}]
           :venia/queries   [[:search_blogs
                              {:page_size :$page_size
                               :page_number :$page_number
                               :search_term :$search_term}
                              [[:results [:id :title :feature :author
                                          :formatted_creation_date :reading_time
                                          :creator :upvote_count :published
                                          [:tags :fragment/tagFields]]]
                               [:pagination [:total :count]]]]]})
(reg-query :search_blogs search-blogs-query)
(def search-blogs-path [:search-blogs :results])

(defquery recommended-jobs-query
          {:venia/operation {:operation/type :query
                             :operation/name "recommended_jobs"}
           :venia/variables [{:variable/name "vertical"
                              :variable/type :vertical}
                             {:variable/name "promoted_amount"
                              :variable/type :Int}]
           :venia/queries   [[:jobs_search {:vertical        :$vertical
                                            :promoted_amount :$promoted_amount}
                              [[:promoted [:fragment/jobCardFields]]]]]})
(reg-query :recommended_jobs recommended-jobs-query)

(defquery recommended-issues-query
          {:venia/operation {:operation/type :query
                             :operation/name "recommended_issues"}
           :venia/variables [{:variable/name "issues_amount"
                              :variable/type :Int}]
           :venia/queries   [[:query_issues
                              {:page_size   :$issues_amount}
                              [[:issues [:fragment/issueListFields]]]]]})
(reg-query :recommended_issues recommended-issues-query)

(defn std-blogs [db]
  [:blogs (learn/params db)])
(defn search-blogs [db]
  [:search_blogs (learn/params db)])
(defn jobs [db]
  [:recommended_jobs (learn/params db)])
(defn issues [db]
  [:recommended_issues (learn/params db)])

(reg-event-fx
  ::load-blogs
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch-n    (->> [(if (learn/search-term db) search-blogs std-blogs)
                          jobs
                          issues]
                         (map #(% db))
                         (mapv #(into [:graphql/query] %)))}))

(reg-event-fx
  ::set-learn-by-tag-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name (str "Articles: " (:tag (learn/params db)))
                  :vertical (:wh.db/vertical db)}}))

#?(:cljs
   (defmethod on-page-load :learn [db]
     [[:wh.pages.core/unset-loader]
      [::load-blogs]]))

#?(:cljs
   (defmethod on-page-load :learn-by-tag [db]
     [[:wh.pages.core/unset-loader]
      [::load-blogs]
      [::set-learn-by-tag-title]]))
