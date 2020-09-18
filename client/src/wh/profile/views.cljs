(ns wh.profile.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.components.not-found :as not-found]
    [wh.logged-in.profile.components :as components]
    [wh.components.loader :refer [loader-full-page]]
    [wh.profile.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn page []
  (cond
    (<sub [::subs/error?])
    [not-found/not-found-profile]
    (<sub [::subs/loader?])
    [loader-full-page]
    :else
    [components/container
     [components/profile (<sub [::subs/profile])
      {:twitter (<sub [::subs/social :twitter])
       :stackoverflow (<sub [::subs/social :stackoverflow])
       :github (<sub [::subs/social :github])}
      :public]
     [components/content
      [components/section-skills (<sub [::subs/skills]) :public]
      [components/section-articles (<sub [::subs/blogs]) :public]
      [components/section-issues (<sub [::subs/issues]) :public]]]))
