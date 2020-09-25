(ns wh.company.candidate.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.keywords :as keywords]
            [wh.common.specs.primitives]
            [wh.company.candidate.db :as candidate]
            [wh.db :as db]
            [wh.logged-in.profile.subs :as profile-subs]))

(reg-sub
  ::sub-db
  (fn [db _]
    (get db ::candidate/sub-db)))

(reg-sub
  ::candidate-data
  (fn [db _]
    (keywords/strip-ns-from-map-keys (get db :wh.logged-in.profile.db/sub-db))))

(reg-sub
  ::header-data
  :<- [::candidate-data]
  (fn [candidate _]
    (profile-subs/header-data candidate (:blogs candidate))))

(reg-sub
  ::cv-data
  :<- [::candidate-data]
  (fn [candidate _]
    (profile-subs/cv-data candidate)))

(reg-sub
  ::private-data
  :<- [::candidate-data]
  (fn [candidate _]
    (profile-subs/private-data candidate)))

(defn augment-titles
  [job-titles jobs]
  (for [job jobs]
    {:id job, :title (get job-titles job job)}))

(reg-sub
  ::admin-data
  :<- [::candidate-data]
  (fn [{:keys [company type applied likes hubspot-profile-url]} _]
    {:applied applied
     :likes likes
     :hs-url hubspot-profile-url
     :company company
     :type type}))

(reg-sub
  ::approve-data
  :<- [::candidate-data]
  (fn [candidate _]
    (select-keys candidate [:id :email :approval :updating])))

(reg-sub
  ::application-data
  :<- [::candidate-data]
  (fn [{:keys [applied]} _]
    applied))

(reg-sub
  ::loading-error
  :<- [::sub-db]
  (fn [db _]
    (get db ::candidate/error)))

(reg-sub
  ::show-login?
  (fn [db _]
    (not (db/logged-in? db))))

(reg-sub
  ::get-in-touch-overlay-job-id
  :<- [::sub-db]
  (fn [db _]
    (get db ::candidate/get-in-touch-overlay)))

(reg-sub
  ::show-get-in-touch-overlay?
  :<- [::get-in-touch-overlay-job-id]
  (fn [job-id _]
    (boolean job-id)))

(reg-sub
  ::get-in-touch-overlay-job-data
  :<- [::sub-db]
  :<- [::get-in-touch-overlay-job-id]
  (fn [[db job-id] _]
    (some #(when (= job-id (:id %)) %) (get-in db [::candidate/data :applied]))))

(reg-sub
  ::job-selection-overlay-state
  :<- [::sub-db]
  (fn [db _]
    (::candidate/job-selection-overlay-state db)))

(reg-sub
  ::show-job-selection-overlay?
  :<- [::job-selection-overlay-state]
  (fn [state _]
    (boolean state)))

(reg-sub
  ::job-selection-overlay-data
  :<- [::application-data]
  :<- [::job-selection-overlay-state]
  :<- [:wh.user.subs/company]
  (fn [[application-data state company] _] ;; state == 'verb'
    (->> application-data
         ;; FIXME this only works accidentally because some states are the same as their verbs...
         (filter #(not (or (= state (:state %))
                           (= "hired" (:state %)))))
         (filter (case state
                  "hire" #(= "get_in_touch" (:state %))
                   (constantly true)))
         (map #(assoc % :company-name (:name company))))))

(reg-sub
  ::job-selection-overlay-job-selections
  :<- [::sub-db]
  (fn [db _]
    (::candidate/job-selection-overlay-job-selections db)))

(reg-sub
  ::profile-fields
  :<- [::application-data]
  (fn [application-data _]
    (if (some #(when (or (= "get_in_touch" (:state %))
                         (= "hired" (:state %))) true) application-data)
      #{:visa :preferred-locations :current-location :email}
      #{:visa :preferred-locations :current-location})))

(reg-sub
 ::updating?
 :<- [::sub-db]
 (fn [db _]
   (::candidate/updating? db)))
