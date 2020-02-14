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
                             {:variable/name "tag"
                              :variable/type :String}
                             {:variable/name "vertical"
                              :variable/type :vertical}
                             {:variable/name "vertical_blogs"
                              :variable/type :vertical}
                             {:variable/name "promoted_amount"
                              :variable/type :Int}
                             {:variable/name "issues_amount"
                              :variable/type :Int}]
           :venia/queries   [[:blogs {:tag             :$tag
                                      :page_size       24
                                      :page_number     :$page_number
                                      :vertical        :$vertical_blogs}
                              [[:pagination [:total]]
                               [:blogs [:id :title :feature :author
                                        :formattedCreationDate :readingTime
                                        :creator :upvoteCount :published
                                        [:tags :fragment/tagFields]]]]]
                             [:jobs_search {:vertical        :$vertical
                                            :promoted_amount :$promoted_amount}
                              [[:promoted [:fragment/jobCardFields]]]]
                             [:query_issues
                              {:page_size   :$issues_amount}
                              [[:issues [:fragment/issueListFields]]]]]})


(reg-query :blogs blogs-query)

(defn initial-query [db]
  [:blogs (learn/params db)])

(reg-event-fx
  ::load-blogs
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch      (into [:graphql/query] (initial-query db))}))

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
