(ns wh.components.activities.company-published
  (:require [wh.components.activities.components :as components]
            [wh.common.specs.company :as company-spec]
            [wh.routes :as routes]))

(defn details [{:keys [name tags slug description size locations] :as company} entity-type]
  [components/inner-card
   [components/title-with-icon
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
    [components/entity-icon "union" entity-type]]
   [components/description {:type :cropped} description]
   [components/tags tags]])

(defn card
  [{:keys [slug] :as company} type]
  [components/card type
   [components/header
    [components/company-info company]
    [components/entity-description :company type]]
   [components/description (if (= type :publish)
                             "Recently published their public profile"
                             "Recently had a lot of views")]
   [details company type]
   [components/footer :default
    [components/footer-buttons
     [components/button
      {:href (routes/path :companies)
       :type :inverted}
      "All companies"]
     [components/button
      {:href (routes/path :company :params {:slug slug})}
      "View Profile"]]]])
