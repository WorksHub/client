(ns wh.login.shared)

(defn initialize-associated-jobs-and-blogs
  [{:keys [likes applied] :as user}]
  (-> user
      (assoc :liked-jobs (->> likes
                              (filter #(= (:__typename %) "Job"))
                              (map :id)
                              set)
             :liked-blogs (->> likes
                               (filter #(= (:__typename %) "Blog"))
                               (map :id)
                               set)
             :applied-jobs (set (map :jobId applied)))
      (dissoc :likes :applied)))