(ns wh.logged-in.dashboard.views
  (:require
    [wh.components.cards :refer [blog-card]]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :refer [button codi-message]]
    [wh.components.error.views :refer [loading-error]]
    [wh.components.icons :refer [icon]]
    [wh.components.job :refer [job-card]]
    [wh.components.loader :refer [loader-cover]]
    [wh.logged-in.dashboard.events :as events]
    [wh.logged-in.dashboard.subs :as subs]
    [wh.subs :refer [<sub]]
    [wh.user.subs :as user-subs]))

(defn jobs-intro []
  [:div
   [codi-message
    [:div
     [:p "These have been chosen based on your selected preferences during sign up."]
     (when (<sub [:user/approved?])
       [:p "Click on the heart if you like what you see, close it if you don’t."])
     [:p "We’ve also listed some of your preferences below. These can be updated any time. Just click on Improve Recommendations or go to your profile in the menu."]]]
   [:div
    [button "OK, got it!" [:wh.user.events/add-onboarding-msg "jobs"]]]])

(defn resources-intro []
  [:div
   [codi-message
    [:div
     [:p "Think of this section as your own career library. We’ll show you content tailored to your preferences."]
     [:p "You can also create a post ✏️ about a story, learning or interesting project from the contribute page."]
     [:p "Which we will then recommend to members, based on their interests and profile!"]]]
   [:div
    [button "Thanks, got it!" [:wh.user.events/add-onboarding-msg "blogs"]]]])

(defn recommended-jobs []
  [:section.dashboard__jobs
   [:h2.spread-or-stack
    [:span "Recommended Jobs for " (<sub [::user-subs/name]) " | " [:span.slender (<sub [::subs/date])]]
    [link [:button.button.is-hidden-mobile "See more"] :recommended]]
   [loader-cover
    (<sub [::subs/loading-recommended?])
    (if-not (seq (<sub [::subs/jobs]))
      [:h3 "Sorry, there seem to be no jobs matching your profile \uD83D\uDE25. Try to add/change skills in your profile to see some recommended jobs."]
      (let [jobs         (<sub [::subs/jobs])
            has-applied? (some? (<sub [:user/applied-jobs]))
            jobs-columns (map (fn [job] [:div.column [job-card job
                                                      {:on-close          :reload-dashboard
                                                       :user-has-applied? has-applied?
                                                       :logged-in?        true}]]) jobs)
            columns      (if (<sub [::user-subs/onboarding-msg-not-seen? "jobs"])
                           (into
                             [[:div.column.codi-column [jobs-intro]]]
                             (take 2 jobs-columns))
                           jobs-columns)]
        (into
          [:div.columns.is-mobile]
          columns)))]])

(defn user-preferences []
  (let [user-name (<sub [::user-subs/name])
        preferences (<sub [::user-subs/skills-names])]
    [:section
     [:h3.is-hidden-tablet user-name "'s Preferences:"]
     [:div.pref-container
      [:div.preferences
       [:span.caption.is-hidden-mobile user-name "'s Preferences:"]
       (into
         [:ul.tags]
         (for [item preferences]
           [:li item]))
       [:span.clamped
        [link [:button.button "Improve Recommendations"] :improve-recommendations]]]]]))

(defn blogs []
  [:section
   [:h2 "Recommended Reading"]
   (if-not (seq (<sub [::subs/blogs]))
     [:h3 "Sorry, there seem to be no blogs matching your profile \uD83D\uDE25. Try to add/change skills in your profile to see some recommended blogs."]
     (let [blogs (<sub [::subs/blogs])
           blogs-columns (map (fn [blog] [:div.column [blog-card blog]]) blogs)
           columns (if (<sub [::user-subs/onboarding-msg-not-seen? "blogs"])
                     (into
                       [[:div.column.codi-column [resources-intro]]]
                       (take 2 blogs-columns))
                     blogs-columns)]
       (into
         [:div.columns.is-mobile]
         columns)))])

(defn page []
  (cond (= (<sub [::subs/loading-error]) :unavailable)
        [:div.main [loading-error]]

        :else
        [:div.main.dashboard
         [:h1 "Dashboard"]
         [recommended-jobs]
         [user-preferences]
         [blogs]]))
