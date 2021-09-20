(ns wh.blogs.learn.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.blogs.learn.db :as learn]
            [wh.blogs.learn.events :as learn-events]
            [wh.verticals :as verticals]
            [wh.common.issue :refer [gql-issue->issue]]
            [wh.common.job :as job]
            [wh.components.pagination :as pagination]
            [wh.graphql-cache :as graphql]
            [wh.util :as util]))

(reg-sub
  ::db
  (fn [db _]
    db))

(reg-sub
  ::current-page
  (fn [db _]
    (learn/current-page db)))

(reg-sub
  ::search-term
  (fn [db _]
    (learn/search-term db)))

(reg-sub
  ::all-blogs
  (fn [db _]
    (let [params      (learn/params db)
          search-term (learn/search-term db)
          query-name  (if search-term
                        :search_blogs
                        :blogs)
          data-path   (if search-term
                        learn-events/search-blogs-path
                        learn-events/std-blogs-path)]
      (get-in (graphql/result db query-name params) data-path))))

(reg-sub
  ::all-blogs-loading?
  (fn [db _]
    (let [params      (learn/params db)
          search-term (learn/search-term db)
          query-name  (if search-term
                        :search_blogs
                        :blogs)]
      (= (graphql/state db query-name params) :executing))))

(reg-sub
  ::current-tag
  :<- [::db]
  :<- [::all-blogs]
  (fn [[db all-blogs] _]
    (when-let [tag-slug (learn/tag db)]
      (let [has-correct-slug? (fn [tag]
                                (= (clojure.string/lower-case tag-slug)
                                   (:slug tag)))]
        (->> all-blogs
             (mapcat :tags)
             (some #(when (has-correct-slug? %) %)))))))

(reg-sub
  ::sub-header
  :<- [::current-tag]
  :<- [:wh/vertical-label]
  (fn [[current-tag vertical-label] _]
    (str "The latest news, resources and thoughts from the world of "
         (or (:label current-tag) vertical-label))))

(reg-sub
  ::header
  :<- [::current-tag]
  (fn [current-tag _]
    (if current-tag
      (str "Articles: " (:label current-tag))
      "Articles")))

(reg-sub
  ::total-pages
  (fn [db _]
    (let [params (learn/params db)
          search-term (learn/search-term db)
          query-name (if search-term
                       :search_blogs
                       :blogs)
          total-blogs-path (if search-term
                             [:search-blogs :pagination :total]
                             [:blogs :pagination :total])
          result (graphql/result db query-name params)
          count (or (get-in result total-blogs-path) learn/page-size)]
      #?(:clj (int (Math/ceil (/ count learn/page-size)))
         :cljs (js/Math.ceil (/ count learn/page-size))))))

;; less blogs -> less recommendation
(defn max-amount-to-display [all-blogs]
  (-> (count all-blogs)
      (quot 2)
      dec
      (max 1)))

(reg-sub
  ::recommended-jobs
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  :<- [::db]
  :<- [::all-blogs]
  (fn [[liked applied db all-blogs] _]
    (let [params            (learn/articles-jobs-params db)
          query-name        (learn-events/query-name-for-articles-jobs db)
          state             (graphql/state db query-name params)
          amount-to-display (max-amount-to-display all-blogs)
          recommended       (get-in (graphql/result db query-name params)
                                    [:jobs-search
                                     (if (learn/search-arguments? db) :jobs :promoted)])]
      (if (= state :executing)
        (util/maps-with-id amount-to-display)
        (->> (job/add-interactions liked applied recommended)
             (job/sort-by-user-score)
             (map job/translate-job)
             (take amount-to-display))))))

(reg-sub
  ::recommended-issues
  :<- [::db]
  :<- [::all-blogs]
  (fn [[db all-blogs] _]
    (let [params            (learn/articles-issues-params db)
          query-name        :recommended_issues
          state             (graphql/state db query-name params)
          amount-to-display (max-amount-to-display all-blogs)
          recommended       (get-in (graphql/result db query-name params) [:query-issues :issues])]
      (if (= state :executing)
        (util/maps-with-id amount-to-display)
        (->> recommended
             (map gql-issue->issue)
             (take amount-to-display))))))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))

(reg-sub
  ::tagbox-tags
  (fn [db _]
    (get-in verticals/vertical-config [(:wh.db/vertical db) :articles-tags])))
