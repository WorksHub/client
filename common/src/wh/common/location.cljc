(ns wh.common.location
  (:require
    #?(:cljs [wh.common.fx.google-maps :as google-maps])
    [camel-snake-kebab.core :as c]
    [clojure.string :as str]
    [wh.common.data :as data]))

(defn format-location
  [{:keys [city state country country-code]}]
  (cond
    (and (not (str/blank? city))
         country-code
         (= "us" (str/lower-case country-code))) (str/join ", " (remove str/blank? [city state country-code]))
    (not (str/blank? city)) (str city ", " country)
    (and (str/blank? city)
         (not (str/blank? state))) (str state ", " country)
    (every? str/blank? [city state]) country))

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

#?(:cljs
   (defn google-place->location
     [{:keys [place_id address_components geometry]}]
     (let [{:keys [route locality postal-town postal-code street-number
                   country country-code state-code]}
           (extract-address-components address_components)
           {:keys [latitude longitude] :as geolocation}
           (google-maps/unlatlng (:location geometry))]
       (merge {:street       (str/trim (str street-number " " route))
               :city         (or (match-city locality) (match-city postal-town) locality postal-town)
               :country      (get data/country-code->country country-code country)
               :country-code country-code
               :post-code    postal-code
               :latitude     latitude
               :longitude    longitude}
              (when (= country-code "US")
                {:state state-code})))))
