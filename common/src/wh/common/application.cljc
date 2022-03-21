(ns wh.common.application)

(def state-hired :hired)
(def state-approved :approved)

(defn state [a] (:state a))

(defn state? [s application]
  (boolean (some-> application
                   state
                   keyword
                   (= s))))

(def approved? (partial state? state-approved))
(def hired? (partial state? state-hired))

(defn hubspot-deal-id [a] (get-in a [:hubspot :id]))
(defn has-conversation? [a] (boolean (:conversation-id a)))

