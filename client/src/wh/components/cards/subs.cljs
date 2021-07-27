(ns wh.components.cards.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.blog :as common-blog]
            [wh.common.user :as user-common]
            [wh.user.db :as user-db]))

(reg-sub
  :job-card/show-closed?
  :<- [:user/admin?]
  (fn [admin? [_ published?]]
    (and (false? published?) (not admin?))))

(reg-sub
  :job-card/show-unpublished?
  (fn [db [_ published? company-id]]
    (let [admin? (user-common/admin? db)
          owner? (and company-id (= company-id (get-in db [::user-db/sub-db ::user-db/company-id])))]
      ;; we can't use wh.job.db/show-unpublished? since it may not be loaded
      (and (false? published?) (or admin? owner?)))))

(reg-sub
 :blog-card/show-unpublished?
 :<- [:user/admin?]
 :<- [:user/id]
 (fn [[admin? user-id] [_ creator-id published]]
   (common-blog/show-unpublished? {:admin?     admin?
                                   :published? published
                                   :creator-id creator-id
                                   :user-id    user-id})))

(reg-sub
 :blog-card/can-edit?
 :<- [:user/admin?]
 :<- [:user/id]
 (fn [[admin? user-id] [_ creator-id published]]
   (common-blog/can-edit? {:admin?     admin?
                           :published? published
                           :creator-id creator-id
                           :user-id    user-id})))
