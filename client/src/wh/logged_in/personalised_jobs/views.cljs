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

(defn job-type-data
  [type-of-jobs]
  (case type-of-jobs
    :recommended {:on-close :reload-recommended
                  :sub [::subs/recommended-jobs]
                  :message [[:p "Add some skills and preferred locations to your profile to see recommendations."]]}
    :liked       {:on-close :reload-liked
                  :sub [::subs/liked-jobs]
                  :message [[:p "Click on some " [icon "bookmark" :class "like red-fill"] " to save jobs you like."]]}
    :applied     {:sub [::subs/applied-jobs]
                  :message [[:p "You haven't applied for any jobs yet... " [link "What are you waiting for?" :jobsboard :class "a--underlined"] "."]]}))

(def titles {:liked       "Saved"
             :recommended "Recommended"
             :applied     "Applied"})

(defn page [type-of-jobs]
  (into
    [:div.main
     [:div.spread-or-stack
      [:h1 (str (titles type-of-jobs) " Jobs")]
      (when (= type-of-jobs :recommended)
        [:div.has-bottom-margin
         [link [:button.button "Improve recommendations"] :improve-recommendations :class "level-item"]])]]
    (let [{:keys [on-close sub message]} (job-type-data type-of-jobs)
          parts                          (partition-all 3 (<sub sub))
          has-applied?                   (some? (<sub [:user/applied-jobs]))]
      (cond
        (seq parts) (conj (vec (for [part parts]
                                 (into [:div.columns]
                                       (for [job part]
                                         [:div.column.is-4
                                          [job-card job (merge {:user-has-applied? has-applied?
                                                                :logged-in?        true
                                                                :apply-source      (str "personalized-jobs-" (name type-of-jobs) "-job")}
                                                               (when on-close
                                                                 {:on-close on-close}))]]))))
                          [:div.columns.is-centered.load-more-section
                           [:div.column.is-4.has-text-centered
                            (when (<sub [::subs/show-load-more?])
                              [:button.button {:on-click #(dispatch [::events/load-more type-of-jobs])} "Load more Jobs"])]])
        :else       (or message [[:p "No jobs found."]])))))
