(ns wh.search.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch-sync reg-event-db]]
            [wh.common.user :as user-common]
            [wh.db :as db]
            [wh.search.views :as search-views]))

(def page-mapping
  ;; TODO: once universal search is ready to go public, remove :can-access?
  ;; condition [4683]
  {:search {:page search-views/search-page, :can-access? user-common/admin?}})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [:wh.search.events/fetch-data])

(loader/set-loaded! :search)
