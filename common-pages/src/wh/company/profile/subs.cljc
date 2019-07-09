(ns wh.company.profile.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.company.profile.db :as profile]
    [wh.graphql-cache :as gql-cache]
    [wh.re-frame.subs :refer [<sub]]
    [clojure.string :as str])
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
      (let [id     (<sub [:wh/page-param :id])
            result (<sub [:graphql/result :company {:id id}])]
        (profile/->company (:company result))))))

(reg-sub-raw
  ::all-tags
  (fn [_ _]
    (reaction
      (map profile/->tag (get-in (<sub [:graphql/result :tags {}]) [:list-tags :tags])))))

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
  ::tags
  :<- [::company]
  (fn [company [_ type]]
    (if type
      (filter (fn [tag] (= type (:type tag))) (:tags company))
      (:tags company))))

(reg-sub
  ::tags--inverted
  :<- [::company]
  (fn [company [_ type]]
    (if type
      (filter (fn [tag] (not= type (:type tag))) (:tags company))
      (:tags company))))

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
  :<- [::company]
  (fn [company _]
    (-> company :blogs :blogs)))

(reg-sub
  ::video-error
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/video-error sub-db)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/updating? sub-db)))

(reg-sub
  ::tag-search
  :<- [::sub-db]
  (fn [sub-db [_ tag-type]]
    (or (get-in sub-db [::profile/tag-search tag-type]) "")))

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
  (fn [_ [_ type]]
    (reaction
      (set (map :id (<sub [::tags type]))))))

(reg-sub-raw
  ::current-tag-ids--inverted
  (fn [_ [_ type]]
    (reaction
      (set (map :id (<sub [::tags--inverted type]))))))

(reg-sub
  ::matching-tags
  :<- [::all-tags]
  :<- [::tag-search--map]
  (fn [[tags tag-search] [_ {:keys [include-ids size type]}]]
    (let [tag-search (str/lower-case (or (get tag-search type) ""))
          matching-but-not-included (filter (fn [tag] (and (or (str/blank? tag-search)
                                                               (str/includes? (str/lower-case (:label tag)) tag-search))
                                                           (= type (:type tag))
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
  (fn [sub-db [_ tag-type]]
    (set (get-in sub-db [::profile/selected-tag-ids tag-type]))))

(reg-sub
  ::development-setup
  :<- [::company]
  (fn [company [_ sub-key]]
    (get-in company [:dev-setup sub-key])))

(reg-sub
  ::video-error
  :<- [::sub-db]
  (fn [db _]
    (::profile/video-error db)))
