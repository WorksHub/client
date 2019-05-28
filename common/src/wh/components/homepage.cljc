(ns wh.components.homepage
  (:require
    [wh.components.common :refer [companies-section wrap-img img]]
    [wh.components.job :as job]))

(defn top-section-template [vertical-display-label get-started-form]
  [:div.container
   [:div.columns.homepage-header
    [:div.column.is-half.header-copy
     [:h1.h1--home-page "Discover the best " [:span.red vertical-display-label] " opportunities"]
     [:h3.h3--home-page "Get started to access companies hiring, salary details, a personalized dashboard and our AI Recruiter \uD83E\uDD16"]
     (when get-started-form
       [get-started-form])]
    [:div.column.is-half
     [:div.is-hidden-mobile
      [:img {:src "/images/homepage/desktop.svg"
             :alt "Homepage hero graphics"}]]
     [:div [:div.is-hidden-desktop.homepage-header__mobile-hero
            [:img {:src "/images/homepage/mobile.svg"
                   :alt "Homepage hero graphics"}]]]]]
   (companies-section "Join engineers from:")])

(defn jobs-section [jobs job-chat-card job-apply-buttons icon]
  [:section.pod-section
   (into
    [:div#featured-jobs.columns.is-mobile]
    (conj
     (for [job (take 2 jobs)]
       [:div.column (job/homepage-job-card job job-apply-buttons icon)])
     [:div.column (when job-chat-card
                    [job-chat-card])]))])

(defn blogs-section [blogs card button vertical-label-name]
  [:section.pod-section
   [:div.columns
    [:div.column.is-half
     [:div.contribute-wrapper
      [:h2 "Ideas and perspectives you won’t find anywhere else."]
      [:p "We tap into the brains of the world’s most talented " vertical-label-name " writers, thinkers, and storytellers to bring you the smartest takes on topics that matter. Recommended directly to you based on your interests, you can always find fresh thinking and unique perspectives."]
      (when button
        [button])]]
    [:div.column.is-half
     [:img.contribute-image {:src "/images/homepage/contribute.png"
                             :alt "Contribute graphics"}]]]
   (into
    [:div#featured-blogs.columns.is-mobile]
    (for [blog blogs]
      [:div.column (card blog)]))])
