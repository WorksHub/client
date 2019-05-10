(ns wh.logged-in.personalised-jobs.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [wh.components.cards.views :refer [job-card]]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
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
    (let [parts (partition-all 3 (<sub [::subs/jobs]))]
      (cond
        (seq parts) (conj (vec (for [part parts]
                                 (into [:div.columns]
                                       (for [job part]
                                         [:div.column.is-4
                                          (if (= type-of-jobs :liked)
                                            [job-card job :public (<sub [::subs/show-public-only?])]
                                            [job-card job :on-close :reload-recommended :public (<sub [::subs/show-public-only?])])]))))
                          [:div.columns.is-centered.load-more-section
                           [:div.column.is-4.has-text-centered
                            (when (<sub [::subs/show-load-more?])
                              [:button.button {:on-click #(dispatch [::events/load-more type-of-jobs])} "Load more Jobs"])]])
        :else (case type-of-jobs
                :recommended [[:p "Add some skills and preffered locations to your profile to see recommendations."]]
                :liked [[:p "Click on some " [icon "like" :class "like red-fill"] " to save jobs you like."]]
                [[:p "No jobs found."]])))))
