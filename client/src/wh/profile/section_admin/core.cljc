(ns wh.profile.section-admin.core
  (:require
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    #?(:cljs [wh.profile.section-admin.views :as section-admin])
    [wh.common.user :as user-common]
    [wh.profile.section-admin.subs :as subs]
    #?(:cljs [wh.profile.section-admin.events :as events])
    [wh.re-frame.subs :refer [<sub]]))

(defn section-for-admin []
  #?(:cljs (let [profile (<sub [::subs/profile])
                 profile-loaded? (<sub [::subs/profile-loaded?])
                 profile-type (:type profile)
                 admin-view? (<sub [::subs/admin-view?])
                 set-approval-status #(dispatch [:wh.profile.section-admin.events/set-approval-status %])
                 fetch-profile [:wh.logged-in.profile.events/fetch-initial-data]
                 show-admin-controls? (and profile-loaded?
                                           admin-view?
                                           (not (user-common/admin-type? profile-type)))]
             (when show-admin-controls?
               [section-admin/controls
                {:hs-url        (<sub [::subs/hs-url])
                 :applications  (<sub [::subs/applications])
                 :liked-jobs    (<sub [::subs/liked-jobs])
                 :approval-info (<sub [::subs/approval-info])
                 :on-approve    #(set-approval-status {:id     (:id profile)
                                                       :status "approved"
                                                       :fetch-profile fetch-profile})
                 :on-reject #(set-approval-status {:id     (:id profile)
                                                   :status "rejected"
                                                   :fetch-profile fetch-profile})
                 :updating-status (<sub [::subs/updating-status])}]))))

(defn toggle-view
  "Toggle the look of a profile so admin can see the it as a guest"
  []
  #?(:cljs (when (<sub [:user/admin?])
             [section-admin/toggle-view
              (<sub [::subs/profile])
              (<sub [::subs/admin-view?])])))