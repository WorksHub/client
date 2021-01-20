(ns wh.components.rich-text-field.loadable
  (:require [shadow.lazy :as lazy]
            [wh.re-frame :refer [atom]]))

(def rich-text-field-lazy (lazy/loadable wh.components.rich-text-field.core/rich-text-field))
(def default (fn [] [:div "Loading editor..."]))

(defn rich-text-field [_]
  (let [component (atom default)
        _promise   (-> (lazy/load rich-text-field-lazy)
                       (.then (fn [root-el]
                                (reset! component root-el))))]
    (fn [props]
      [@component props])))