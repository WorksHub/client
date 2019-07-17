(ns wh.company.profile.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.common.location :as location]
    [wh.company.profile.db :as profile]
    [wh.re-frame.subs :refer [<sub]])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::profile/sub-db db)))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [slug     (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company {:slug slug}])]
        (profile/->company (:company result))))))

(reg-sub-raw
  ::company-extra-data
  (fn [_ _]
    (reaction
      (let [slug     (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company-issues-and-blogs {:slug slug}])]
        (:company result)))))

(reg-sub-raw
  ::all-tags
  (fn [_ _]
    (->> (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags])
         (map profile/->tag)
         (reaction))))

(reg-sub-raw
  ::all-tags-of-type
  (fn [_ [_ tag-type]]
    (->> (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags])
         (filter #(= (str/lower-case (name tag-type)) (str/lower-case (name (:type %)))))
         (map profile/->tag)
         (reaction))))

(reg-sub
  ::profile-enabled?
  :<- [::company]
  (fn [company _]
    (:profile-enabled company)))

(reg-sub
  ::name
  :<- [::company]
  (fn [company _]
    (:name company)))

(reg-sub
  ::logo
  :<- [::company]
  (fn [company _]
    (:logo company)))

(reg-sub
  ::pending-logo
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/pending-logo sub-db)))

(reg-sub
  ::id
  :<- [::company]
  (fn [company _]
    (:id company)))

(reg-sub
  ::description
  :<- [::company]
  (fn [company _]
    (:description-html company)))

(reg-sub
  ::logo
  :<- [::company]
  (fn [company _]
    (:logo company)))

(reg-sub
  ::logo-uploading?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/logo-uploading? sub-db)))

(reg-sub
  ::tech-scales
  :<- [::company]
  (fn [company _]
    (:tech-scales company)))

(reg-sub
  ::tech-scale
  :<- [::company]
  (fn [company [_ k]]
    (get (:tech-scales company) k)))

(reg-sub
  ::how-we-work
  :<- [::company]
  (fn [company _]
    (:how-we-work company)))

(reg-sub
  ::tags
  :<- [::company]
  (fn [company [_ tag-type tag-subtype]]
    (if tag-type
      (filter (fn [tag] (and (= tag-type (:type tag))
                             (if tag-subtype
                               (= tag-subtype (:subtype tag))
                               true))) (:tags company))
      (:tags company))))

(reg-sub
  ::tags--inverted
  :<- [::company]
  (fn [company [_ tag-type tag-subtype]]
    (if tag-type
      (filter (fn [tag] (and (not= tag-type (:type tag))
                             (if tag-subtype
                               (not= tag-subtype (:subtype tag))
                               true))) (:tags company))
      (:tags company))))

(reg-sub
  ::industry
  :<- [::tags :industry]
  (fn [industry-tags _]
    (first industry-tags)))

(reg-sub
  ::funding
  :<- [::tags :funding]
  (fn [funding-tags _]
    (first funding-tags)))

(reg-sub
  ::images
  :<- [::company]
  (fn [company _]
    (map-indexed (fn [idx img] (assoc img :index idx)) (:images company))))

(reg-sub
  ::photo-uploading?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/photo-uploading? sub-db)))

(reg-sub
  ::videos
  :<- [::company]
  (fn [company _]
    (:videos company)))

(reg-sub
  ::blogs
  :<- [::company-extra-data]
  (fn [company-extra-data _]
    (-> company-extra-data :blogs :blogs)))

(reg-sub
  ::issues
  :<- [::company]
  :<- [::company-extra-data]
  (fn [[company extra-data] _]
    (->> extra-data
         :issues
         :issues
         (map (fn [issue]
                (-> issue
                    (assoc :company (select-keys company [:logo]))
                    (gql-issue->issue)))))))

(reg-sub
  ::video-error
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/video-error sub-db)))

(reg-sub
  ::size
  :<- [::company]
  (fn [company _]
    (:size company)))

(reg-sub
  ::founded-year
  :<- [::company]
  (fn [company _]
    (:founded-year company)))

(reg-sub
  ::location
  :<- [::company]
  (fn [company _]
    (some-> company :locations first location/format-location)))

(reg-sub
  ::pending-location--raw
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/pending-location sub-db)))

(reg-sub
  ::pending-location
  :<- [::pending-location--raw]
  (fn [pending-location _]
    (some-> pending-location location/format-location)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/updating? sub-db)))

(reg-sub
  ::tag-search
  :<- [::sub-db]
  (fn [sub-db [_ tag-type tag-subtype]]
    (or (get-in sub-db [::profile/tag-search tag-type tag-subtype]) "")))

(reg-sub
  ::tag-search--map
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/tag-search sub-db)))

(defn tag->form-tag
  [{:keys [id label]}]
  {:tag label
   :key id
   :selected false})

(reg-sub-raw
  ::current-tag-ids
  (fn [_ [_ tag-type tag-subtype]]
    (reaction
      (set (map :id (<sub [::tags tag-type tag-subtype]))))))

(reg-sub-raw
  ::current-tag-ids--inverted
  (fn [_ [_ type]]
    (reaction
      (set (map :id (<sub [::tags--inverted type]))))))

(reg-sub
  ::matching-tags
  :<- [::all-tags]
  :<- [::tag-search--map]
  (fn [[tags tag-search] [_ {:keys [include-ids size type subtype]}]]
    (let [tag-search (str/lower-case (or (get-in tag-search [type subtype]) ""))
          matching-but-not-included (filter (fn [tag] (and (or (str/blank? tag-search)
                                                               (str/includes? (str/lower-case (:label tag)) tag-search))
                                                           (= type (:type tag))
                                                           (= subtype (:subtype tag))
                                                           (not (contains? include-ids (:id tag))))) tags)
          included-tags (filter (fn [tag] (contains? include-ids (:id tag))) tags)]
      (->> (concat included-tags matching-but-not-included)
           (map tag->form-tag)
           (take (+ (or size 20) (count include-ids)))))))

(reg-sub
  ::creating-tag?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/creating-tag? sub-db)))

(reg-sub
  ::selected-tag-ids
  :<- [::sub-db]
  (fn [sub-db [_ tag-type tag-subtype]]
    (set (get-in sub-db (cond-> [::profile/selected-tag-ids tag-type]
                                tag-subtype (conj tag-subtype))))))

(reg-sub
  ::location-search
  :<- [::sub-db]
  (fn [db _]
    (::profile/location-search db)))

(reg-sub
  ::location-suggestions
  :<- [::sub-db]
  (fn [db _]
    (let [search-term-lower (some-> (::profile/location-search db) (str/lower-case))]
      (when-not (str/blank? search-term-lower)
        (let [results (->> (::profile/location-suggestions db)
                           (map :description)
                           (map #(hash-map :id % :label %))
                           (take 5)
                           (vec))]
          (if (and (= 1 (count results))
                   (= search-term-lower (some-> results (first) (:label) (str/lower-case))))
            nil
            results))))))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [db _]
    nil))
