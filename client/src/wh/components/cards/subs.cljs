(ns wh.components.cards.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [wh.user.db :as user-db]))

(reg-sub
  :job-card/show-closed?
  :<- [:user/admin?]
  (fn [admin? [_ published?]]
    (and (false? published?) (not admin?))))

(reg-sub
  :job-card/show-unpublished?
  (fn [db [_ published? company-id]]
    (let [admin? (user-db/admin? db)
          owner? (and company-id (= company-id (get-in db [::user-db/sub-db ::user-db/company-id])))]
      ;; we can't use wh.job.db/show-unpublished? since it may not be loaded
      (and (false? published?) (or admin? owner?)))))

(defn show-blog-unpublished? [admin? creator user-email published?]
  (and (and (not (nil? published?)) (not published?))
       (or admin?
           (and user-email (= creator user-email)))))

(defn can-edit-blog? [admin? creator user-email published?]
  (or admin?
      (and (and creator (= creator user-email))
           (and (not (nil? published?)) (not published?)))))

(reg-sub
  :blog-card/show-unpublished?
  :<- [:user/admin?]
  :<- [:user/email]
  (fn [[admin? email] [_ creator published]]
    (show-blog-unpublished? admin? creator email published)))

(reg-sub
  :blog-card/can-edit?
  :<- [:user/admin?]
  :<- [:user/email]
  (fn [[admin? email] [_ creator published]]
    (can-edit-blog? admin? creator email published)))
