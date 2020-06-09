(ns wh.landing-new.views
  (:require [wh.components.side-card.side-card :as side-cards]
            [wh.landing-new.subs :as subs]
            #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.landing :as styles]))

(defn page []
  (let [blogs (<sub [::subs/top-blogs])
        jobs (<sub [::subs/recent-jobs])
        users (<sub [::subs/top-users])
        issues (<sub [::subs/live-issues])
        companies (<sub [::subs/top-companies])]
    [:div {:class styles/container-all-cards}
     [side-cards/live-issues issues]
     [side-cards/top-ranking-users users]
     [side-cards/recent-jobs jobs]
     [side-cards/top-ranking {:blogs blogs
                              :companies companies}]]))
