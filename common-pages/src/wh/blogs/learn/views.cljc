(ns wh.blogs.learn.views
  (:require
    #?(:cljs [reagent.core :as r])
    [re-frame.core :refer [dispatch]]
    [wh.components.icons :as icons]
    [wh.blogs.learn.subs :as subs]
    [wh.blogs.learn.db :as learn-db]
    [wh.components.cards :refer [blog-card blog-row]]
    [wh.components.pagination :refer [pagination]]
    [wh.components.issue :as issue]
    [wh.components.carousel :refer [carousel]]
    [wh.interop :as interop]
    [wh.components.pods.candidates :as candidate-pods]
    [wh.routes :as routes]
    [wh.components.job :refer [job-card]]
    [wh.slug :as slug]
    [wh.util :as util]
    [wh.re-frame :as rf]
    [wh.components.tag :as tag]
    [wh.re-frame.subs :refer [<sub]]))

(defn learn-header
  []
  (let [logged-in? (<sub [:user/logged-in?])]
    [:div
     [:h1 (<sub [::subs/header])]
     [:div.spread-or-stack
      [:h3 (<sub [::subs/sub-header])]
      [:div.has-bottom-margin
       (when (<sub [::subs/show-contribute?])
         [:button#learn_contribute.button.learn--contribute-button
          #?(:clj (when-not logged-in?
                    (interop/on-click-fn
                      (interop/show-auth-popup :contribute [:contribute])))
             :cljs {:on-click #(dispatch (if logged-in?
                                           [:wh.events/nav :contribute]
                                           [:wh.events/contribute]))}) "Write Article"])]]]))

(defn recommended-jobs []
  (let [jobs         (<sub [::subs/recommended-jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])]
    (when-not (= (count jobs) 0)
      [:section.recommendation
       [:h2 "Recommended Jobs"]
       (for [job jobs]
         ^{:key (:id job)}
         [:div
          [job-card job {:logged-in?        logged-in?
                         :small?            true
                         :user-has-applied? has-applied?
                         :user-is-company?  (not (nil? company-id))
                         :user-is-owner?    (or admin? (= company-id (:company-id job)))}]])])))

(defn recommended-issues []
  (let [issues (<sub [::subs/recommended-issues])]
    (when-not (= (count issues) 0)
      [:section.recommendation
       [:h2 "Recommended Issues"]
       [:div.issues__list
        (for [issue issues]
          ^{:key (:id issue)}
          [issue/issue-card issue {:small? true}])]])))

(defn tag-picker [tags]
  [:section.split-content-section.tag-picker
   (tag/strs->tag-list :a tags
    {:f #(assoc % :href (routes/path :learn-by-tag :params {:tag (slug/slug+encode (:label %))}))})])

(def carousel-size 6)

(defn recommended-issues-mobile []
  (let [issues (take carousel-size (<sub [::subs/recommended-issues]))
        steps (for [issue issues]
                ^{:key (:id issue)}
                [issue/issue-card issue {:small? true}])]
    (when-not (= (count issues) 0)
      [:div.recommendation.recommendation--mobile.recommendation--issues.is-hidden-desktop
       [:h2.recommendation__title "Recommended Issues"]
       [carousel steps {:arrows? true
                        :arrows-position :bottom}]])))

(defn recommended-jobs-mobile []
  (let [jobs         (take carousel-size (<sub [::subs/recommended-jobs]))
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        steps (for [job jobs]
                ^{:key (:id job)}
                [job-card job {:logged-in?        logged-in?
                               :small?            true
                               :user-has-applied? has-applied?
                               :user-is-company?  (not (nil? company-id))
                               :user-is-owner?    (or admin? (= company-id (:company-id job)))}])]
    (when-not (= (count jobs) 0)
      [:div.recommendation.recommendation--mobile.is-hidden-desktop
       [:h2.recommendation__title "Recommended Jobs"]
       [carousel steps {:arrows? true
                        :arrows-position :bottom}]])))

(defn newsletter
  []
  (let [render (fn []
                 (when-not (<sub [:user/logged-in?])
                   [:section
                    {:class (util/merge-classes "pod"
                              "pod--no-shadow"
                              "newsletter-subscription")}
                    [:div [:h3.newsletter-subscription__title "Join our newsletter!"]
                     [:p.newsletter-subscription__description "Join over 111,000 others and get access to exclusive content, job opportunities and more!"]
                     [:form#newsletter-subscription.newsletter-subscription__form
                      [:div.newsletter-subscription__input-wrapper
                       [:label.newsletter-subscription__label {:for "email"} "Your email"]
                       [:input.newsletter-subscription__input {:type "email" :name "email" :placeholder "turing@machine.com" :id "email"}]]
                      [:button.button.newsletter-subscription__button "Subscribe"]]
                     [:div.newsletter-subscription__success.is-hidden
                      [:div.newsletter-subscription__primary-text "Thanks! " [:br.is-hidden-desktop] "See you soon!"]]]]))]
    #?(:cljs (r/create-class
               {:component-did-mount (interop/listen-newsletter-form)
                :reagent-render render})
       :clj  (some-> (render)
                     (conj [:script (interop/listen-newsletter-form)])))))

(defn search-btn [search?]
  (let [icon-name (if search? "search-new" "close")
        aria-label (if search? "Search button" "Reset search")]
    [:button.search-button
     {:aria-label aria-label}
     [icons/icon icon-name]]))

(defn search []
  (let [search-term (<sub [::subs/search-term])
        local-search (rf/atom search-term)]
    (fn []
      (let [search-term (<sub [::subs/search-term])
            search? #?(:cljs (or (nil? @local-search)
                                 (not= @local-search search-term))
                       :clj true)]
        [:form.wh-formx.articles-page__search-form
         #?(:cljs {:on-submit (if search?
                                #(do (.preventDefault %)
                                     (dispatch [:wh.events/nav--set-query-param
                                                learn-db/search-query-name
                                                @local-search]))
                                #(do (.preventDefault %)
                                     (dispatch [:wh.events/nav--set-query-param
                                                learn-db/search-query-name
                                                nil])
                                     (reset! local-search nil)))})
         [:input.input
          (merge {:name learn-db/search-query-name
                  :placeholder "Search articles..."
                  :type "text"
                  :id "blog-search-box"
                  :value @local-search}
                 #?(:cljs {:on-change #(reset! local-search (.. % -target -value))}))]
         [:input {:type "hidden"
                  :name "interaction"
                  :value 1}]
         [search-btn search?]]))))

(defn page []
  (let [blogs (<sub [::subs/all-blogs])
        tag (<sub [:wh/page-param :tag])
        tags (<sub [::subs/tagbox-tags])
        ch-size (quot (count blogs) 3)
        [ch1 ch2 ch3] [(take ch-size blogs)
                       (->> blogs
                            (drop ch-size)
                            (take ch-size))
                       (drop (* 2 ch-size) blogs)]]
    [:div.main.articles-page
     (learn-header)
     [:div.split-content
      [:div.split-content__main.articles-page__blogs
       [search]
       [:div.is-hidden-desktop
        [tag-picker tags]]
       (for [blog ch1]
         ^{:key (:id blog)}
         [blog-row  blog])
       [newsletter]
       (when (> ch-size 1)
         [recommended-jobs-mobile])
       (for [blog ch2]
         ^{:key (:id blog)}
         [blog-row  blog])
       (when (> ch-size 1)
         [recommended-issues-mobile])
       (for [blog ch3]
         ^{:key (:id blog)}
         [blog-row  blog])
       [pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        (if tag :learn-by-tag :learn)
        (<sub [:wh/query-params])
        (when tag {:tag tag})]]
      [:div.split-content__side.is-hidden-mobile
       [tag-picker tags]
       [candidate-pods/candidate-cta]
       [recommended-jobs]
       [recommended-issues]]]]))



