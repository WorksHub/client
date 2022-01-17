(ns wh.search.core
  (:require [cljs.loader :as loader]
            [re-frame.core :refer [dispatch-sync reg-event-db]]
            [wh.db :as db]
            [wh.search.views :as search-views]))

(def page-mapping
  {:search search-views/search-page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(dispatch-sync [::initialize-page-mapping])

(loader/set-loaded! :search)
