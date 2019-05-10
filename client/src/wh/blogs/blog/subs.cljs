(ns wh.blogs.blog.subs
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [goog.i18n.NumberFormat :as nf]
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [reagent.ratom :refer [reaction]]
    [wh.blogs.blog.db :as blog]
    [wh.common.http :as http]
    [wh.common.user :as common-user]
    [wh.components.cards.subs :as cards-subs]
    [wh.graphql.jobs :as jobs]
    [wh.subs :refer [<sub]])
  (:require-macros [clojure.core.strint :refer [<<]]))

;; These subscriptions get blog/recommended-jobs query results for the
;; currently viewed blog. They are registered with reg-sub-raw because
;; they need to pass one dependent subscription's value as another's
;; parameter.

(reg-sub-raw
  ::blog
  (fn [_ _]
    (reaction
     (let [id     (<sub [:wh.subs/page-param :id])
           result (<sub [:graphql/result :blog {:id id}])]
       (:blog result)))))

(reg-sub-raw
  ::recommended-jobs
  (fn [_ _]
    (reaction
     (let [id           (<sub [:wh.subs/page-param :id])
           results      (<sub [:graphql/result :recommended-jobs-for-blog {:id id}])
           liked-jobs   (<sub [:wh.user/liked-jobs])
           applied-jobs (<sub [:wh.user/applied-jobs])]
       (jobs/add-interactions liked-jobs applied-jobs (:jobs results))))))

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
    (:reading-time blog)))

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
  ::recommendations-heading
  :<- [::recommended-jobs]
  :<- [::tags]
  (fn [[recommended-jobs tags] _]
    (->> recommended-jobs
         (mapcat :tags)
         (map str/lower-case)
         set
         (set/intersection (set (map str/lower-case tags)))
         (map str/capitalize)
         (str/join ", "))))

(reg-sub
  ::show-public-only?
  :<- [::recommended-jobs]
  (fn [jobs _]
    (jobs/show-public-only? jobs)))

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
  (let [formatter (goog.i18n.NumberFormat. nf/Format.COMPACT_SHORT)]
    (fn [[db id] _]
      (.format formatter (get-in db [::blog/upvotes id])))))

;; Finally, these ones return visibility of UI items based on other pieces
;; of app's state.

(reg-sub
  ::can-edit?
  :<- [::creator]
  :<- [:user/admin?]
  :<- [:user/email]
  :<- [::published?]
  (fn [[creator admin? email published?] _]
    (cards-subs/can-edit-blog? admin? creator email published?)))

(reg-sub
  ::show-unpublished?
  :<- [:user/admin?]
  :<- [::creator]
  :<- [:user/email]
  :<- [::published?]
  (fn [[admin? creator email published?] _]
    (cards-subs/show-blog-unpublished? admin? creator email published?)))

(reg-sub
  ::show-get-started-banner?
  :<- [:wh.subs/vertical]
  :<- [:user/logged-in?]
  (fn [[vertical logged-in?] _]
    (and (not (= vertical "www")) (not logged-in?))))
