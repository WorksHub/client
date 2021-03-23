(ns wh.profile.db
  (:require [clojure.string :as str]
            [wh.common.text :as text])
  (#?(:clj :require :cljs :require-macros)
   [clojure.core.strint :refer [<<]]))

(def maximum-skills 6)

(defn state->str
  [s]
  (case s
    "get_in_touch" "Interviewing"
    "approved"     "Pending"
    "rejected"     "Rejected by WorksHub"
    (str/capitalize s)))

(def application-state
  {:get-in-touch "get_in_touch"
   :pass "pass"
   :hired "hired"
   :pending "pending"
   :approved "approved"
   :rejected "rejected"})

(def application-action
  {:get-in-touch "get_in_touch"
   :pass "pass"
   :hire "hire"})

;; ------------

(def updating-user-status-key :updating-status)

(defn updating-status [db]
  (get db updating-user-status-key))

(defn set-updating-status [db status]
  (assoc db updating-user-status-key (if (= "approved" status)
                                       :approving
                                       :rejecting)))

(defn unset-updating-status [db]
  (assoc db updating-user-status-key nil))

;; ------------

(defn start-updating-application-state [db]
  (assoc db :setting-application-state? true))

(defn finish-updating-application-state [db]
  (assoc db :setting-application-state? false))

(defn updating-application-state? [db]
  (boolean (get db :setting-application-state?)))

;; ---------------------

(def path [:application-user-info :open?])

(defn close-modal [db]
  (assoc-in db path false))

(defn open-modal [db]
  (assoc-in db path true))

(defn modal-opened? [db]
  (boolean (get-in db path)))

;; ----------------------

(defn salary-string [{:keys [min currency time-period]}]
  (let [min         (text/not-blank min)
        time-period (when time-period (str/lower-case time-period))]
    (if min
      (<< "Minimum ~{min} ~{currency} ~{time-period}")
      "Unspecified")))

(defn visa-status-string
  [statuses other]
  (let [have-other? (some #(= % "Other") statuses)
        otherless   (remove #(= % "Other") statuses)]
    (if (seq statuses)
      (str/join ", " (cond-> otherless have-other? (conj other)))
      "No statuses selected")))

(defn location-label [loc]
  (when loc
    (<< "~(:city loc), ~(:country loc)")))

(defn preferred-location-strings
  [locations]
  (mapv location-label locations))

(defn format-profile [profile]
  (-> profile
      (update :salary salary-string)
      (update :visa-status visa-status-string (:visa-status-other profile))
      (update :company-perks (partial mapv :name))
      (update :current-location location-label)
      (update :preferred-locations preferred-location-strings)
      (update :remote boolean)
      (select-keys [:email :job-seeking-status :company-perks :salary :visa-status :current-location :preferred-locations :remote :role-types :phone])))

(def cv-path
  [:cv-visible])

(defn close-cv [db]
  (assoc-in db cv-path false))

(defn open-cv [db]
  (assoc-in db cv-path true))

(defn cv-visible? [db]
  (boolean (get-in db cv-path)))

(def default-db
  (open-cv {}))
