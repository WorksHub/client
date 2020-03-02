(ns wh.blogs.blog.subs
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.blogs.blog.db :as blog]
    [wh.common.job :as common-job]
    [wh.common.issue :as common-issue]
    [wh.common.user :as common-user]
    [wh.graphql.jobs :as jobs]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]
    [clojure.core.strint :refer [<<]]))

;; These subscriptions get blog/recommended-jobs query results for the
;; currently viewed blog. They are registered with reg-sub-raw because
;; they need to pass one dependent subscription's value as another's
;; parameter.

(reg-sub-raw
  ::blog
  (fn [db _]
    (->> (<sub [:graphql/result :blog (blog/params @db)])
         (:blog)
         (reaction))))

(reg-sub-raw
  ::blog-error
  (fn [db _]
    (reaction (<sub [:graphql/error-key :blog (blog/params @db)]))))

(reg-sub-raw
  ::recommendations
  (fn [db _]
    (reaction
      (let [state        (<sub [:graphql/state :recommendations-query-for-blogs (blog/params @db)])
            results      (<sub [:graphql/result :recommendations-query-for-blogs (blog/params @db)])
            liked-jobs   (<sub [:user/liked-jobs])
            applied-jobs (<sub [:user/applied-jobs])
            stub         (util/maps-with-id blog/page-size)]
        (if (= :executing state)
          {:issues stub
           :jobs   stub
           :blogs  stub}
          {:issues (map
                     common-issue/gql-issue->issue
                     (get-in results [:query-issues :issues]))
           :jobs   (map
                     common-job/translate-job
                     (jobs/add-interactions liked-jobs applied-jobs (:jobs results)))
           :blogs  (get-in results [:blogs :blogs])})))))

;; These subscriptions extract various bits from ::blog and ::recommended-jobs.

(reg-sub
  ::id
  :<- [::blog]
  (fn [blog _]
    (:id blog)))

(reg-sub
  ::title
  :<- [::blog]
  (fn [blog _]
    (:title blog)))

(reg-sub
  ::feature
  :<- [::blog]
  (fn [blog _]
    (:feature blog)))

(reg-sub
  ::author
  :<- [::blog]
  (fn [blog _]
    (:author blog)))

(reg-sub
  ::published?
  :<- [::blog]
  (fn [blog _]
    (:published blog)))

(reg-sub
  ::tags
  :<- [::blog]
  (fn [blog _]
    (:tags blog)))

(reg-sub
  ::reading-time
  :<- [::blog]
  (fn [blog _]
    (or (:reading-time blog) 0)))

(reg-sub
  ::html-body
  :<- [::blog]
  (fn [blog _]
    (:html-body blog)))

(reg-sub
  ::formatted-creation-date
  :<- [::blog]
  (fn [blog _]
    (:formatted-creation-date blog)))

(reg-sub
  ::creator
  :<- [::blog]
  (fn [blog _]
    (:creator blog)))

(reg-sub
  ::original-source
  :<- [::blog]
  (fn [blog _]
    (:original-source blog)))

(reg-sub
  ::show-original-source?
  :<- [::original-source]
  (fn [source _]
    (not (str/blank? source))))

(reg-sub
  ::original-source-domain
  :<- [::original-source]
  (fn [source _]
    (some-> source
            (str/replace #"^https?://" "")
            (str/replace #"/.*" ""))))

(reg-sub
  ::author-info
  :<- [::blog]
  (fn [db _]
    (when-let [info (:author-info db)]
      (update info
              :image-url #(or % (common-user/random-avatar-url))))))

(reg-sub
  ::company-name
  :<- [::blog]
  (fn [blog _]
    (:name (:company blog))))

(reg-sub
  ::recommendations-heading
  :<- [::recommended-jobs]
  :<- [::tags]
  (fn [[recommended-jobs tags] _]
    (let [tags-labels (->> tags
                           (map :label)
                           (map str/lower-case)
                           set)]
      (->> recommended-jobs
           (mapcat :tags)
           (map str/lower-case)
           set
           (set/intersection tags-labels)
           (map str/capitalize)
           (str/join ", ")))))

(reg-sub
  ::recommendations-from-company?
  :<- [::company-name]
  :<- [::recommended-jobs]
  (fn [[company-name recommended-jobs] _]
    (every? (comp (partial = company-name) :company-name) recommended-jobs)))

;; These subscriptions refer to client-side state of the blog.

(reg-sub
  ::blog-db
  (fn [db _]
    (::blog/sub-db db)))

(reg-sub
  ::share-links-shown?
  :<- [::blog-db]
  (fn [db _]
    (::blog/share-links-shown? db)))

(reg-sub
  ::author-info-visible?
  :<- [::blog-db]
  (fn [db _]
    (::blog/author-info-visible? db)))

(reg-sub
  ::upvote-count
  :<- [::blog-db]
  :<- [::id]
  (fn [[db id] _]
    (util/number->compact-str (get-in db [::blog/upvotes id]))))

;; Finally, these ones return visibility of UI items based on other pieces
;; of app's state.

(defn can-edit-blog? [admin? creator user-email published?]
  (or admin?
      (and (and creator (= creator user-email))
           (and (not (nil? published?)) (not published?)))))

(reg-sub
  ::can-edit?
  :<- [::creator]
  :<- [:user/admin?]
  :<- [:user/email]
  :<- [::published?]
  (fn [[creator admin? email published?] _]
    (can-edit-blog? admin? creator email published?)))

(defn show-blog-unpublished? [admin? creator user-email published?]
  (and (and (not (nil? published?)) (not published?))
       (or admin?
           (and user-email (= creator user-email)))))

(reg-sub
  ::show-unpublished?
  :<- [:user/admin?]
  :<- [::creator]
  :<- [:user/email]
  :<- [::published?]
  (fn [[admin? creator email published?] _]
    (show-blog-unpublished? admin? creator email published?)))
