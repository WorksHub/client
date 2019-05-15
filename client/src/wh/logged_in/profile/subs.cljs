(ns wh.logged-in.profile.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [wh.common.data :as data]
    [wh.common.specs.primitives]
    [wh.common.url :as url]
    [wh.logged-in.profile.db :as profile]
    [wh.subs :refer [with-unspecified-option]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::profile
  (fn [db _]
    (::profile/sub-db db)))

(defn header-data
  [data contributions]
  (-> data
      (select-keys [:image-url :name :skills :other-urls :summary])
      (update :other-urls url/detect-urls-type)
      (assoc :contributions contributions)))

(reg-sub
  ::header-data
  :<- [::profile]
  (fn [profile _]
    (header-data
     (util/strip-ns-from-map-keys profile)
     (::profile/contributions profile))))

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

(defn nil-if-blank [s]
  (when-not (str/blank? s)
    s))

(defn salary-string [{:keys [min currency time-period]}]
  (let [min (nil-if-blank min)
        time-period (when time-period (str/lower-case time-period))]
    (if min
      (<< "Minimum ~{min} ~{currency} ~{time-period}")
      "Unspecified")))

(defn visa-status-string
  [statuses other]
  (let [have-other? (contains? statuses "Other")
        otherless (disj statuses "Other")]
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
      (select-keys [:email :job-seeking-status :company-perks :salary :visa-status :current-location :preferred-locations :remote :role-types])))

(reg-sub
  ::private-data
  :<- [::profile]
  (fn [profile _]
    (private-data (util/strip-ns-from-map-keys profile))))

(reg-sub
  ::company-data
  :<- [::profile]
  (fn [profile _]
    (-> profile
        util/strip-ns-from-map-keys
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
  ::email
  :<- [::private-data]
  (fn [private _]
    (:email private)))

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
  {:cv-url      (nil-if-blank (:url file)),
   :cv-filename (nil-if-blank (:name file)),
   :cv-link     (nil-if-blank link)})

(reg-sub
  ::cv-data
  :<- [::profile]
  (fn [profile _]
    (cv-data (util/strip-ns-from-map-keys profile))))

(reg-sub
  ::remote
  :<- [::profile]
  (fn [db]
    (::profile/remote db)))

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
