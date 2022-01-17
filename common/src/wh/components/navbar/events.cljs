(ns wh.components.navbar.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [wh.components.navbar.db :as db]))

(reg-event-db
  ::set-search-value
  (fn [db [_ value]]
    (assoc db ::db/search-value value)))

(reg-event-fx
  :wh.search/search-with-value
  (fn [{db :db} [_ value]]
    (when (seq value)
      {:db       (-> db
                     (assoc ::db/search-value value)
                     (assoc-in [:wh.search/data] {}))
       :navigate [:search :params {:query value}]})))
