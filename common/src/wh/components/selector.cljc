(ns wh.components.selector
  (:require [wh.util :as util]))

(defn selector
  [selected-tab tbs on-click]
  [:ul.selector
   {}
   (for [[id label :as t] tbs]
     ^{:key id}
     [:li
      (merge {:class (util/merge-classes "selector__option"
                                         (when (= id selected-tab) "selector__option--selected"))}
             #?(:cljs {:on-click #(when on-click (on-click id))}))
      label])])
