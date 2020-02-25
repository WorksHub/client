(ns wh.blogs.learn.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [wh.blogs.learn.db :as learn]
    [wh.blogs.learn.events :as learn-events]
    [wh.verticals :as verticals]
    [wh.graphql.jobs :as jobs]
    [wh.common.job :as jobc]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.components.pagination :as pagination]
    [wh.graphql-cache :as graphql]))

(reg-sub
  ::current-tag
  (fn [db _]
    (when-let [tag (learn/tag db)]
      (str/capitalize tag))))

(reg-sub
  ::current-page
  (fn [db _]
    (learn/current-page db)))

(reg-sub
  ::search-term
  (fn [db _]
    (learn/search-term db)))

(reg-sub
  ::header
  :<- [::current-tag]
  (fn [current-tag _]
    (if current-tag
      (str "Articles: " current-tag)
      "Articles")))

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
    (let [params (learn/params db)
          search-term (learn/search-term db)
          query-name (if search-term
                       :search_blogs
                       :blogs)
          data-path (if search-term
                      learn-events/search-blogs-path
                      learn-events/std-blogs-path)]
      (if (= (graphql/state db query-name params) :executing)
        (map (partial hash-map :id) (range learn/page-size))
        (get-in (graphql/result db query-name params) data-path)))))

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

(reg-sub
  ::db
  (fn [db _]
    db))

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
    (let [params (learn/params db)
          query-name :recommended_jobs
          state (graphql/state db query-name params)
          amount-to-display (max-amount-to-display all-blogs)
          recommended (get-in (graphql/result db query-name params) [:jobs-search :promoted])]
      (if (= state :executing)
        (->> (range amount-to-display)
             (map (partial hash-map :id)))
        (->> (jobs/add-interactions liked applied recommended)
             (map #(assoc % :display-location (jobc/format-job-location (:location %) (:remote %))))
             (take amount-to-display))))))

(reg-sub
  ::recommended-issues
  :<- [::db]
  :<- [::all-blogs]
  (fn [[db all-blogs] _]
    (let [params (learn/params db)
          query-name :recommended_issues
          state (graphql/state db query-name params)
          amount-to-display (max-amount-to-display all-blogs)
          recommended (get-in (graphql/result db query-name params) [:query-issues :issues])]
      (if (= state :executing)
        (->> (range amount-to-display)
             (map (partial hash-map :id)))
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
