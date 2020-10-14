(ns wh.components.activities.job-published
  (:require [wh.common.url :as url]
            [wh.components.activities.components :as components]
            [wh.interop :as interop]
            [wh.re-frame.events :refer [dispatch]]
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
  [{:keys [slug id title] :as job} actor type
   {:keys [logged-in? saved-jobs base-uri vertical facebook-app-id]}]
  [components/card type
   [components/header
    [components/company-info actor]
    [components/entity-description :job type]]
   [components/description {:type :cropped} (:tagline job)]
   [details job type]
   [components/footer :compound
    (let [url (str (url/strip-path base-uri)
                   (routes/path :job :params {:slug slug}))]
      [components/actions
       {:share-opts {:url             url
                     :id              id
                     :content-title   title
                     :content         (str "this " (if (= type :publish) "new " "") title " job")
                     :vertical        vertical
                     :facebook-app-id facebook-app-id}
        :saved?     (contains? saved-jobs id)
        :save-opts  (if logged-in?
                      {:on-click #?(:cljs #(dispatch [:wh.events/toggle-job-like job])
                                    :clj "")}
                      (interop/on-click-fn
                        (interop/show-auth-popup
                          :jobcard-save
                          [:liked
                           :query-params {"action" "save" "job-id" id}])))}])
    [components/footer-buttons
     [components/button
      (let [job-page-params [:job
                             :params {:slug slug}
                             :query-params {:apply "true"}]]
        (merge {:type :inverted-highlighed}
               (if logged-in?
                 {:href (apply routes/path job-page-params)}
                 {:on-click (interop/show-auth-popup
                              :jobpage-apply
                              job-page-params)})))
      "Apply Now"]
     [components/button
      {:href (routes/path :job :params {:slug slug})}
      "View job"]]]])
