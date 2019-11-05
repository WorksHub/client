(ns wh.logged-in.personalised-jobs.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
    [wh.components.job :refer [job-card]]
    [wh.logged-in.personalised-jobs.events :as events]
    [wh.logged-in.personalised-jobs.subs :as subs]
    [wh.subs :refer [<sub]]))

(defn page [type-of-jobs]
  (into
    [:div.main
     [:div.spread-or-stack
      [:h1 (str (-> type-of-jobs name str/capitalize)) " Jobs"]
      (when (= type-of-jobs :recommended)
        [:div.has-bottom-margin
         [link [:button.button "Improve recommendations"] :improve-recommendations :class "level-item"]])]]
    (let [parts (partition-all 3 (<sub [::subs/jobs]))
          has-applied? (some? (<sub [:user/applied-jobs]))]
      (cond
        (seq parts) (conj (vec (for [part parts]
                                 (into [:div.columns]
                                       (for [job part]
                                         [:div.column.is-4
                                          [job-card job (merge {:user-has-applied? has-applied?
                                                                :logged-in? true}
                                                               (when (= type-of-jobs :recommended)
                                                                 {:on-close :reload-recommended})
                                                               (when (= type-of-jobs :liked)
                                                                 {:on-close :reload-liked}))]]))))
                          [:div.columns.is-centered.load-more-section
                           [:div.column.is-4.has-text-centered
                            (when (<sub [::subs/show-load-more?])
                              [:button.button {:on-click #(dispatch [::events/load-more type-of-jobs])} "Load more Jobs"])]])
        :else (case type-of-jobs
                :recommended [[:p "Add some skills and preffered locations to your profile to see recommendations."]]
                :liked [[:p "Click on some " [icon "like" :class "like red-fill"] " to save jobs you like."]]
                :applied [[:p "You haven't applied for any jobs yet... " [link "What are you waiting for?" :jobsboard :class "a--underlined"] "."]]
                [[:p "No jobs found."]])))))
