(ns wh.profile.db)

(def maximum-skills 6)

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