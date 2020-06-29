(ns wh.components.activities.company-published
  (:require [wh.components.activities.components :as components]
            [wh.common.specs.company :as company-spec]
            [wh.routes :as routes]))

(defn details [{:keys [name tags slug description size locations] :as company}]
  [components/inner-card
   [:div
    [components/title
     {:href   (routes/path :company :params {:slug slug})
      :margin true}
     name]
    (when (or size (first locations))
      (let [location (first locations)
            location-str (str (:city location) ", " (:country location))]
        [components/meta-row
         (when-let [size-str (company-spec/size->range size)]
           [components/text-with-icon {:icon "couple"} size-str])
         (when location [components/text-with-icon {:icon "pin"} location-str])]))]
   [components/description {:type :cropped} description]
   [components/tags tags]])

(defn card
  [{:keys [slug] :as company} type]
  [components/card type
   [components/header
    [components/company-info company]
    [components/entity-icon "union" type]]
   [components/description "Recently published their public profile"]
   [details company]
   [components/footer :default
    [components/footer-buttons
     [components/button
      {:href (routes/path :companies)
       :type :inverted}
      "All companies"]
     [components/button
      {:href (routes/path :company :params {:slug slug})}
      "View Profile"]]]])
