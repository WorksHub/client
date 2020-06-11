(ns wh.activities.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.re-frame.subs :refer [<sub]]))

(defn normalize-activity
  [{:keys [feed-company feed-issue feed-job feed-blog] :as activity}]
  (-> activity
      (merge
        (cond
          feed-job     {:object      feed-job
                        :object-type "job"}
          feed-blog    {:object      feed-blog
                        :object-type "article"}
          feed-issue   {:object      feed-issue
                        :object-type "issue"}
          feed-company {:object      feed-company
                        :object-type "company"}
          :else        {}))
      (update :actor :id)))

(reg-sub
  ::activities
  (fn [_ _]
    (let [activities (get-in (<sub [:graphql/result :all_activities {}])
                             [:query-activities :activities])
          activities (map normalize-activity activities)]
      activities)))
