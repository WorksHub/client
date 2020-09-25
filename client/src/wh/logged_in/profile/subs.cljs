(ns wh.logged-in.profile.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.common.data :as data]
            [wh.common.keywords :as keywords]
            [wh.common.specs.primitives]
            [wh.common.text :as text]
            [wh.common.url :as url]
            [wh.logged-in.profile.db :as profile]
            [wh.subs :refer [with-unspecified-option]]
            [wh.user.db :as user])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::profile
  (fn [db _]
    (::profile/sub-db db)))

(defn header-data
  [data contributions]
  (-> data
      (select-keys [:image-url :name :skills :other-urls :summary :stackoverflow-info
                    :github-id :twitter-info :last-seen :updated])
      (update :other-urls url/detect-urls-type)
      (assoc :contributions contributions)))

(reg-sub
  ::header-data
  :<- [::profile]
  (fn [profile _]
    (header-data
      (keywords/strip-ns-from-map-keys profile)
      (::profile/contributions profile))))

(reg-sub
  ::skills
  :<- [::profile]
  (fn [profile _]
    (sort-by #(or (:rating %) 0) > (::profile/skills profile))))

(reg-sub
  ::social-urls
  :<- [::profile]
  (fn [profile]
    (-> profile
        keywords/strip-ns-from-map-keys
        :other-urls
        url/detect-urls-type)))

(reg-sub
  ::social
  :<- [::social-urls]
  (fn [urls [_ type]]
    (some #(when (= type (:type %)) %) urls)))

(reg-sub
  ::currencies
  (with-unspecified-option data/currencies))

(reg-sub
  ::time-periods
  (with-unspecified-option data/time-periods))

(reg-sub
  ::image-url
  :<- [::header-data]
  (fn [header _]
    (:image-url header)))

(reg-sub
  ::predefined-avatar
  :<- [::profile]
  (fn [profile _]
    (::profile/predefined-avatar profile 1)))

(reg-sub
  ::custom-avatar-mode
  :<- [::profile]
  (fn [profile _]
    (::profile/custom-avatar-mode profile)))

(reg-sub
  ::name
  :<- [::header-data]
  (fn [header _]
    (:name header)))

(reg-sub
  ::rated-skills
  :<- [::header-data]
  (fn [header _]
    (:skills header)))

(reg-sub
  ::summary
  :<- [::header-data]
  (fn [header _]
    (:summary header)))

(reg-sub
  ::other-urls
  :<- [::header-data]
  (fn [header _]
    (:other-urls header)))

(reg-sub
  ::editable-urls
  :<- [::profile]
  (fn [profile _]
    (mapv :url (::profile/other-urls profile))))

(reg-sub
  ::contributions
  :<- [::profile]
  (fn [profile _]
    (vec (::profile/contributions profile))))

(reg-sub
  ::issues
  :<- [::profile]
  (fn [profile _]
    (::profile/issues profile)))

(reg-sub
  ::published?
  (fn [db _]
    (user/published? db)))

(defn salary-string [{:keys [min currency time-period]}]
  (let [min         (text/not-blank min)
        time-period (when time-period (str/lower-case time-period))]
    (if min
      (<< "Minimum ~{min} ~{currency} ~{time-period}")
      "Unspecified")))

(defn visa-status-string
  [statuses other]
  (let [have-other? (contains? statuses "Other")
        otherless   (disj statuses "Other")]
    (if (seq statuses)
      (str/join ", " (cond-> otherless have-other? (conj other)))
      "No statuses selected")))

(defn location-label [loc]
  (when loc
    (<< "~(:location/city loc), ~(:location/country loc)")))

(defn preferred-location-strings
  [locations]
  (mapv location-label locations))

(defn private-data [private]
  (-> private
      (update :salary salary-string)
      (update :visa-status visa-status-string (str (:visa-status-other private)))
      (update :company-perks (partial mapv :name))
      (update :current-location location-label)
      (update :preferred-locations preferred-location-strings)
      (update :remote boolean)
      (select-keys [:email :job-seeking-status :company-perks :salary :visa-status :current-location :preferred-locations :remote :role-types :phone])))

(reg-sub
  ::private-data
  :<- [::profile]
  (fn [profile _]
    (private-data (keywords/strip-ns-from-map-keys profile))))

(reg-sub
  ::company-data
  :<- [::profile]
  (fn [profile _]
    (-> profile
        keywords/strip-ns-from-map-keys
        (select-keys [:email :name]))))

(reg-sub
  ::num-skills
  :<- [::profile]
  (fn [profile _]
    (count (::profile/skills profile))))

(reg-sub
  ::skill
  :<- [::profile]
  (fn [profile [_ i]]
    (:name (get-in profile [::profile/skills i]))))

(reg-sub
  ::avatar-uploading?
  :<- [::profile]
  (fn [profile _]
    (::profile/avatar-uploading? profile)))

(reg-sub
  ::cv-uploading?
  :<- [::profile]
  (fn [profile _]
    (::profile/cv-uploading? profile)))

(reg-sub
  ::cover-letter-uploading?
  :<- [::profile]
  (fn [profile _]
    (::profile/cover-letter-uploading? profile)))

(reg-sub
  ::email
  :<- [::private-data]
  (fn [private _]
    (:email private)))

(reg-sub
  ::phone
  :<- [::private-data]
  (fn [header _]
    (:phone header)))

(reg-sub
  ::job-seeking-status
  :<- [::private-data]
  (fn [private _]
    (:job-seeking-status private)))

(reg-sub
  ::company-perks
  :<- [::private-data]
  (fn [private _]
    (:company-perks private)))

(reg-sub
  ::salary-string
  :<- [::private-data]
  (fn [db _]
    (:salary db)))

(reg-sub
  ::url-save-success
  :<- [::profile]
  (fn [profile]
    (::profile/url-save-success profile)))

(reg-sub
  ::job-seeking-statuses
  (constantly [{:id nil, :label "What's your status?"}
               "Looking for a new job"
               "Open to offers"
               "No offers please"]))

(reg-sub
  ::visa-statuses
  (constantly (sort data/visa-options)))

(reg-sub
  ::visa-status
  :<- [::profile]
  (fn [profile _]
    (::profile/visa-status profile)))

(reg-sub
  ::role-types
  :<- [::profile]
  (fn [profile _]
    (::profile/role-types profile)))

(reg-sub
  ::visa-status-other
  :<- [::profile]
  (fn [profile _]
    (::profile/visa-status-other profile)))

(reg-sub
  ::visa-status-other-visible?
  :<- [::visa-status]
  (fn [status _]
    (contains? status "Other")))

(reg-sub
  ::salary-min
  :<- [::profile]
  (fn [profile _]
    (get-in profile [::profile/salary :min])))

(reg-sub
  ::salary-currency
  :<- [::profile]
  (fn [profile _]
    (get-in profile [::profile/salary :currency])))

(reg-sub
  ::salary-time-period
  :<- [::profile]
  (fn [profile _]
    (get-in profile [::profile/salary :time-period])))

(reg-sub
  ::preferred-location-labels
  :<- [::profile]
  (fn [profile _]
    (mapv #(if (string? %) % (location-label %))
          (::profile/preferred-locations profile))))

(reg-sub
  ::current-location-label
  :<- [::profile]
  (fn [profile _]
    (or (::profile/current-location-text profile)
        (location-label (::profile/current-location profile)))))

(defn location->suggestion
  [x]
  {:label (location-label x), :id x})

(reg-sub
  ::current-location-suggestions
  :<- [::profile]
  (fn [profile [_ i]]
    (mapv location->suggestion (::profile/current-location-suggestions profile))))

(reg-sub
  ::suggestions
  :<- [::profile]
  (fn [profile [_ i]]
    (mapv location->suggestion (get-in profile [::profile/location-suggestions i]))))

(reg-sub
  ::error-fields
  :<- [::profile]
  (fn [profile _]
    (->> profile
         (wh.common.specs.primitives/problematic-paths ::profile/sub-db)
         (map #(-> % first name keyword))
         set)))

(defn cv-data
  [{{:keys [file link] :as cv} :cv}]
  {:cv-url      (text/not-blank (:url file)),
   :cv-filename (text/not-blank (:name file)),
   :cv-link     (text/not-blank link)})

(reg-sub
  ::cv-data
  :<- [::profile]
  (fn [profile _]
    (cv-data (keywords/strip-ns-from-map-keys profile))))

(defn cover-letter-data
  [{{:keys [file link] :as cover-letter} :cover-letter}]
  {:cover-letter-url      (text/not-blank (:url file)),
   :cover-letter-filename (text/not-blank (:name file)),
   :cover-letter-link     (text/not-blank link)})

(reg-sub
  ::cover-letter-data
  :<- [::profile]
  (fn [profile _]
    (cover-letter-data (keywords/strip-ns-from-map-keys profile))))

(reg-sub
  ::remote
  :<- [::profile]
  (fn [db]
    (::profile/remote db)))

(reg-sub
  ::percentile
  :<- [::profile]
  (fn [db]
    (::profile/percentile db)))

(reg-sub
  ::created
  :<- [::profile]
  (fn [db]
    (::profile/created db)))

(reg-sub
  ::owner?
  :<- [::profile]
  :<- [:user/sub-db]
  (fn [[db user] _]
    (= (::profile/id db) (:wh.user.db/id user))))

(reg-sub
  ::cv-link
  :<- [::cv-data]
  (fn [cv _]
    (:cv-link cv)))

(reg-sub
  ::error-message
  :<- [::error-fields]
  (fn [errors [_ field]]
    (when (contains? errors field)
      (condp = field
        :email "Please enter a valid email address."
        "This field is incorrect."))))

(reg-sub
  ::articles-count
  :<- [::contributions]
  (fn [contributions]
    (count contributions)))

(reg-sub
  ::issues-count
  :<- [::issues]
  (fn [issues]
    (count issues)))

(reg-sub
  ::last-seen
  :<- [::profile]
  (fn [profile _]
    (::profile/last-seen profile)))

(reg-sub
  ::updated
  :<- [::profile]
  (fn [profile _]
    (::profile/updated profile)))

(reg-sub
  ::id
  (fn [db _]
    (user/id db)))


;; magic number. I'm not going to try and show data only from 4 months everytime.
;; 18 seems like a safe, nice number of weeks
(def weeks-count 18)

(reg-sub
  ::contributions-collection
  :<- [::profile]
  (fn [profile _]
    (get profile ::profile/contributions-collection)))

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
  (-> week first :date js/Date.
      (.toLocaleString "en-us" #js {:month "short"})))

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
