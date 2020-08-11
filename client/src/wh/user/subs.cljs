(ns wh.user.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.data :refer [super-admins]]
            [wh.common.data :as data]
            [wh.db :as db]
            [wh.user.db :as user]))

(reg-sub ::user
         (fn [db _]
           (get-in db [::user/sub-db])))

(reg-sub ::all-visa-statuses
         (constantly data/visa-options))

(reg-sub ::all-currencies
         (constantly data/currencies))

;; FIXME: remove me
(reg-sub ::type
         (fn [db _]
           (get-in db [::user/sub-db ::user/type])))

(reg-sub :wh.user/super-admin?
         :<- [:user/admin?]
         :<- [:user/email]
         (fn [[admin? email] _]
           (and admin?
                (some (partial = email) super-admins))))

(reg-sub :wh.user/workshub?
         :<- [::user]
         :<- [:user/admin?]
         (fn [[user admin?]]
           (or admin? (= (:wh.user.db/company-id user) "workshub-f0774"))))

(reg-sub ::show-consent-popup?
         :<- [::user]
         :<- [:user/logged-in?]
         :<- [:wh.pages.core/page] ; we do not want to show it in registration
         (fn [[user logged-in? page] _]
           (and (not (::user/consented user))
                (not (contains? #{:register :homepage-not-logged-in} page))
                ;; The seemingly contradictory situation where page is `homepage-not-logged-in` and
                ;; we ARE logged in can happen immediately after swapping back app-db from persistent
                ;; state in github-callback.
                logged-in?)))

(reg-sub ::name
         (fn [db _]
           (get-in db [::user/sub-db ::user/name])))

(reg-sub ::skills-names
         (fn [db _]
           (mapv :name (get-in db [::user/sub-db ::user/skills]))))

(reg-sub ::visa-status
         (fn [db _]
           (get-in db [::user/sub-db ::user/visa-status])))

(reg-sub ::visa-status-other
         (fn [db _]
           (get-in db [::user/sub-db ::user/visa-status-other])))

(reg-sub ::min-salary
         (fn [db _]
           (get-in db [::user/sub-db ::user/salary :min])))

(reg-sub ::saving-consent?
         :<- [::user]
         (fn [user _]
           (::user/saving-consent? user)))

(reg-sub ::save-consent-error?
         :<- [::user]
         (fn [user _]
           (::user/save-consent-error? user)))

(reg-sub ::company
         :<- [::user]
         (fn [user _]
           (::user/company user)))

(reg-sub :wh.user/can-use-integrations?
         :<- [::company]
         (fn [company _]
           (contains? (:permissions company) :can_use_integrations)))

(reg-sub :wh.user/can-add-users?
         :<- [::company]
         (fn [company _]
           (contains? (:permissions company) :can_add_user)))

(reg-sub ::onboarding-msg-not-seen?
         (fn [db [_ msg]]
           (user/onboarding-msg-not-seen? db msg)))

(reg-sub ::company-onboarding-msg-not-seen?
         (fn [db [_ msg]]
           (user/company-onboarding-msg-not-seen? db msg)))

(reg-sub ::cover-letter
         (fn [db _]
           (get-in db [::user/sub-db ::user/cover-letter])))
