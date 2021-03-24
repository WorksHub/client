(ns wh.jobsboard.views
  (:require #?(:cljs [wh.jobs.jobsboard.subs :as subs]
               :clj [wh.jobsboard.subs :as subs])
            [wh.components.job-new :as job]
            [wh.components.newsletter :as newsletter]
            [wh.components.pagination :as pagination]
            [wh.interop :as interop]
            [wh.jobsboard.components :as components]
            [wh.jobsboard.db :as jobsboard]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.jobsboard :as styles]
            [wh.util :as util]))

(def jobs-container-class
  {:cards "jobs-board__jobs-list__content"
   :list  ""})

(defn header
  ([]
   (header {}))
  ([{:keys [preset-search? searching?]}]
   (let [{:keys [title subtitle description]} (<sub [::subs/header-info])]
     [:div
      {:class     (when (and preset-search? searching?) "skeleton")
       :data-test "jobs-board-header"}
      [:div
       [:h1 title]
       [:h2 subtitle]
       [:h3 description]]])))

(defn- skeleton-jobs [view-type]
  [:section.jobs-board__jobs-list
   [:div
    {:class (jobs-container-class view-type)}
    (for [i [1 2]]
      ^{:key i}
      [job/job-card {:id (str "skeleton-job-" i)}
       {:view-type view-type}])]])


(defn jobs-list-with-cta [view-type jobs job-card-opts]
  (let [jobs (split-at 6 jobs)]
    [:<>
     [:div
      {:class (jobs-container-class view-type)}
      (for [job (first jobs)]
        ^{:key (str "col-" (:id job))}
        [job/job-card job (job-card-opts (:company-id job))])]

     [newsletter/newsletter {:logged-in? false
                             :type       :job-list}]

     [:div
      {:class (jobs-container-class view-type)}
      (for [job (second jobs)]
        ^{:key (str "col-" (:id job))}
        [job/job-card job (job-card-opts (:company-id job))])]]))

(defn jobs-list [view-type jobs job-card-opts]
  [:div
   {:class (jobs-container-class view-type)}
   (for [{:keys [id] :as job} jobs]
     ^{:key id}
     [job/job-card job (job-card-opts (:company-id job))])])

(defn jobs-board [{:keys [route query-params view-type preset-search?]}]
  (let [jobs         (<sub [::subs/jobs])
        current-page (<sub [::subs/current-page])
        total-pages  (<sub [::subs/total-pages])
        logged-in?   (<sub [:user/logged-in?])
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        searching?   #?(:cljs (<sub [:wh.search/searching?])
                        :clj false)
        jobs-list    (if logged-in? jobs-list jobs-list-with-cta)

        job-card-opts (fn [job-company-id]
                        (merge {:logged-in?        logged-in?
                                :view-type         view-type
                                :user-has-applied? has-applied?
                                :user-is-company?  #?(:clj (or admin? (= company-id job-company-id))
                                                      :cljs (not (nil? company-id)))
                                :user-is-owner?    #?(:clj (= company-id job-company-id)
                                                      :cljs (or admin? (= company-id job-company-id)))
                                :apply-source      "jobsboard-job"}
                               #?(:clj {:on-save (interop/on-click-fn
                                                   (interop/show-auth-popup :search-jobs [:jobsboard]))})))]

    [:section.jobs-board__jobs-list
     (if searching?
       [skeleton-jobs view-type]

       [jobs-list view-type jobs job-card-opts])

     (when (and (seq jobs) (not searching?))
       #?(:clj [pagination/pagination
                current-page (pagination/generate-pagination current-page total-pages)
                route query-params]
          :cljs [pagination/pagination
                 (<sub [::subs/current-page])
                 (<sub [::subs/pagination])
                 (when-not preset-search? :jobsboard)
                 (<sub [::subs/pagination-query-params])]))]))

(defn navigation [view-type query-params logged-in?]
  [components/navigation
   (merge
     {:view-type           view-type
      :on-list-type-change #?(:clj (fn [type]
                                     {:href (routes/path
                                              :jobsboard
                                              :query-params (assoc query-params "view-type" (name type)))})
                              :cljs (fn [type]
                                      {:on-click #(dispatch [:wh.events/nav--set-query-param
                                                             jobsboard/view-type-param (name type)])}))}
     (when-not logged-in?
       {:on-button-click (fn [type]
                           (interop/on-click-fn
                             (interop/show-auth-popup type [:jobsboard])))}))])

(defn jobsboard-page []
  (let [logged-in?             (<sub [:user/logged-in?])
        view-type              (<sub [::subs/view-type])
        preset-search?         false
        query-params           (<sub [:wh/query-params])
        jobs-loaded?           (seq (<sub [::subs/jobs]))
        promoted-jobs-present? (seq (<sub [::subs/promoted-jobs]))
        searching? #?(:cljs (<sub [:wh.search/searching?])
                      :clj     false)]
    [:div (merge {:class "main"}
                 (when jobs-loaded? {:data-test "jobs-loaded"}))
     (when-not logged-in?
       [header])

     [:div.search-results
      [navigation view-type query-params logged-in?]

      [:div (util/smc styles/page)
       [components/search-box]

       [:div (util/smc styles/search-results)
        (when (and promoted-jobs-present?
                   (not searching?))
          [components/promoted-jobs view-type])

        [:h3 (util/smc "search-result-count"
                       styles/jobs-section__title
                       (when searching? "skeleton"))
         (<sub [::subs/result-count-str])]

        [jobs-board
         (merge {:view-type view-type}
                #?(:clj {:route        :jobsboard
                         :query-params query-params}
                   :cljs {:preset-search? preset-search?}))]]]]]))


(defn preset-search-page []
  (let [view-type      (<sub [::subs/view-type])
        query-params   (<sub [:wh/query-params])
        searching?     #?(:cljs (<sub [:wh.search/searching?])
                          :clj false)
        logged-in?     (<sub [:user/logged-in?])
        preset-search? true]
    [:div.main
     [header {:preset-search? preset-search? :searching? searching?}]

     [:div.search-results
      [navigation view-type query-params logged-in?]

      [:h3 (util/smc "search-result-count"
                     (when searching? "skeleton"))
       (<sub [::subs/result-count-str])]
      ;; `nil` route means use current
      [jobs-board
       {:route          nil
        :preset-search? true
        :query-params   query-params
        :view-type      view-type}]]]))
