(ns wh.components.skeletons.components
  (:require [wh.components.tag :as tag]
            [wh.styles.skeletons :as styles]
            [wh.util :as util]))

(defn image-with-info []
  [:div (util/smc styles/image-with-info)
   [:div (util/smc styles/image-with-info__logo)]
   [:div (util/smc styles/image-with-info__info1)]
   [:div (util/smc styles/image-with-info__info2)]])

(defn text []
  [:div (util/smc styles/paragraph)
   [:div (util/smc styles/paragraph__line)]
   [:div (util/smc styles/paragraph__line)]
   [:div (util/smc styles/paragraph__line styles/paragraph__line--short)]])

(defn button []
  [:div (util/smc styles/button)])

(defn title []
  [:div (util/smc styles/title)])

(defn tags
  ([]
   (tags 5))
  ([n]
   [tag/tag-list :li (repeat n nil)]))
