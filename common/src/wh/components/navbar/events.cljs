(ns wh.components.navbar.events
  (:require [re-frame.core :refer [reg-fx reg-event-db reg-event-fx dispatch]]
            [wh.components.navbar.db :as db]
            [wh.re-frame.subs :refer [<sub]]
            [wh.components.navbar.subs :as subs]
            [wh.pages.core :as pages]))

(defn submit-search
  [value]
  (pages/navigate [:search :query-params {:query value}]))

(reg-event-db
  ::set-search-value
  (fn [db [_ value]]
    (assoc db ::db/search-value value)))

(reg-event-db
  ::set-search-focus
  (fn [db [_ value]]
    (assoc db ::db/search-focus value)))

(reg-event-db
  ::set-local-search
  (fn [db [_ value]]
    (assoc db ::db/local-search value)))

(reg-event-fx
  :wh.search/search-with-value
  (fn [{db :db} [_ value]]
    (let [local-search (<sub [::subs/local-search])]
      (let [values (distinct (into [] (concat [value] local-search )))]
        (.setItem js/localStorage "local_search" 
          (clojure.string/join "||" values))
          (dispatch [:wh.components.navbar.events/set-local-search values])))
      {:db (-> db
        (assoc ::db/search-value value)
        (assoc-in [:wh.search/data] {})) :universal-search value}))

(reg-fx
  :universal-search
  (fn [value]
    (when-not (empty? value)
      (submit-search value))))
