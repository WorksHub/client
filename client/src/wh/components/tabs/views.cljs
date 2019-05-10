(ns wh.components.tabs.views
  (:require
    [reagent.core :as r]
    [wh.components.icons :refer [icon]]))

(defn tabs
  [_selected-tab _tbs _on-click]
  (let [collapsed? (r/atom true)]
    (fn [selected-tab tbs on-click]
      [:ul.tabs
       {:class (when-not @collapsed? "tabs--expanded")
        :style (when-not @collapsed? {:height (str (* (count tbs) 40) "px")})}
       (for [[id label :as t] tbs]
         ^{:key id}
         [:li.tab
          {:class (when (= id selected-tab) "tab--selected")
           :on-click #(do
                        (reset! collapsed? true)
                        (when on-click (on-click id)))}
          label])
       [:div.tab__roll-down
        {:on-click #(do (swap! collapsed? not)
                        (.stopPropagation %))
         :class (when-not @collapsed? "tab__roll-down--expanded")}
        [icon "roll-down"]]])))
