(ns wh.company.common
  (:require
    [ajax.json :as ajax-json]
    [camel-snake-kebab.core :as c]
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.common.fx.google-maps :as google-maps]))

(defn get-company-suggestions
  [n success]
  {:method          :get
   :uri             "https://autocomplete.clearbit.com/v1/companies/suggest"
   :params          {:query n}
   :response-format (ajax-json/json-response-format {:keywords? true})
   :timeout         10000
   :on-success      [success]})

(defn extract-address-components [components]
  (->> (for [component components type (:types component)]
         [[(c/->kebab-case-keyword type) (:long_name component)]
          (when (= type "country")
            [:country-code (:short_name component)])
          (when (= type "administrative_area_level_1")
            [:state-code (:short_name component)])])
       (apply concat)
       (remove nil?)
       (into {})))

(defn match-city [new-city]
  (when new-city
    (->> data/cities
         (filter (fn [city] (str/includes? new-city city)))
         first)))

(defn google-place->location
  [{:keys [place_id address_components geometry]}]
  (let [{:keys [route locality postal-town postal-code street-number
                country country-code state-code]}
        (extract-address-components address_components)
        {:keys [latitude longitude] :as geolocation}
        (google-maps/unlatlng (:location geometry))]
    {:street (str/trim (str street-number " " route))
     :city (or (match-city locality) (match-city postal-town) locality postal-town)
     :country (get data/country-code->country country-code country)
     :country-code country-code
     :state (if (= country-code "US")
              state-code
              "")
     :post-code postal-code
     :latitude latitude
     :longitude longitude}))
