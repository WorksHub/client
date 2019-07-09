(ns wh.blogs.learn.subs
  (:require
    [bidi.bidi :as bidi]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [wh.blogs.learn.db :as learn]
    [wh.components.pagination :as pagination]
    [wh.graphql-cache :as graphql]))

(reg-sub
  ::current-tag
  (fn [db _]
    (when-let [tag (learn/tag db)]
      (str/capitalize tag))))

(reg-sub
  ::total-pages
  (fn [db _]
    (let [result (graphql/result db :blogs (learn/params db))
          count (or (get-in result [:blogs :pagination :total]) learn/page-size)]
      #?(:clj (int (Math/ceil (/ count learn/page-size)))
         :cljs (js/Math.ceil (/ count learn/page-size))))))

(reg-sub
  ::current-page
  (fn [db _]
    (learn/current-page db)))

(reg-sub
  ::header
  :<- [::current-tag]
  (fn [current-tag _]
    (if current-tag
      (str "Learn " current-tag)
      "Learn")))

(reg-sub
  ::sub-header
  :<- [::current-tag]
  :<- [:wh/vertical-label]
  (fn [[current-tag vertical-label] _]
    (str "The latest news, resources and thoughts from the world of " (or current-tag
                                                                          vertical-label))))

(reg-sub
  ::show-contribute?
  :<- [:wh/vertical]
  :<- [:user/admin?]
  (fn [[vertical admin?] _]
    (or admin? (not= "www" vertical))))

(reg-sub
  ::all-blogs
  (fn [db _]
    (let [params (learn/params db)]
      (if (= (graphql/state db :blogs params) :executing)
        (repeat learn/page-size {})
        (get-in (graphql/result db :blogs params) [:blogs :blogs])))))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))
