(ns wh.jobsboard.events
  (:require [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
            [wh.db :as db]))

(reg-event-fx
  ::on-location-select
  db/default-interceptors
  (fn [_ [{:keys [attr value]}]]
    {:dispatch (cond
                 (= attr "location.region")       [:wh.search/toggle-region value]
                 (= attr "location.city")         [:wh.search/toggle-city value]
                 (= attr "location.country-code") [:wh.search/toggle-country value])}))

(reg-event-fx
  ::on-tag-select
  db/default-interceptors
  (fn [_ [{:keys [value]}]]
    {:dispatch [:wh.search/search-by-tag value true]}))
