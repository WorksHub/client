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

;; first element is the on-close map, the second element is the relevant job pool , and the third element is the message if page would be empty.
(defn job-type-data
  [type-of-jobs]
  (case type-of-jobs
    :recommended [{:on-close :reload-recommended}
                  [::subs/recommended-jobs]
                  [[:p "Add some skills and preffered locations to your profile to see recommendations."]]]
    :liked [{:on-close :reload-liked}
            [::subs/liked-jobs]
            [[:p "Click on some " [icon "like" :class "like red-fill"] " to save jobs you like."]]]
    :applied [{:on-close :none}
              [::subs/applied-jobs]
              [[:p "You haven't applied for any jobs yet... " [link "What are you waiting for?" :jobsboard :class "a--underlined"] "."]]]))

(defn page [type-of-jobs]
  (into
    [:div.main
     [:div.spread-or-stack
      [:h1 (str (-> type-of-jobs name str/capitalize)) " Jobs"]
      (when (= type-of-jobs :recommended)
        [:div.has-bottom-margin
         [link [:button.button "Improve recommendations"] :improve-recommendations :class "level-item"]])]]
    ;; data stops us from having to do three case checks , by putting all relevant data per keyword into a vector.
    (let [data (job-type-data type-of-jobs)
          on-close (first data)
          subscription (second data)
          message (last data)
          parts (partition-all 3 (<sub subscription))
          has-applied? (some? (<sub [:user/applied-jobs]))]
      (cond
        (seq parts) (conj (vec (for [part parts]
                                 (into [:div.columns]
                                       (for [job part]
                                         [:div.column.is-4
                                          [job-card job (merge {:user-has-applied? has-applied?
                                                                :logged-in? true}
                                                               on-close)]]))))
                          [:div.columns.is-centered.load-more-section
                           [:div.column.is-4.has-text-centered
                            (when (<sub [::subs/show-load-more?])
                              [:button.button {:on-click #(dispatch [::events/load-more type-of-jobs])} "Load more Jobs"])]])
        :else (if message
                message
                [[:p "No jobs found."]])))))
