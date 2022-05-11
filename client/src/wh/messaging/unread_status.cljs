(ns wh.messaging.unread-status
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx reg-sub]]
    [wh.db :as db]
    [wh.user.db :as user-db])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

;; event handlers

(defquery unread-status
  {:venia/operation {:operation/type :query
                     :operation/name "unread_status"}
   :venia/queries [[:me [[:conversationsUnreadStatus [:total]]]]]})

(reg-event-fx
  ::fetch-unread-status
  db/default-interceptors
  (fn [_ _]
    {:graphql {:query unread-status
               :on-success [::fetch-unread-status-success]}}))

(reg-event-db
  ::fetch-unread-status-success
  db/default-interceptors
  (fn [db [resp]]
    (assoc-in db [::user-db/sub-db ::user-db/conversations-unread-status]
              (get-in resp [:data :me :conversationsUnreadStatus]))))

;; subs

(reg-sub
  ::unread-conversations-count
  (fn [db _]
    (get-in db [::user-db/sub-db ::user-db/conversations-unread-status :total] 0)))
