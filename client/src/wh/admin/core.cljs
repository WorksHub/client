(ns wh.admin.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
            [reagent.core :as reagent]
            [wh.admin.articles.events :as articles-events]
            [wh.admin.articles.views :as articles]
            [wh.admin.candidates.events :as candidates-events]
            [wh.admin.candidates.views :as candidates]
            [wh.admin.companies.views :as companies]
            [wh.admin.create-candidate.events :as create-candidate-events]
            [wh.admin.create-candidate.views :as create-candidate]
            [wh.admin.create-offer.events :as create-offer-events]
            [wh.admin.create-offer.views :as create-offer]
            [wh.admin.tags.views :as tags]
            [wh.db :as db]
            [wh.user.db :as user]))

(def page-mapping
  {:candidates           {:page candidates/page, :can-access? user/admin?}
   :create-candidate     {:page create-candidate/page, :can-access? user/admin?}
   :admin-companies      {:page companies/page, :can-access? user/admin?}
   :admin-articles       {:page articles/page, :can-access? user/admin?}
   :create-company-offer {:page create-offer/page, :can-access? user/admin?}
   :tags-edit            {:page tags/page, :can-access? user/admin?}})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [::candidates-events/initialize-db])
(dispatch-sync [::create-offer-events/initialize-db])
(dispatch-sync [::articles-events/initialize-db])

(db/redefine-app-db-spec!)

(loader/set-loaded! :admin)
