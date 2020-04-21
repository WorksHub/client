(ns wh.components.info-icon
  (:require #?(:cljs ["react-tooltip" :as ReactTooltip])
            #?(:cljs [reagent.core :as reagent])
            [wh.components.icons :refer [icon]]))

#?(:cljs
   (def react-tooltip (reagent/adapt-react-class ReactTooltip)))

(defn info-icon
  [id txt]
  #?(:cljs
     (let [tt-id (str "info-icon-"(name id))]
       [:div.info-icon.has-tooltip
        [:a {:data-tip true
             :data-for tt-id}
         [icon "info"]]
        [react-tooltip {:id tt-id :place "top" :type "light" :effect "solid"
                        :class-name "tooltip"}
         txt]])))
