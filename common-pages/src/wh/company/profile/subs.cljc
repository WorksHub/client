(ns wh.company.profile.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.common.location :as location]
    [wh.company.profile.db :as profile]
    [wh.components.stats.db :as stats]
    [wh.components.tag :refer [->tag tag->form-tag]]
    [wh.re-frame.subs :refer [<sub]])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::profile/sub-db db)))

(reg-sub-raw
  ::company-query-loading?
  (fn [_ _]
    (reaction
      (let [slug  (<sub [:wh/page-param :slug])
            state (<sub [:graphql/state :company {:slug slug}])]
        (or (not state) (= state :executing))))))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [slug   (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company {:slug slug}])]
        (profile/->company (:company result))))))

(reg-sub-raw
  ::company-extra-data
  (fn [_ _]
    (reaction
      (let [slug   (<sub [:wh/page-param :slug])
            result (<sub [:graphql/result :company-issues-and-blogs {:slug slug}])]
        (:company result)))))

(reg-sub-raw
  ::stats
  (fn [_ _]
    (reaction
      (let [slug    (<sub [:wh/page-param :slug])
            company (<sub [:graphql/result :company {:slug slug}])
            result  (<sub [:graphql/result :company-stats {:company_id (-> company :company :id)}])]
        (:job-analytics result)))))

(reg-sub-raw
  ::all-tags
  (fn [_ _]
    (->> (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags])
         (map ->tag)
         (reaction))))

(reg-sub-raw
  ::all-tags-of-type
  (fn [_ [_ tag-type]]
    (->> (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags])
         (filter #(= (str/lower-case (name tag-type)) (str/lower-case (name (:type %)))))
         (map ->tag)
         (reaction))))

(reg-sub
  ::slug
  :<- [::company]
  (fn [company _]
    (:slug company)))

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
  ::pending-logo
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/pending-logo sub-db)))

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
  ::additional-tech-info
  :<- [::company]
  (fn [company _]
    (when-let [ati (:additional-tech-info company)]
      (when (and ati (not= ati "<p><br></p>")) ;; quill's version of 'empty'
        ati))))

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
  ::jobs
  :<- [::company-extra-data]
  (fn [company-extra-data _]
    (not-empty
      (-> company-extra-data :jobs :jobs))))

(reg-sub
  ::total-number-of-jobs
  :<- [::company-extra-data]
  (fn [company-extra-data _]
    (or (-> company-extra-data :jobs :pagination :total) 0)))

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
  ::repos
  :<- [::company-extra-data]
  (fn [extra-data _]
    (->> extra-data :repos :repos)))

(reg-sub
  ::github-orgs
  :<- [::repos]
  (fn [repos _]
    (set (map :owner repos))))

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

(defn add-readable-name-to-locations [locations]
  (some->> locations
           (map #(hash-map
                  :location/data %
                  :location/name (location/format-location %)))))

;; This sub is used to display list of actual company locations.
;; It changes only after admin modified company details
(reg-sub
 ::locations
 :<- [::company]
 (fn [company _]
   (add-readable-name-to-locations (:locations company))))

;; This one is distinguished from normal ::locations sub, to
;; to allow edition of locations list, when editing company details.
;; It's ephemeral. It's value is important only when editing
;; and updating locations
(reg-sub
 ::current-locations
 :<- [::sub-db]
 (fn [db _]
   (add-readable-name-to-locations (::profile/current-locations db))))

(reg-sub
  ::has-published-profile?
  :<- [::company]
  (fn [company _]
    (or (:has-published-profile company)
        (:profile-enabled company))))

(reg-sub
  ::pending-locations--raw
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/pending-locations sub-db)))

(reg-sub
  ::pending-locations
  :<- [::pending-locations--raw]
  (fn [pending-locations _]
    (add-readable-name-to-locations pending-locations)))

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
                                                           (if subtype (= subtype (:subtype tag)) true)
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
    (set (get-in sub-db [::profile/selected-tag-ids tag-type tag-subtype]))))


(reg-sub-raw
  ::selected-tag-ids--all-of-type
  (fn [_ [_ tag-type include-existing?]]
    (reaction
      (let [sub-db (<sub [::sub-db])
            r (set (reduce concat (vals (get-in sub-db [::profile/selected-tag-ids tag-type]))))]
        (if include-existing?
          (reduce conj r (map :id (<sub [::tags tag-type])))
          r)))))

(reg-sub
  ::selected-tag-ids--map
  :<- [::sub-db]
  (fn [sub-db [_ tag-type]]
    (set (get-in sub-db (cond-> [::profile/selected-tag-ids tag-type])))))

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
  ::publishing?
  :<- [::sub-db]
  (fn [db _]
    (::profile/publishing? db)))

(reg-sub
  ::show-sticky?
  :<- [::sub-db]
  (fn [db _]
    (boolean (::profile/show-stick? db))))

(reg-sub
  ::show-jobs-link?
  :<- [:user/candidate?]
  :<- [::jobs]
  (fn [[candidate? jobs] _]
    ;; we only hide the job link if
    ;; user is a candidate and there are no jobs to show
    (not (and candidate? (not jobs)))))

(reg-sub
  ::error-message
  :<- [::sub-db]
  (fn [sub-db [_ k]]
    (get-in sub-db [::profile/error-map k])))

(reg-sub
  ::granularity
  :<- [::stats]
  (fn [stats _]
    (if (= (:granularity stats) 7)
      :week
      :month)))

(reg-sub
  ::stats-title
  :<- [::granularity]
  (fn [granularity _]
    (if (= granularity :week)
      "Last 7 Days’ Stats"
      "Last Month’s Stats")))

(reg-sub
  ::stats-item
  :<- [::stats]
  (fn [stats [_ stat]]
    (stats/stat-item-data stats stat)))
