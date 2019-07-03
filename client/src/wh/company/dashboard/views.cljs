(ns wh.company.dashboard.views
  (:require
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [clojure.set :refer [rename-keys]]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.common.data :as data]
    [wh.common.text :refer [pluralize]]
    [wh.company.dashboard.events :as events]
    [wh.company.dashboard.subs :as subs]
    [wh.company.edit.subs :as edit-subs]
    [wh.company.payment.views :as payment]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :as codi]
    [wh.components.ellipsis.views :refer [ellipsis]]
    [wh.components.icons :refer [icon]]
    [wh.components.stats.views :refer [stats-item]]
    [wh.jobs.job.views :as job]
    [wh.subs :refer [<sub]]
    [wh.user.subs :as user-subs]))

(defn- date->str [d]
  (let [t (t/today-at-midnight)
        t-1 (t/minus (t/today-at-midnight) (t/days 1))
        t+1 (t/plus (t/today-at-midnight) (t/days 1))
        today (t/interval t t+1)
        yesterday (t/interval t-1 t)]
    (cond
      (t/within? today d)     "today"
      (t/within? yesterday d) "yesterday"
      :otherwise              (str "on " (tf/unparse (tf/formatters :date) d)))))

(defn company-edit-link [link-title & args]
  (if (<sub [:user/company?])
    (into [link link-title :edit-company] args)
    (when-let [id (<sub [::subs/company-id])]
      (into [link link-title :admin-edit-company :id id] args))))

(defn mailto-link [title]
  [:a.a--underlined
   {:href "mailto:hello@works-hub.com"}
   title])

(defn upgrade-link [title]
  [link title
   :payment-setup
   :step :select-package
   :class "a--underlined"])

(defn intro-your-company []
  [:section.intro-your-company
   [:h2 "Your Company" [:span.slender " | " (<sub [::subs/today])]]
   [codi/codi-message
    [:div
     [:p "Welcome to your company dashboard! This is where you’ll see an overview of all the stats on your published roles. This will allow you to monitor the performance of your jobs and help you get more applications."]
     [:br]
     [:p "If you want to add new users to your company dashboard just head over to your company settings page."]]]
   [codi/button "Thanks, got it!" [:wh.user.events/add-welcome-msg "your_company"]]])

(defn intro-live-roles []
  [:div.intro-live-roles
   [codi/codi-message
    [:div
     [:p "Your first role will be published as soon as a member of our team has verified it."]
     [:br]
     [:p "Now might be a good time to go and review it. Just click on the card in your unpublished roles below."]]]
   [codi/button "OK, got it!" [:wh.user.events/add-welcome-msg "live_roles"]]])

(defn your-company []
  (let [package (<sub [::subs/package])]
    [:section.your-company
     [:h2 "Your Company" [:span.slender " | " (<sub [::subs/today])]]
     [:div.your-company__card.card
      [:div.your-company__name-line
       (if-let [logo (<sub [::subs/logo])]
         [:img.your-company__logo {:src logo}]
         [company-edit-link [icon "add-new-2"]
          :class "button button--inverted your-company__logo"])
       [:div.your-company__name
        [ellipsis (<sub [::subs/name]) {:vcenter? true}]]
       [company-edit-link "Edit"
        :class "button button--inverted your-company__edit"]]
      (when-let [description-html (<sub [::subs/description-html])]
        [:div.your-company__blurb {:dangerouslySetInnerHTML {:__html description-html}}])
      [:hr]
      (cond
        (<sub [::subs/disabled?])
        [:div.your-company__payment-setup.your-company__payment-setup--disabled
         [:p "Your company account has been disabled. If you believe it was done by mistake, please get in touch."]
         [:a
          {:href "mailto:hello@works-hub.com"
           :target "_blank"
           :rel "noopener"}
          [:button.button "Get in touch"]]]
        ;;
        (<sub [::subs/has-offer?])
        [:div.your-company__payment-setup
         [:p "We have prepared a new Take-Off package offer for you \uD83D\uDE80" [:br]
          " Click the button to see the offer and accelerate your hiring process!"]
         [link [:button.button.your-company__payment-setup__button "View Take-Off offer"]
          :payment-setup :step :pay-confirm :query-params {:package :take_off
                                                           :billing (or (<sub [::subs/billing-period])
                                                                        data/default-billing-period)}]]
        ;;
        (not= :unselected package)
        [:div.your-company__package
         [:div.your-company__package-top-line
          [:div.your-company__package-logo
           {:style {:background-image (str "url(" (<sub [::subs/package-icon]) ")")}}]
          [:div.your-company__package-name
           [:div.your-company__package-header "Your package:"]
           (<sub [::subs/package-name])]
          (when-not (= :take_off package)
            [:div.your-company__package-link
             (upgrade-link "")])] ; content specified in CSS
         [:div.your-company__package-description
          (if (= :take_off (<sub [::subs/package]))
            [:p "Now that you are our premium client with a Take-Off package, why not "
             (mailto-link "talk to us")
             " about what we can do for you?"]
            "It’s great to start out with this, but if you have more ambitious hiring plans you should upgrade to our Take-Off Package.")]]
        ;;
        :else
        [:div.your-company__payment-setup.your-company__payment-setup--unselected
         [:div [:h3 "Welcome!"]
          [:p "To get started just click Publish on any of your jobs and you will be guided through the next steps, including selecting one of our pricing packages."]]])]]))

(defn stats-codi-item []
  [:div.stats__item.stats__codi
   [:div.stats__icon [icon "codi"]]
   [:div.stats__caption "Codi says:"]
   [:div.stats__value-line (<sub [::subs/codi-message])]])

(defn stats []
  [:section.stats
   [:h2 (<sub [::subs/stats-title])]
   [:div.stats__card.card
    [stats-codi-item]
    [stats-item (merge {:icon-name "views"
                        :caption "Views"}
                       (<sub [::subs/stats-item :views]))]
    [stats-item (merge {:icon-name "like"
                        :caption "Likes"}
                       (<sub [::subs/stats-item :likes]))]
    [stats-item (merge {:icon-name "applications"
                        :caption "Applications"}
                       (<sub [::subs/stats-item :applications]))]]])


(defn job-stat [icon-name count & [hover-text]]
  [:div.company-job__stat
   (cond-> {:class (str "company-job__" icon-name)}
     hover-text (assoc :title hover-text))
   [icon icon-name]
   [:div.company-job__stat-number count]])


(defn job-card [{:keys [id slug title display-location published first-published tags stats partial-view-data matching-users verticals] :as job}]
  (let [company-name (<sub [::subs/name])
        logo (<sub [::subs/logo])
        preset-job (assoc job
                          :company-name company-name
                          :logo logo)]
    [:div.card.company-job
     {:id (str "dashboard__job-card__" id)}
     [:div.company-job__head-row
      [:img.company-job__logo {:src logo}]
      [:div.company-job__stats
       [job-stat "views" (:views stats)
        (when partial-view-data "We've only been collecting views since 18 July 2018, so the view counts for this job may be incomplete.")]
       [job-stat "like" (:likes stats)]
       [:a {:id (str "dashboard-job-card__view-applications-stat_" id)
            :href (<sub [::subs/view-applications-link id])}
        [job-stat "applications" (:applications stats)]]]]
     [:div.company-job__basic-info
      [:div.company-job__title [link title :job
                                :slug slug
                                :on-click #(dispatch-sync [:wh.job/preset-job-data preset-job])]]
      [:div.company-job__company
       [link company-name :job
        :slug slug
        :class "company-job__company-name"
        :on-click #(dispatch-sync [:wh.job/preset-job-data preset-job])]
       [:span.company-job__thats-you " – that's you	🙌"]]
      [:div.company-job__address [link display-location :job
                                  :slug slug
                                  :on-click #(dispatch-sync [:wh.job/preset-job-data preset-job])]]]
     (if published
       [:div.company-job__posted "Posted on " first-published]
       [:div.company-job__posted " "])
     (into
      [:ul.company-job__tags.tags]
      (map (fn [tag] [:li tag]) tags))
     (when (pos? matching-users)
       [:div.company-job__candidate-info
        "We have " matching-users " active " (pluralize matching-users "member") " with 75%+ match rates for this role. "
        (when-not (= (<sub [::subs/package]) :take_off)
          [:span (upgrade-link "Upgrade") " to access these candidates."])])
     [:div.company-job__buttons
      (if published
        [:a.button {:id (str "dashboard-job-card__view-applications-button_" id)
                    :href (<sub [::subs/view-applications-link id])}
         "View Applications"]
        (let [is-publishing? (<sub [::subs/is-job-publishing? id])]
          [:button.button
           {:id (str "dashboard-job-card__publish-button_" id)
            :class (when is-publishing? "button--inverted button--loading")
            :on-click #(dispatch [::events/publish-role id])}
           "Publish Role"]))
      [link "Edit" :edit-job :id id :class "button button--inverted"]]
     (when (<sub [::subs/is-publish-celebration-showing? id])
       [payment/publish-celebration
        {:title title
         :company-name company-name
         :verticals verticals
         :on-close #(dispatch [::events/close-publish-celebration id])}])]))

(defn add-job-card []
  [link [:div.card.add-job
         [:div.add-job__icon [icon "add-new-2"]]
         [:div.add-job__add "Add new role"]] :create-job])

(defn roles [class-name title jobs live-roles?]
  (into
   [:section {:class class-name}
    [:h2 title]]
   (let [cards (concat
                (when (and live-roles? (<sub [::user-subs/welcome-msg-not-seen? "live_roles"]))
                  [[intro-live-roles]])
                (for [job jobs] [job-card job])
                (when live-roles?
                  [[add-job-card]]))]
     (for [part (partition-all 2 cards)]
       (into [:div.columns]
             (for [card part]
               [:div.column.is-6 card]))))))

(defn live-roles []
  (roles "live-roles"
         "Live Roles"
         (<sub [::subs/published-jobs])
         true))

(defn unpublished-roles []
  (when-let [jobs (seq (<sub [::subs/unpublished-jobs]))]
    (roles "unpublished-roles"
           "Unpublished Roles"
           jobs
           false)))

(defmulti activity-item-content :type)

(defmethod activity-item-content "codi" [_]
  [:span
   "Need help promoting your role? Contact us! "
   (mailto-link "Just ask Codi")])

(defmethod activity-item-content "application" [{:keys [job-slug job-title user-id user-name timestamp]}]
  [:span
   (if (<sub [::subs/can-see-applications?])
     [link user-name :candidate :id user-id :class "a--underlined"]
     [:strong user-name])
   " applied for your "
   [link job-title :job :slug job-slug :class "a--underlined"]
   " role "
   (date->str timestamp)])

(defmethod activity-item-content "like" [{:keys [job-slug job-title timestamp]}]
  [:span
   "New like on your "
   [link job-title :job :slug job-slug :class "a--underlined"]
   " role "
   (date->str timestamp)])

(defmethod activity-item-content "views" [{:keys [job-slug job-title count timestamp]}]
  [:span
   "Your "
   [link job-title :job :slug job-slug :class "a--underlined"]
   " role has "
   count
   " "
   (pluralize count "view")
   " "
   (date->str timestamp)])

(defmethod activity-item-content "match" [{:keys [job-slug job-title count user-name timestamp]}]
  [:span
   "New "
   user-name
   " with +75% match rate for "
   (if (= count 1)
     [:span "your " [link job-title :job :slug job-slug :class "a--underlined"] " role "]
     [:span count " of your roles "])
   "has registered on the platform "
   (date->str timestamp)
   (when-not (= (<sub [::subs/package]) :take_off)
     [:span
      ". "
      (upgrade-link "Upgrade")
      " to access them."])])

(defmethod activity-item-content :default [_]
  "Not implemented yet")

(def activity-item-icons
  {"like" "like", "application" "applications", "views" "views", "match" "high-match", "codi" "codi"})

(defn activity-item [{:keys [type] :as data}]
  [:div.activity__item
   [:div.activity__icon-holder
    (let [icon-name (activity-item-icons type)]
      [icon icon-name :class icon-name])]
   [:div.activity__content (activity-item-content data)]])

(defn activity []
  [:section.activity
   [:h2 "Activity"]
   [:div.activity__container
    (into [:div]
          (map #(vector activity-item %)
               (<sub [::subs/activity])))
    (when (<sub [::subs/show-more?])
      [:button.button.button--inverted.activity__show-more
       {:on-click #(dispatch [::events/show-more])}
       "Show more..."])]])

(defn page []
  [:div.main-container
   [:div.main.company-dashboard
    {:class (when (<sub [::user-subs/welcome-msg-not-seen? "your_company"])
              "company-dashboard--with-intro-your-company")}
    [:h1 "Dashboard"]
    [:div.company-dashboard__grid
     [intro-your-company]
     [your-company]
     [stats]
     [activity]
     [live-roles]
     [unpublished-roles]]]
   (when (<sub [:wh.job/show-admin-publish-prompt?])
     [job/admin-publish-prompt
      (<sub [::subs/permissions])
      (<sub [::subs/company-id])
      [::events/update-company]])])
