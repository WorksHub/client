;;; TODO: This will eventually be refactored. I'm moving it here
;;; for now because of event name clashes with wh.logged-in.profile.events.

(ns wh.logged-in.profile.location-events
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [reg-event-fx]]
            [wh.db :as db]))

(reg-event-fx
  ::search-failure
  db/default-interceptors
  (fn [_ [{:keys [retry-attempt on-failure]
           :as options
           :or {retry-attempt 1}}
          result]]
    (js/console.error "Retry attempt:" retry-attempt)
    (js/console.error "Location search failed:" result)
    {:dispatch (if (> retry-attempt 2)
                 (conj on-failure result)
                 [::search (assoc options :retry-attempt (inc retry-attempt))])}))

(defn parse-location
  [{:keys [administrative country country_code locale_names _geoloc]}]
  (cond-> {:location/city         (first locale_names)
           :location/country      country
           :location/country-code (str/upper-case country_code)}
    (seq administrative) (assoc :location/administrative (first administrative))
    (seq _geoloc) (assoc :location/longitude (:lng _geoloc)
                         :location/latitude  (:lat _geoloc))))

(defn parse-locations
  [{hits :hits}]
  (mapv parse-location hits))

(reg-event-fx
  ::search-success
  db/default-interceptors
  (fn [_ [{:keys [on-success] :as options} result]]
    {:dispatch (conj on-success (parse-locations result))}))

(reg-event-fx
  ::search
  db/default-interceptors
  (fn [_ [{:keys [query retry-attempt]
           :as options}]]
    {:algolia {:index      :places
               :retry-num  retry-attempt
               :params     {:query       query
                            :type        "city"
                            :language    "en"
                            :hitsPerPage 5}
               :on-success [::search-success options]
               :on-failure [::search-failure options]}}))
