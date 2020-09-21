(ns wh.profile.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.components.loader :refer [loader-full-page]]
    [wh.components.not-found :as not-found]
    [wh.logged-in.profile.components :as components]
    [wh.profile.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn page []
  (let [articles (<sub [::subs/blogs])
        issues   (<sub [::subs/issues])]
    (cond
      (<sub [::subs/error?])
      [not-found/not-found-profile]
      (<sub [::subs/loader?])
      [loader-full-page]
      :else
      [components/container
       [components/profile (<sub [::subs/profile])
        {:twitter       (<sub [::subs/social :twitter])
         :stackoverflow (<sub [::subs/social :stackoverflow])
         :github        (<sub [::subs/social :github])}
        :public]
       [components/content
        [components/section-stats {:is-owner?       false
                                   :percentile      (<sub [::subs/percentile])
                                   :created         (<sub [::subs/created])
                                   :articles-count (count articles)
                                   :issues-count   (count issues)}]
        [components/section-skills (<sub [::subs/skills]) :public]
        [components/section-articles articles :public]
        [components/section-issues issues :public]]])))
