(ns wh.components.activities.job-published
  (:require [clojure.string :as str]
            [wh.components.icons :as icons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.slug :as slug]
            [wh.styles.feed-published-cards :as styles]
            [wh.util :as util]))

(defn job-tag [tag]
  (let [href (routes/path :pre-set-search :params {:tag (slug/slug (:slug tag))})]
    [tag/tag :a (assoc tag :href href)]))

(defn job-tags
  [tags]
  [:ul (util/smc styles/job-details__tags "tags" "tags--inline")
   (for [tag tags]
     [job-tag tag])])

(defn job-details [{:keys [title slug remote location remuneration
                           sponsorship-offered display-location
                           display-salary tags role-type display-date]}]
  [:div (util/smc styles/job-info)
   [:h1 (util/smc styles/job-info__title)
    [:a {:class (util/mc styles/job-info__title__link)
         :href  (routes/path :job :params {:slug slug})} title]]

   [:div (util/smc styles/job-details)
    [:p (util/smc styles/job-details__remuneration) display-salary]

    [:div (util/smc styles/job-details__contract-type)
     [:div (util/smc styles/job-details__icon) [icons/icon "clock"]]
     [:p role-type]]

    (when display-date
      [:div (util/smc styles/job-details__published-date)
       [:div (util/smc styles/job-details__icon) [icons/icon "calendar"]]
       [:p display-date]])

    [:div (util/smc styles/job-details__location)
     [:div (util/smc styles/job-details__icon) [icons/icon "pin"]]
     [:p display-location]]

    (when remote
      [:div (util/smc styles/job-details__remote)
       [:div (util/smc styles/job-details__icon) [icons/icon "world"]]
       [:p "Remote"]])

    (when sponsorship-offered
      [:div (util/smc styles/job-details__sponsorship)
       [:div (util/smc styles/job-details__icon) [icons/icon "award"]]
       [:p "Sponsorship"]]) ]

   [job-tags tags]])

(defn card-footer [{:keys [slug]}]
  [:div (util/smc styles/links)
   ;; Commented out icon that exists on design but doesn't have particular functionality yet
   #_[:div (util/smc styles/icon-thumbsup) [icons/icon "thumbsup"]]
   (let [job-page-path [:job
                        :params {:slug slug}
                        :query-params {"apply" true "apply_source" "public-feed"}]]
     [:a (merge
           (util/smc styles/icon-save)
           (interop/on-click-fn
             (interop/show-auth-popup :jobcard-apply job-page-path)))
      [icons/icon "save"]])
   ;; Commented out icon that exists on design but doesn't have particular functionality yet
   #_[:div (util/smc styles/icon-network) [icons/icon "network"]]

   [:a {:class    (util/mc styles/all-jobs)
        :href     (routes/path :jobsboard)}
    "All jobs"]
   [:a {:class    (util/mc styles/see-job)
        :href     (routes/path :job :params {:slug slug})}
    "See job"]])

(defn company-details [{:keys [name total-published-job-count logo]}]
  [:div {:class styles/company}
   [:img {:class styles/company__logo
          :src   logo}]
   [:h1 (util/smc styles/company__name) name]

   (when total-published-job-count
     [:span (util/smc styles/company__live-jobs)
      (str "Live jobs: " total-published-job-count)])

   [icons/icon "suitcase" :class styles/company__icon]])

(defn card [{title   :title  slug    :slug
             remote  :remote tagline :tagline
             company :job-company
             :as     job}]

  [:div (util/smc styles/card)
   [company-details company]

   [:p (util/smc styles/job-description) tagline]

   [job-details job]

   [card-footer job]])
