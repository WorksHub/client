(ns wh.company.profile.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [reagent.core :as reagent]
    [wh.company.listing.db :as listing-db]
    [wh.company.listing.events :as listing-events]
    [wh.company.listing.views :as listing]
    [wh.company.profile.db :as profile-db]
    [wh.company.profile.events :as profile-events]
    [wh.company.profile.views :as profile]
    [wh.db :as db]))

(def page-mapping
  {:company profile/page
   :companies listing/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj ::profile-db/sub-db ::listing-db/sub-db)

(dispatch-sync [::initialize-page-mapping])
(dispatch [:wh.pages.core/unset-loader])

(db/redefine-app-db-spec!)

;; load extra symbols
(let [symbol-filename "symbols/company_profile.svg"]
  (when-not (.getElementById js/document (str "load-icons-" symbol-filename))
    (js/loadSymbols symbol-filename)))

(loader/set-loaded! :company-profile)
