(ns wh.blogs.like
  (:require
    [re-frame.core :refer [reg-event-fx reg-sub]]
    [wh.db :as db]
    [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
   [wh.graphql-macros :refer [defquery]]))

(defquery like-blog-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "LikeBlog"}
   :venia/variables [{:variable/name "id"
                      :variable/type :String!}
                     {:variable/name "add"
                      :variable/type :Boolean!}]
   :venia/queries   [[:mark_entity {:id :$id, :add :$add, :action :like, :entity_type :blog}
                      [:marked]]]})

;;

(defn start-executing [db id]
  (assoc-in db [:like-blog-mutation id] true))

(defn stop-executing [db id]
  (assoc-in db [:like-blog-mutation id] false))

(defn executing? [db id]
  (true? (get-in db [:like-blog-mutation id])))

(defn toggle-like [db id]
  (update-in db [:wh.user.db/sub-db :wh.user.db/liked-blogs] util/toggle id))

;;

(reg-event-fx
  ::like-success
  db/default-interceptors
  (fn [{db :db} [{:keys [blog]}]]
    {:db (stop-executing db (:id blog))}))


(reg-event-fx
  ::like-failure
  db/default-interceptors
  (fn [{db :db} [{:keys [blog]}]]
    {:db       (-> db
                   (stop-executing (:id blog))
                   (toggle-like (:id blog)))
     :dispatch [:error/set-global "Failed to bookmark the article."]}))

;;

(defn like-blog
  [{:keys [db blog like?]}]
  (let [event-name (str "Blog " (if like? "liked" "unliked"))
        id         (:id blog)]
    {:db              (-> (start-executing db id)
                          (toggle-like (:id blog)))
     :graphql         {:query      like-blog-mutation
                       :variables  {:id id :add like?}
                       :on-success [::like-success {:blog blog}]
                       :on-failure [::like-failure {:blog blog}]}
     :analytics/track [event-name {:id (:id blog)}]}))

(reg-sub
  ::executing?
  (fn [db [_ blog]]
    (executing? db (:id blog))))

(reg-sub
  ::liked-blogs-by-user
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/liked-blogs])))

(reg-sub
  ::liked-by-user?
  :<- [::liked-blogs-by-user]
  (fn [liked-blogs [_ blog]]
    (contains? liked-blogs (:id blog))))

(reg-event-fx
  ::like
  db/default-interceptors
  (fn [{db :db} [blog]]
    (like-blog {:db    db
                :blog  blog
                :like? true})))

(reg-event-fx
  ::unlike
  db/default-interceptors
  (fn [{db :db} [blog]]
    (like-blog {:db    db
                :blog  blog
                :like? false})))