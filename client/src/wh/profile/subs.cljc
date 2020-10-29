(ns wh.profile.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.issue :refer [gql-issue->issue]]
            [wh.common.specs.primitives]
            [wh.common.subs :as _subs]
            [wh.common.time :as time]
            [wh.common.url :as url]
            [wh.components.tag :as tag]
            [wh.graphql-cache :as gql-cache]
            [wh.profile.db :as profile]
            [wh.profile.events :as profile-events]))

(reg-sub
  ::profile-query-result
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (gql-cache/result db query params))))

(reg-sub
  ::profile
  :<- [::profile-query-result]
  (fn [result _]
    (:user result)))

(reg-sub
  ::db
  (fn [db _]
    (::profile/sub-db db)))

(reg-sub
  ::issues
  :<- [::profile-query-result]
  (fn [result _]
    (->> [:query-issues :issues]
         (get-in result)
         (map gql-issue->issue))))

(reg-sub
  ::blogs
  :<- [::profile-query-result]
  (fn [result _]
    (get-in result [:blogs :blogs])))

(reg-sub
  ::skills
  :<- [::profile]
  (fn [profile _]
    (->> profile
         :skills
         (sort-by #(or (:rating %) 0) >))))

(reg-sub
  ::interests
  :<- [::profile]
  (fn [profile _]
    (map tag/->tag (:interests profile))))

(reg-sub
  ::social
  :<- [::profile]
  :<- [::profile-hidden?]
  (fn [[profile hidden?] [_ type]]
    (when-not hidden?
      (->> profile
           :other-urls
           url/detect-urls-type
           (some #(when (= type (:type %)) %))))))

(reg-sub
  ::last-seen
  :<- [::profile]
  :<- [::profile-hidden?]
  (fn [[profile hidden?] _]
    (when-not hidden?
      (:last-seen profile))))

(reg-sub
  ::updated
  :<- [::profile]
  :<- [::profile-hidden?]
  (fn [[profile hidden?] _]
    (when-not hidden?
      (:updated profile))))

(reg-sub
  ::percentile
  :<- [::profile]
  (fn [db _]
    (:percentile db)))

(reg-sub
  ::created
  :<- [::profile]
  (fn [db _]
    (:created db)))

(reg-sub
  ::profile-hidden?
  :<- [::profile]
  (fn [db _]
    (false? (:published db))))

;; alias standard subscription to incorporate query params
(reg-sub
  ::admin-view?
  :<- [:user/admin?]
  :<- [:wh/query-param "type"]
  (fn [[admin? type] _]
    (and admin? (not (= type "public")))))

(reg-sub
  ::hide-profile?
  :<- [::admin-view?]
  :<- [::profile-hidden?]
  (fn [[admin? profile-hidden?] _]
    (and (not admin?) profile-hidden?)))

(reg-sub
  ::error?
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (= :failure (gql-cache/state db query params)))))

(reg-sub
  ::loader?
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (boolean (#{:initial :executing} (gql-cache/state db query params))))))

;; magic number. I'm not going to try and show data only from 4 months everytime.
;; 18 seems like a safe, nice number of weeks
(def weeks-count 18)

(reg-sub
  ::contributions-collection
  :<- [::profile]
  (fn [profile _]
    (:contributions-collection profile)))

(reg-sub
  ::contributions-calendar
  :<- [::contributions-collection]
  (fn [contributions _]
    (->> (get-in contributions [:contribution-calendar :weeks])
         (map :contribution-days)
         (take-last weeks-count))))

(reg-sub
  ::contributions-count
  :<- [::contributions-collection]
  (fn [contributions _]
    (get contributions :total-commit-contributions)))

(reg-sub
  ::contributions-repos
  :<- [::contributions-collection]
  (fn [contributions _]
    (get contributions :total-repositories-with-contributed-commits)))

(defn week->month [week]
  (-> week first :date (time/str->time :date) time/short-month))

;; magic number. 4 fits quite nice into design
(def month-count 4)

(reg-sub
  ::contributions-months
  :<- [::contributions-calendar]
  (fn [contributions _]
    (->> contributions
         (map week->month)
         (distinct)
         (take-last month-count))))

(reg-sub
  ::liked-jobs
  :<- [::profile]
  (fn [profile _]
    (:likes profile)))

(reg-sub
  ::hs-url
  :<- [::profile]
  (fn [profile _]
    (:hubspot-profile-url profile)))

(reg-sub
  ::applications
  :<- [::profile]
  (fn [profile _]
    (:applied profile)))

(reg-sub
  ::approval-info
  :<- [::profile]
  (fn [profile _]
    (:approval profile)))

(reg-sub
  ::updating-status
  :<- [::db]
  (fn [db _]
    (profile/updating-status db)))


