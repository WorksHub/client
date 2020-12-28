(ns wh.promotions.create-promotion.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.promotions.create-promotion.events :as events]
            [wh.promotions.create-promotion.feed.views :as feed]
            [wh.promotions.create-promotion.jobsboard.views :as jobsboard]
            [wh.promotions.create-promotion.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))


(defn- promote-on-feed []
  [:<>
   [:h1 "Post a promotion to Feed"]

   [feed/promote]])

(defn- promote-on-feed-jobsboard []
  (let [selected-channel   (<sub [::subs/selected-channel])
        jobsboard-selected (= :jobsboard selected-channel)
        feed-selected      (= :feed selected-channel)]
    [:<>
     [:div (util/smc styles/create-promotion__select-channel)
      [:h1 "Post a promotion"]

      [:a {:href     "#"
           :on-click #(dispatch [::events/select-channel :feed])
           :class    (util/mc styles/create-promotion__select-channel__title
                              [feed-selected styles/create-promotion__select-channel__title--active])}
       "to Feed"]

      [:a {:href     "#"
           :on-click #(dispatch [::events/select-channel :jobsboard])
           :class    (util/mc styles/create-promotion__select-channel__title
                              [jobsboard-selected styles/create-promotion__select-channel__title--active])}
       "to Jobsboard"]]

     (cond
       feed-selected      [feed/promote]
       jobsboard-selected [jobsboard/promote])]))

(defn page []
  (let [object-type (<sub [::subs/object-type])]
    [:div.main
     (if (= "job" object-type)
       [promote-on-feed-jobsboard]

       [promote-on-feed])]))
