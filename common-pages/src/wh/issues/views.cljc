(ns wh.issues.views
  (:require
    #?(:cljs [wh.components.forms.views :refer [radio-buttons select-field]])
    #?(:cljs [wh.components.github :as github])
    #?(:cljs [wh.components.overlay.views :refer [popup-wrapper]])
    [wh.common.data :as data]
    [wh.common.job :refer [format-job-location]]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :as issue]
    [wh.components.pagination :as pagination]
    [wh.how-it-works.views :as how-it-works]
    [wh.issue.edit.views :as edit-issue]
    [wh.issues.events :as events]
    [wh.issues.subs :as subs]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn webhook-info []
  #?(:cljs
     [popup-wrapper
      {:id           :webhook-info
       :on-ok        #(dispatch [:issues/show-webhook-info false])
       :button-label "OK, got it!"}
      [:div.info-content
       [:h1 "We were unable to set up a GitHub webhook for this repository"]
       [:h2 "What this means"]
       [:p "Every time you connect a repository to "
        (<sub [:wh.subs/platform-name])
        ", we try to instruct GitHub on your behalf that it should notify our platform whenever there's a new pull request. This is called a "
        [:a.a--underlined {:target "_blank" :rel "noopener" :href "https://developer.github.com/webhooks/"} "webhook"]
        ". We do this to change the issue's status when it's interacted with on GitHub."]
       [:p "This time around, we were unable to create this webhook for you. Your issues have been imported successfully, but they won't automatically follow GitHub activity."]
       [:h2 "What to do"]
       [:h3 "Check your repository's permissions"]
       [:p "To create a webhook, you need to have "
        [:a.a--underlined {:target "_blank" :rel "noopener" :href "https://help.github.com/articles/repository-permission-levels-for-an-organization/"} "admin permissions"]
        " on the repository. Contact the repository owner to ensure your permissions are sufficient."]
       [:h3 "Check your organization's settings"]
       [:p "Follow "
        [:a.a--underlined {:target "_blank" :rel "noopener" :href "https://help.github.com/articles/requesting-organization-approval-for-oauth-apps/"} "these instructions"]
        " to grant organization approval to the " [:code "workshub"] " app."]
       [:h3 "Add the webhook manually"]
       [:p "We don't provide support for it yet. But we will soon, as a fallback!"]]]))

(defn admin-pod []
  (if (<sub [::subs/loading?])
    [:div.admin-pod.skeleton.card
     [:div.github-info
      [:div.github-info__link
       [:a.github-info__link__org]]]
     [:div
      [:div.manage-issues]]]
    [:div.admin-pod.card
     [:div.github-info
      (for [org (<sub [::subs/github-orgs])]
        [:div.github-info__link {:key org}
         [:a.github-info__link__org.a--underlined {:href (str "https://www.github.com/" org)} org]
         [icon "github"]])]
     #?(:cljs [:div.github-info__manage
               (if (<sub [:user/company-connected-github?]) ;; TODO same as below
                 [link "Manage Issues" :manage-issues
                  :class "manage-issues button level-item"]
                 [github/install-github-app])])]))

(defn stats-ball
  [[title para] colour]
  [:div
   {:class (util/merge-classes
             "issues__stats__ball"
             (str "issues__stats__ball--" (name colour)))}
   [:strong title]
   [:p para]])

(defn balls-side []
  [:div.issues__stats__balls
   (let [{:keys [orange blue grey]} (get data/how-it-works-stats :company)
         yellow (:blue (get data/how-it-works-stats :candidate))]
     [:div.issues__stats__balls-container
      (stats-ball blue :blue)
      (stats-ball grey :grey)
      (stats-ball orange :orange)
      (stats-ball yellow :yellow)])])

(defn issues-list []
  (let [issues (<sub [::subs/issues])
        issue-opts #?(:cljs {:edit-fn         (when (<sub [::subs/can-manage-issues?])
                                                :edit-issue/show-issue-edit-popup)
                             :edit-success-fn [::events/update-issue-success]}
                      :clj  {})]
    (if (<sub [::subs/loading?])
      [:div.issues__list
       [issue/issue-card {:id (str "skeleton-issue-" 1)}]
       [issue/issue-card {:id (str "skeleton-issue-" 2)}]
       [issue/issue-card {:id (str "skeleton-issue-" 3)}]]
      (if (seq issues)
        (let [issues-1 (take 4 issues)
              issues-2 (take 4 (drop 4 issues))
              issues-3 (drop 8 issues)]
          [:div.issues__list
           [:div.issues__list__head
            (for [issue issues-1]
              ^{:key (:id issue)}
              [issue/issue-card issue issue-opts])]
           [:div.is-hidden-desktop
            [balls-side]]
           [:div.issues__list__rest
            (for [issue issues-2]
              ^{:key (:id issue)}
              [issue/issue-card issue issue-opts])
            #?(:cljs
               [how-it-works/pod--benefits (<sub [:user/type])])
            (for [issue issues-3]
              ^{:key (:id issue)}
              [issue/issue-card issue issue-opts])]])
        [:div
         [:p "No published issues were found."]]))))

(defn hiring-pod
  []
  (let [company-name (:name (<sub [::subs/company]))]
    [:div.pod.pod--no-shadow.issues__hiring-pod
     [:div.issues__hiring-pod__header
      [:h2 (str company-name " is hiring!")]
      [icon "codi"]]
     (if (<sub [:user/logged-in?])
       [:div.issues__hiring-pod__jobs
        (doall
          (for [{:keys [id slug title location remote]} (<sub [::subs/jobs])]
            ^{:key id}
            [:div.issues__hiring-pod__job
             [link [:strong title] :job :slug slug]
             [:span (format-job-location location remote)]]))
        [link [:button.button "View Jobs At This Company"] :jobsboard :query-params {:search company-name}]]
       [:div.issues__hiring-pod__sign-up
        [:p "Sign up to Workshub to view all their available positions"]
        [link [:button.button "Get Started"] :get-started]])]))

(defn company-pod [class]
  (let [company (<sub [::subs/company])]
    [:div {:class (util/merge-classes "card" "company-pod" class)}
     [:div.company-header__top
      [:div.logo (wrap-img img (:logo company) {})]
      [:h1 (<sub [::subs/header])]]
     [:div.company-header__github-links
      (for [org (<sub [::subs/github-orgs])]
        [:div.company-header__github-link {:key org}
         [:a.a--underlined {:href (str "https://www.github.com/" org)} org]
         [icon "github"]])]]))

(defn public-header []
  [:div.issues__header--public.issues__header--public
   [:div.issues__header--public__inner
    [:div.issues__header--public__img
     [:div.issues__header--public__img-inner
      [:img {:src "/images/hiw/header.svg"
             :alt "Hero graphic"}]]]
    [:div.issues__header--public__copy
     [:h1 "Use Open Source to hire or get hired"]
     [:p data/www-hero-copy]
     [:div.issues__header__buttons
      [link [:button.button "Get Started"] :get-started :query-params {:redirect "issues"}]
      [link [:button.button.button--inverted.find-out-more "Find out more"] :how-it-works]]]]])

(defn sorting-component []
  [:div.issues__sorting
   [:div {:class (util/merge-classes
                    "issues__count"
                    (when (<sub [::subs/loading?]) "skeleton"))}
    [:span (<sub [::subs/issues-count-str])]]
   #?(:cljs
      [:div.issues__sorting__dropdown__container.wh-formx
       [:span.issues__sorting__dropdown__label "Sort by:"]
       [select-field (<sub [::subs/sorting-by])
        {:class     "issues__sorting__dropdown"
         :options   (<sub [::subs/sorting-options])
         :on-change [::events/sort-by]}]])])

(defn page []
  (if (and (<sub [::subs/own-company?])
           (not (<sub [:user/company-connected-github?])))
    (how-it-works/page {:github? true :site-type :company})
    (let [logged-in? #?(:cljs    (<sub [:user/logged-in?])
                        :default false
                        :clj     false)]
      [:div.main
       (if (<sub [::subs/issues-for-company-id])
         [company-pod "is-mobile"]
         (if logged-in?
           [:h1 {:class (util/merge-classes "issues__header"
                                            (when (<sub [::subs/loading?]) "skeleton"))}
            (<sub [::subs/header])]
           [public-header]))
       [:div.is-flex.issues__main__container
        [:div.issues__main
         (when (<sub [::subs/company-pod?])
           [company-pod "is-desktop"])
         [sorting-component]
         [issues-list]
         [pagination/pagination (<sub [::subs/current-page-number]) (<sub [::subs/pagination]) (<sub [::subs/page]) (<sub [::subs/query-params]) (<sub [::subs/page-params])]]
        [edit-issue/edit-issue]
        [:div.issues__side
         #?(:cljs
            (when (<sub [::subs/can-manage-issues?])
              [admin-pod]))
         (cond
           #?(:cljs (<sub [:user/company?])
              :clj  false)
           [how-it-works/pod--company]
           logged-in?
           [how-it-works/pod--candidate]
           (and (= :issues (<sub [::subs/page]))
                (<sub [::subs/company-pod?]))
           [how-it-works/pod--choice]
           (<sub [::subs/all-issues?])
           [:div.is-hidden-mobile
            [balls-side]]
           :else
           [how-it-works/pod--basic])
         (when (and (<sub [::subs/company-view?])
                    (<sub [::subs/has-jobs?]))
           [hiring-pod])
         #?(:cljs
            (when (<sub [::subs/show-webhook-info?])
              [webhook-info]))]]])))
