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
   :manage-repository-issues {:page manage-issues/issues-page :can-access? user/company?}
   :issue issue/page
   :issues issues/page
   :issues-by-language issues/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::edit-issue/initialize-db])
(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [:wh.pages.core/unset-loader])

;; load extra symbols
(let [symbol-filename "symbols/issues.svg"]
  (when-not (.getElementById js/document (str "load-icons-" symbol-filename))
    (js/loadSymbols symbol-filename)))

(loader/set-loaded! :issues)
