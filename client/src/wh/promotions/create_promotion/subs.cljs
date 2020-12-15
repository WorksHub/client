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

(reg-sub-raw
  ::job
  (fn [_ _]
    (reaction
      (let [id (<sub [::object-id])]
        (-> (<sub [:graphql/result :job {:id id}])
            (:job)
            (set/rename-keys
              {:company :job-company})
            (update
              :job-company
              (fn [company]
                (set/rename-keys company {:logo :image-url})))
            (job/translate-job))))))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
      (let [id (<sub [::object-id])]
        (-> (<sub [:graphql/result :company {:id id}])
            (:company)
            (update :size keyword))))))

(reg-sub-raw
  ::issue
  (fn [_ _]
    (reaction
      (let [id (<sub [::object-id])]
        (-> (<sub [:graphql/result :issue {:id id}])
            (:issue)
            (set/rename-keys
              {:company :issue-company})
            (update
              :issue-company
              (fn [company]
                (set/rename-keys company {:logo :image-url}))))))))

(reg-sub-raw
  ::article
  (fn [_ _]
    (reaction
      (let [id (<sub [::object-id])]
        (-> (<sub [:graphql/result :blog {:id id}])
            (:blog)
            (blog/translate-blog))))))

(reg-sub-raw
  ::object
  (fn [_ _]
    (reaction
      ;; create qualified keyword from object type: #{::issue ::job ::company ::article}
      (<sub [(keyword :wh.promotions.create-promotion.subs (<sub [::object-type]))]))))

(reg-sub
  ::promoter
  :<- [:user/sub-db]
  (fn [{:keys [wh.user.db/name wh.user.db/image-url] :as user} _]
    {:image-url image-url
     :name      name}))

(reg-sub
  ::description
  (fn [{:keys [::db/description] :as db} _]
    description))
