(ns wh.components.navbar.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  ::user-image
  :<- [:user/sub-db]
  (fn [user _]
    (:wh.user.db/image-url user)))
