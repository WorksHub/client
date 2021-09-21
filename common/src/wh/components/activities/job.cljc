(ns wh.components.activities.job
  (:require [wh.common.url :as url]
            [wh.components.activities.components :as components]
            [wh.interop :as interop]
            [wh.re-frame.events :refer [dispatch]]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))


(defn details
  ([job entity-type]
   (details job entity-type nil))

  ([{:keys [title slug remote sponsorship-offered display-location display-salary
            tags role-type display-date recent-interviews-count interview-requests-period]
     :as   _job} entity-type actor]
   [components/inner-card
    [:div
     [components/title-with-icon
      [components/title
       {:href (routes/path :job :params {:slug slug})}
       title]
      [components/entity-icon "suitcase" entity-type]]

     (when (= entity-type :interview-requests)
       [:span {:class styles/job__recent-interviews-count}
        [components/->interviews-display-value {:interviews-count  recent-interviews-count
                                                :interviews-period interview-requests-period}]])]

    (when (= entity-type :promote)
      [components/company-info-small actor])

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

    [components/tags tags]]))

(defn card-footer [{:keys [base-uri slug id title vertical facebook-app-id saved-jobs logged-in? job]}]
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
                            :query-params {:apply       "true"
                                           :interaction 1}]]
       {:type :inverted-highlighed
        :href (apply routes/path job-page-params)})
     "Apply Now"]
    [components/button
     {:href (routes/path :job :params {:slug slug})}
     "View job"]]])

(defn base-card
  [{:keys [slug id title] :as job} actor type
   {:keys [logged-in? saved-jobs base-uri vertical facebook-app-id]}]
  [:<>
   [details job type actor]
   [card-footer
    {:base-uri        base-uri
     :slug            slug
     :id              id
     :title           title
     :vertical        vertical
     :facebook-app-id facebook-app-id
     :saved-jobs      saved-jobs
     :logged-in?      logged-in?
     :job             job}]])
