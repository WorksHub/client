(ns wh.profile.events
  (:require
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.db :as db]
    [wh.graphql-cache :refer [reg-query] :as gql-cache]
    #?(:cljs [wh.pages.core :as pages]))
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(defquery profile-query
  {:venia/operation {:operation/type :query
                     :operation/name "fetch_user"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:user {:id :$id} [[:skills [:name :rating]]
                                      [:otherUrls [:url]]
                                      :imageUrl
                                      :name
                                      :id
                                      :summary
                                      :percentile
                                      :created]]
                   [:blogs {:user_id :$id} [[:blogs [:id :title :formattedCreationDate :readingTime
                                                     :upvoteCount :published]]]]
                   [:query_issues {:user_id :$id}
                    [[:issues [:id :title :level
                               [:compensation [:amount :currency]]
                               [:company [:id :name :logo :slug]]
                               [:repo [:primary_language]]]]]]]})

(reg-query :profile profile-query)

(defn profile-query-description [db]
  [:profile {:id (get-in db [:wh.db/page-params :id])}])

(reg-event-fx
  ::set-page-title
  db/default-interceptors
  (fn [{db :db} _]
    {:page-title {:page-name (some-> (apply gql-cache/result db (profile-query-description db)) :user :name)
                  :vertical  (:wh.db/vertical db)}}))

(reg-event-fx
  ::load-profile
  db/default-interceptors
  (fn [{db :db} _]
    {:scroll-to-top true
     :dispatch-n    [(into [:graphql/query]
                           (conj (profile-query-description db)
                                 {:on-complete [::set-page-title]}))]}))

#?(:cljs
   (defmethod pages/on-page-load :user [_]
     ;; TODO: ask why it's impossible to user ::pages/unset-loader
     [[:wh.pages.core/unset-loader]
      [::load-profile]]))
