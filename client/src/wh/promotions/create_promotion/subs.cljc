(ns wh.promotions.create-promotion.subs
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-sub reg-sub-raw]]
            [wh.common.blog :as blog]
            [wh.common.job :as job]
            [wh.common.subs]
            [wh.promotions.create-promotion.db :as db]
            [wh.re-frame.subs :refer [<sub]])
  (:require-macros [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::object-type
  :<- [:wh/page-param :type]
  (fn [type _] type))

(reg-sub
  ::object-id
  :<- [:wh/page-param :id]
  (fn [id _] id))


(defn- object-state
  "Takes object and condition that determines whether object should be blocked
  from promoting (when it's unpublished, or closed), and returns object,
  or information why object is not given, or nil when object is totally absent."
  [obj not-allowed]
  (cond
    (nil? obj)  nil
    not-allowed :not-allowed
    (map? obj)  obj
    :else       :not-recognized))

(defn- prepare-job-preview
  "Prepare job data structure to be previewed safely on feed and jobsboard"
  [job]
  (->> (set/rename-keys (:company job) {:logo :image-url})
       (assoc job :job-company)))

(reg-sub-raw
  ::job
  (fn [_ _]
    (reaction
      (let [id  (<sub [::object-id])
            ;; use some-> to make sure data is here, and we do not try to process absent data
            job (some-> (<sub [:graphql/result :job {:id id}])
                        (:job)
                        (job/translate-job)
                        (prepare-job-preview))]
        (object-state job (not (:published job)))))))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [id      (<sub [::object-id])
            company (some-> (<sub [:graphql/result :company {:id id}])
                            (:company)
                            (update :size keyword))]
        (object-state company (not (:profile-enabled company)))))))

(reg-sub-raw
  ::issue
  (fn [_ _]
    (reaction
      (let [id    (<sub [::object-id])
            issue (some-> (<sub [:graphql/result :issue {:id id}])
                          (:issue)
                          (set/rename-keys
                            {:company :issue-company})
                          (update
                            :issue-company
                            (fn [company]
                              (set/rename-keys company {:logo :image-url}))))]
        (object-state issue (= "closed" (:status issue)))))))

(reg-sub-raw
  ::article
  (fn [_ _]
    (reaction
      (let [id   (<sub [::object-id])
            blog (some-> (<sub [:graphql/result :blog {:id id}])
                         (:blog)
                         (blog/translate-blog))]
        (object-state blog (not (:published blog)))))))

(reg-sub-raw
  ::object
  (fn [_ _]
    (reaction
      ;; create qualified keyword from object type: #{::issue ::job ::company ::article}
      (<sub [(keyword :wh.promotions.create-promotion.subs (<sub [::object-type]))]))))

(reg-sub
  ::promoter
  :<- [:user/sub-db]
  (fn [{:keys [wh.user.db/name wh.user.db/image-url wh.user.db/id] :as user} _]
    {:image-url image-url
     :name      name
     :id        id}))

(reg-sub
  ::description
  (fn [{:keys [::db/description] :as db} _]
    description))

(reg-sub-raw
  ::can-publish?
  (fn [_ [_ channel]]
    (reaction
      (let [status      (<sub [::send-promotion-status channel])
            object      (<sub [::object])
            description (<sub [::description])]
        (and (map? object)
             (not (#{:sending :success} status))
             (or (not= channel :feed)
                 (> (count description) 3)))))))

(reg-sub
  ::send-promotion-status
  (fn [db [_ channel]]
    (get-in db [::db/promotion-status channel])))

(reg-sub
  ::selected-channel
  (fn [db [_ channel]]
    (get db ::db/selected-channel :feed)))
