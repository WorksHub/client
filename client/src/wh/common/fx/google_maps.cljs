(ns wh.common.fx.google-maps
  (:require [re-frame.core :refer [reg-fx reg-event-fx reg-event-db dispatch]]))

(defn autocomplete-service []
  (js/google.maps.places.AutocompleteService.))

(defn places-service [map-or-element]
  (let [map-or-element (or map-or-element (js/document.createElement "div"))]
    (js/google.maps.places.PlacesService. map-or-element)))

(defn geocoder-service []
  (js/google.maps.Geocoder.))

(defn latlng [{:keys [latitude longitude]}]
  (js/google.maps.LatLng. latitude longitude))

(defn unlatlng [^js/google.maps.LatLng x]
  {:latitude (.lat x), :longitude (.lng x)})

(defn- exec-service
  [service method request on-success on-failure]
  (let [callback (fn [response status]
                   (let [response (js->clj response :keywordize-keys true)]
                     (if-not (#{"OK" "ZERO_RESULTS"} status)
                       (dispatch (into on-failure [response status]))
                       (dispatch (into on-success [response])))))]
    (.call (aget service method) service (clj->js request) callback)))

(defn place-predictions [{:keys [input type on-success on-failure]}]
  (exec-service (autocomplete-service)
                "getQueryPredictions"
                {:input input, :type type}
                on-success
                on-failure))

(defn place-details [{:keys [map-or-element place-id on-success on-failure]}]
  (let [place-ids (if (string? place-id) [place-id] place-id)]
    (doseq [place-id place-ids]
      (exec-service (places-service map-or-element)
                    "getDetails"
                    {:placeId place-id}
                    on-success
                    on-failure))))

(defn nearby-search [{:keys [location radius type map-or-element on-success on-failure]}]
  (exec-service (places-service map-or-element)
                "nearbySearch"
                {:location location, :radius radius, :type type}
                on-success
                on-failure))

(defn geocode [{:keys [location on-success on-failure]}]
  (exec-service (geocoder-service)
                "geocode"
                {:location location}
                on-success
                on-failure))

(reg-fx :google/place-predictions place-predictions)
(reg-fx :google/place-details place-details)
(reg-fx :google/nearby-search nearby-search)
(reg-fx :google/geocode geocode)

(defn load-maps [callback]
  (when-not (js/document.getElementById "googleMapsLoader")
    (let [script (js/document.createElement "script")]
      (aset script "type" "text/javascript")
      (aset script "async" true)
      (aset script "src" (str (aget js/window "googleMapsURL") "&callback=googleMapsLoaded"))
      (aset script "id" "googleMapsLoader")
      (aset js/window "googleMapsLoaded" callback)
      (js/document.body.appendChild script))))

(reg-fx :google/load-maps load-maps)

(reg-event-fx
  :google/load-maps
  (fn [{db :db} _]
    {:google/load-maps #(dispatch [:google/maps-loaded])}))

(reg-event-db
  :google/maps-loaded
  (fn [db _]
    (assoc db :google/maps-loaded? true)))
