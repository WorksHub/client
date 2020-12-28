(ns wh.promotions.create-promotion.jobsboard.views
  (:require [wh.components.job :as job]
            [wh.common.url :as url]
            [wh.promotions.create-promotion.components :as components]
            [wh.promotions.create-promotion.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn- preview []
  (let [object      (<sub [::subs/object])
        vertical    (<sub [:wh/vertical])
        environment (<sub [:wh/env])]
    [:div
     [:h3 "Promotion preview"]

     (cond
       (= object :not-allowed)
       [:h4 "The object you're trying to promote is not public right now. You cannot promote it."]

       (= object :not-recognized)
       [:h4 "Something wrong happened. Please contact dev-team"]

       (map? object)
       [job/job-card
        object
        {:logged-in? true
         :base-uri   (url/vertical-homepage-href environment vertical)
         :vertical   vertical}])]))

(defn promote []
  [:div (util/smc styles/create-promotion)
   [preview]

   [components/send :jobs_board "Publish Promotion to Jobsboard"]])
