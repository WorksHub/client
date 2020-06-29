(ns wh.components.activities.company-published
  (:require [wh.components.activities.components :as components]
            [wh.routes :as routes]
            [wh.util :as util]
            [wh.styles.activities :as styles]))

(def convert {:micro  "1-9"
              :small  "10-49"
              :medium "50-250"
              :large  "250+"})

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
         (when (convert size) [components/text-with-icon {:icon "couple"} (convert size)])
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
