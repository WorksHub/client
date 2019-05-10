(ns wh.issues.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.db :as db]
    [wh.issue.edit.events :as edit-issue]
    [wh.issue.views :as issue]
    [wh.issues.manage.views :as manage-issues]
    [wh.issues.views :as issues]
    [wh.user.db :as user]))

(def page-mapping
  {:company-issues {:page issues/page :can-access? user/company?}
   :manage-issues {:page manage-issues/page :can-access? user/company?}
   :issue issue/page
   :issues issues/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch [::edit-issue/initialize-db])
(dispatch-sync [::initialize-page-mapping])

;; load extra issue symbols
(js/loadSymbols "symbols/issues.svg")

(loader/set-loaded! :issues)
