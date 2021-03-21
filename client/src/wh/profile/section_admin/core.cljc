(ns wh.profile.section-admin.core
  (:require
    #?(:cljs [wh.profile.section-admin.events :as events])
    #?(:cljs [wh.profile.section-admin.views :as section-admin])
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.common.user :as user-common]
    [wh.profile.section-admin.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn section-for-admin []
  #?(:cljs (let [profile              (<sub [::subs/profile])
                 profile-loaded?      (<sub [::subs/profile-loaded?])
                 profile-type         (:type profile)
                 admin-view?          (<sub [::subs/admin-view?])
                 set-approval-status  #(dispatch [:wh.profile.section-admin.events/set-approval-status %])
                 delete-user          #(dispatch [:wh.profile.section-admin.events/delete-user %])
                 fetch-profile        [:wh.logged-in.profile.events/fetch-initial-data]
                 show-admin-controls? (and profile-loaded?
                                           admin-view?
                                           (not (user-common/admin-type? profile-type)))]
             (when show-admin-controls?
               [section-admin/controls
                {:user-id         (<sub [::subs/user-id])
                 :hs-url          (<sub [::subs/hs-url])
                 :applications    (<sub [::subs/applications])
                 :liked-jobs      (<sub [::subs/liked-jobs])
                 :approval-info   (<sub [::subs/approval-info])
                 :on-approve      #(set-approval-status {:id            (:id profile)
                                                         :status        "approved"
                                                         :fetch-profile fetch-profile})
                 :on-reject       #(set-approval-status {:id            (:id profile)
                                                         :status        "rejected"
                                                         :fetch-profile fetch-profile})
                 :on-delete       delete-user
                 :updating-status (<sub [::subs/updating-status])}]))))

(defn toggle-view
  "Toggle the look of a profile so admin can see the it as a guest"
  []
  #?(:cljs (when (<sub [:user/admin?])
             [section-admin/toggle-view
              (<sub [::subs/profile])
              (<sub [::subs/admin-view?])])))
