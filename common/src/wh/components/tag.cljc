(ns wh.components.tag
  (:require [wh.util :as util]))

(defn tag
  [element-type {:keys [label type subtype id]}]
  [element-type
   {:key id
    :class (util/merge-classes "tag"
                               (str "tag--type-" (name type))
                               (when subtype (str "tag--subtype-" (name subtype))))}
   label])
