(ns wh.components.branding
  (:require [clojure.string :as str]
            [wh.styles.branding :as branding-style]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn vertical-title
  [vertical {:keys [size multiline?]
             :or   {size :medium}}]
  [:div (util/smc branding-style/vertical-title
                  (when multiline? branding-style/vertical-title--multiline)
                  (case size
                    :small  branding-style/vertical-title--small
                    :medium branding-style/vertical-title--medium))
   (map #(vector :span {:key % :class branding-style/vertical-title__line} %)
        (str/split (if (= "www" vertical) "Works Hub"
                       (verticals/config vertical :platform-name)) #"\s"))])
