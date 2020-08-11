(ns wh.logged-in.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch-sync reg-event-db]]
            [wh.common.user :as user-common]
            [wh.db :as db]
            [wh.landing-new.events]
            [wh.landing-new.views :as landing]
            [wh.logged-in.apply.db :as apply-db]
            [wh.logged-in.apply.events :as apply-events]
            ;; We need to req this ns to add overlay-apply to extra-overlays atom.
            ;; I really do not like this construct.
            [wh.logged-in.apply.views]
            [wh.logged-in.contribute.views :as contribute]
            [wh.logged-in.notifications.settings.views :as notif-settings]
            [wh.logged-in.personalised-jobs.views :as personalised-jobs]
            [wh.logged-in.profile.db :as profile-db]
            [wh.logged-in.profile.events :as profile-events]
            [wh.logged-in.profile.views :as profile]))

(def page-mapping
  {:homepage-dashboard        landing/page
   :liked                     (partial personalised-jobs/page :liked)
   :recommended               (partial personalised-jobs/page :recommended)
   :applied                   (partial personalised-jobs/page :applied)
   :contribute                {:page contribute/page :can-access? db/logged-in?}
   :contribute-edit           {:page contribute/page :can-access? db/logged-in?}
   :candidate-edit-header     {:page profile/header-edit-page :can-access? user-common/admin?}
   :candidate-edit-cv         {:page profile/cv-edit-page :can-access? user-common/admin?}
   :candidate-edit-private    {:page profile/private-edit-page :can-access? user-common/admin?}
   :improve-recommendations   {:page profile/improve-recommendations-page :can-access? db/logged-in?}
   :notifications-settings    {:page notif-settings/page :can-access? db/logged-in?}
   :profile                   {:page profile/view-page :can-access? db/logged-in?}
   :profile-edit-header       {:page profile/header-edit-page :can-access? db/logged-in?}
   :profile-edit-cv           {:page profile/cv-edit-page :can-access? db/logged-in?}
   :profile-edit-private      {:page profile/private-edit-page :can-access? db/logged-in?}
   :profile-edit-company-user {:page profile/company-user-edit-page :can-access? db/logged-in?}})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj ::apply-db/sub-db ::profile-db/sub-db)

(dispatch-sync [::apply-events/initialize-db])
(dispatch-sync [::profile-events/initialize-db])
(dispatch-sync [::initialize-page-mapping])

(db/redefine-app-db-spec!)

(loader/set-loaded! :logged-in)
