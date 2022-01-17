(ns wh.blogs.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.blogs.blog.db :as blog-db]
    [wh.blogs.blog.views :as blog]
    [wh.blogs.blog.events]
    [wh.blogs.learn.views :as learn]
    [wh.blogs.learn.events]
    [wh.blogs.liked.views :as liked]
    [wh.blogs.liked.events]
    [wh.db :as db]))

(def page-mapping
  {:blog         blog/page
   :learn        learn/page
   :learn-search learn/page
   :learn-by-tag learn/page
   :liked-blogs  liked/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj ::blog-db/sub-db)

(dispatch-sync [::initialize-page-mapping])
;; don't unset loader here; too early

(db/redefine-app-db-spec!)

(loader/set-loaded! :blogs)
