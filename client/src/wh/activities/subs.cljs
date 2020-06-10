(ns wh.activities.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.re-frame.subs :refer [<sub]]))

(defn normalize-activity
  [{:keys [issue job blog] :as activity}]
  (-> activity
      (merge
        (cond
          job   {:object      job
                 :object-type "job"}
          blog  {:object      blog
                 :object-type "article"}
          issue {:object      issue
                 :object-type "issue"}
          :else {}))
      (update :actor :id)))

(reg-sub
  ::activities
  (fn [_ _]
    (let [activities (get-in (<sub [:graphql/result :all_activities {}])
                             [:query-activities :activities])
          activities (map normalize-activity activities)]
      activities)))
