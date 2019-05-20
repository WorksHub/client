(ns wh.homepage.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.components.cards :refer [blog-card]]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :refer [codi-message button]]
    [wh.components.get-started.views :refer [get-started-buttons get-started-banner]]
    [wh.components.homepage :refer [top-section-template jobs-section blogs-section]]
    [wh.components.icons :refer [icon]]
    [wh.components.www-homepage :as www]
    [wh.homepage.subs :as subs]
    [wh.landing.views :as landing]
    [wh.subs :refer [<sub] :as subs-common]))

(defn top-section []
  (let [vertical-display-label (<sub [::subs-common/vertical-label])]
    [top-section-template vertical-display-label (partial get-started-buttons [:homepage-hero])]))

(defn job-apply-buttons [job]
  [:div.apply
   [:div.buttons
    [:button.button {:on-click #(dispatch [:auth/show-popup {:type :homepage-jobcard-more-info :job job}])}
     "More Info"]
    [:button.button {:on-click #(dispatch [:apply/try-apply job :homepage-jobcard-apply])}
     "Easy Apply"]]])

(defn job-chat-card []
  [:div.card.card--job.card--homepage
   [:div
    [codi-message "\uD83D\uDC4B "
     "Here's a sample of the types of jobs you will see once you sign up."]
    [codi-message "Our members get access to companies hiring \uD83E\uDD84, salary details \uD83D\uDCB0 and full job descriptions."]
    [button "Get Started" [:register/get-started {:type :homepage-fake-jobcard}]]]])

(defn jobs []
  (let [jobs (<sub [::subs/jobs])]
    [jobs-section jobs job-chat-card job-apply-buttons icon]))

(defn search-jobs []
  [:section.pod-section
   [:div.home-page-job-search
    [link [:button#jobboard-button.button.button--inverted "Search for jobs"] :jobsboard]]])

(defn contribute-button []
  [:button.button {:on-click #(dispatch [:wh.events/contribute])} "Contribute"])

(defn blogs []
  (let [blogs (<sub [::subs/blogs])]
    [blogs-section blogs blog-card contribute-button (<sub [::subs-common/vertical-label])]))

(defn page []
  (cond
    (= "www" (<sub [:wh.subs/vertical]))
    (www/homepage (<sub [:wh.subs/page-params]))

    (<sub [:wh.subs/query-param "newlanding"])
    (landing/page)

    true
    [:div
     [top-section]
     [:div.dashboard
      [:div.main
       [jobs]
       [search-jobs]
       [blogs]]]
     [:div.banner-section
      [get-started-banner [:homepage-banner]]]]))
