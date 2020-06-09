(ns wh.components.attract-card
  (:require [clojure.string :as str]
            [wh.common.data :as data]
            [wh.components.branding :as branding]
            [wh.components.icons :refer [icon]]
            [wh.styles.attract-card :as style]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn intro
  [vertical]
  [:div (util/smc style/intro)
   [:div (util/smc style/intro__branding)
    [icon vertical :class style/intro__icon]
    [branding/vertical-title vertical
     {:size :small :multiline? true}]]
   [:p (util/smc style/intro__description)
    [:span (str (get-in data/in-demand-hiring-data [vertical :discover]) " with ")]
    [:span (util/smc style/intro__vertical-title) (verticals/config vertical :platform-name)]]])
