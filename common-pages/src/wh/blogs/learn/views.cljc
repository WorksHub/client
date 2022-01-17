(ns wh.blogs.learn.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.blogs.learn.db :as learn-db]
    [wh.blogs.learn.events :as events]
    [wh.blogs.learn.subs :as subs]
    [wh.blogs.learn.components :as learn-components]
    [wh.components.buttons-page-navigation :as buttons-page-navigation]
    [wh.components.carousel :refer [carousel]]
    [wh.components.icons :as icons]
    [wh.components.issue :as issue]
    [wh.components.job :refer [job-card]]
    [wh.components.pagination :refer [pagination]]
    [wh.components.pods.candidates :as candidate-pods]
    [wh.styles.blogs :as styles]
    [wh.re-frame :as rf]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]))

(defn learn-header
  []
  [:div
   [:h1 (<sub [::subs/header])]
   [:div.spread-or-stack
    [:h3 (<sub [::subs/sub-header])]]])

(def carousel-size 6)

(defn recommended-issues-mobile []
  (let [issues (take carousel-size (<sub [::subs/recommended-issues]))
        steps  (for [issue issues]
                 ^{:key (:id issue)}
                 [issue/issue-card issue {:small? true}])]
    (when-not (zero? (count issues))
      [:div.recommendation.recommendation--mobile.recommendation--issues.is-hidden-desktop
       [:h2.recommendation__title "Recommended Issues"]
       [carousel steps {:arrows?         true
                        :arrows-position :bottom}]])))

(defn recommended-jobs-mobile []
  (let [jobs         (take carousel-size (<sub [::subs/recommended-jobs]))
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        steps        (for [job jobs]
                       ^{:key (:id job)}
                       [job-card job {:logged-in?        logged-in?
                                      :small?            true
                                      :user-has-applied? has-applied?
                                      :user-is-company?  (not (nil? company-id))
                                      :user-is-owner?    (or admin? (= company-id (:company-id job)))}])]
    (when-not (zero? (count jobs))
      [:div.recommendation.recommendation--mobile.is-hidden-desktop
       [:h2.recommendation__title "Recommended Jobs"]
       [carousel steps {:arrows?         true
                        :arrows-position :bottom}]])))

(defn search-btn [search?]
  (let [icon-name  (if search? "search-new" "close")
        aria-label (if search? "Search button" "Reset search")]
    [:button.search-button
     {:aria-label aria-label
      :data-test  "blog-search-button"}
     [icons/icon icon-name]]))

(defn search []
  (let [search-term  (<sub [::subs/search-term])
        local-search (rf/atom search-term)]
    (fn []
      (let [search-term      (<sub [::subs/search-term])
            search? #?(:cljs (or (nil? @local-search)
                                 (not= @local-search search-term))
                       :clj  true)]
        [:form.wh-formx.articles-page__search-form
         (merge {:action (routes/path :learn :query-params {learn-db/search-query-name ""})}
                #?(:cljs {:on-submit #(do (.preventDefault %)
                                          (dispatch [::events/search-articles @local-search]))}))
         [:input.input
          (merge {:name        learn-db/search-query-name
                  :placeholder "Search articles..."
                  :type        "text"
                  :id          "blog-search-box"
                  :value       @local-search
                  :data-test   "blog-search-input"}
                 #?(:cljs {:on-change #(reset! local-search (.. % -target -value))}))]
         [:input {:type  "hidden"
                  :name  "interaction"
                  :value 1}]
         [search-btn search?]]))))

(defn blog-list-comp [ctx blogs]
  [:div {:class styles/blog-list}
   (cond
     (:loading? ctx)
     (for [i (range 10)]
       ^{:key i}
       [learn-components/blog-comp ctx nil])
     ;;
     (= (count blogs) 0)
     [:div {:class styles/not-found} "We found no blogs matching your criteria \uD83D\uDE22"]
     ;;
     :else
     (for [blog blogs]
       ^{:key (:id blog)}
       [learn-components/blog-comp ctx blog]))])

(defn page []
  (let [blogs       (<sub [::subs/all-blogs])
        page-params (<sub [:wh/page-params])
        tag         (:tag page-params)
        tags        (<sub [::subs/tagbox-tags])
        loading?    (<sub [::subs/all-blogs-loading?])
        logged-in?  (<sub [:user/logged-in?])]
    [:div {:class     styles/page
           :data-test "page"}
     (when-not logged-in?
       [learn-header])
     [:div {:class styles/navigation-buttons-wrapper}
      [buttons-page-navigation/buttons-articles]]
     [:div {:class styles/page-columns}
      ;; remove unnecessary space from pagination
      [:div {:class styles/main-column}
       [search]
       [:div.is-hidden-desktop
        [learn-components/tag-picker-comp tags]]
       [blog-list-comp
        {:loading?   loading?
         :logged-in? (<sub [:user/logged-in?])
         :test?      true}
        blogs]
       [pagination
        (<sub [::subs/current-page])
        (<sub [::subs/pagination])
        (if tag :learn-by-tag :learn-search)
        (<sub [:wh/query-params])
        page-params]]
      [:div.is-hidden-mobile
       [learn-components/tag-picker-comp tags]
       [candidate-pods/candidate-cta]]]]))
