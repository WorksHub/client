(ns wh.components.activities.job-published
  (:require [wh.interop :as interop]
            [wh.components.activities.components :as components]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn details [{:keys [title slug remote sponsorship-offered display-location display-salary tags role-type display-date]
                :as   job}
               entity-type]
  [components/inner-card
   [components/title-with-icon
    [components/title
     {:href (routes/path :job :params {:slug slug})}
     title]
    [components/entity-icon "suitcase" entity-type]]
   [components/meta-row
    [:p (util/smc styles/job__salary) display-salary]
    [components/text-with-icon {:icon "clock"} role-type]
    (when display-date
      [components/text-with-icon {:icon "calendar"} display-date])
    [components/text-with-icon {:icon "pin"} display-location]
    (when remote
      [components/text-with-icon {:icon "world"} "Remote"])
    (when sponsorship-offered
      [components/text-with-icon {:icon "sponsorship"} "Sponsorship"])]
   [components/tags tags]])

(defn card
  [{:keys [slug] :as job} type]
  [components/card type
   [components/header
    [components/company-info (:job-company job)]
    [components/entity-description :job type]]
   [components/description {:type :cropped} (:tagline job)]
   [details job type]
   [components/footer :compound
    [components/actions
     {:save-opts (interop/on-click-fn
                   (interop/show-auth-popup
                     :jobcard-apply
                     [:job
                      :params {:slug slug}
                      :query-params {"apply" true "apply_source" "public-feed"}]))}]
    [components/footer-buttons
     [components/button
      {:href (routes/path :jobsboard)
       :type :inverted}
      "All jobs"]
     [components/button
      {:href (routes/path :job :params {:slug slug})}
      "View job"]]]])


