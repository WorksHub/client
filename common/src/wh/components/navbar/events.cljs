(ns wh.components.navbar.events
  (:require [re-frame.core :refer [reg-fx reg-event-db reg-event-fx]]
            [wh.components.navbar.db :as db]
            [wh.pages.core :as pages]))

(defn submit-search
  [value]
  (pages/navigate [:search :query-params {:query value}]))

(reg-event-db
  ::set-search-value
  (fn [db [_ value]]
    (assoc db ::db/search-value value)))

(reg-event-fx
  :wh.search/search-with-value
  (fn [{db :db} [_ value]]
    {:db               (-> db
                           (assoc ::db/search-value value)
                           (assoc-in [:wh.search/data] {}))
     :universal-search value}))

(reg-fx
  :universal-search
  (fn [value]
    (when-not (empty? value)
      (submit-search value))))
