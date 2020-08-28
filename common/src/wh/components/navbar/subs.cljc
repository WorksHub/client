(ns wh.components.navbar.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.user :as user-common]
            [wh.components.navbar.db :as db]))

(reg-sub
  ::profile-image
  :<- [:user/sub-db]
  (fn [{:keys [wh.user.db/type wh.user.db/image-url wh.user.db/company] :as user} _]
    (if (user-common/company-type? type) (get company :logo) image-url)))

(reg-sub
  ::company-slug
  :<- [:user/sub-db]
  (fn [user _]
    (get-in user [:wh.user.db/company :slug])))

(reg-sub
  ::search-value
  (fn [db _]
    (db ::db/search-value)))
