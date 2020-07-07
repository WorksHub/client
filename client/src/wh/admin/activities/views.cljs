(ns wh.admin.activities.views
  (:require [reagent.core :as r]
            [wh.admin.activities.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.stream-preview :as styles]
            [wh.util :as util]))

(defn verb-style
  [verb]
  (case verb
    "publish"   styles/activity--publish
    "highlight" styles/activity--highlight
    nil))

(defn activity
  [_activity]
  (let [expanded? (r/atom false)]
    (fn [{:keys [id verb object object-type date] :as act}]
      [:li {:class (util/mc styles/activity (verb-style verb)) :key id}
       [:div (util/smc styles/header)
        [:p {:class styles/date} (str (.toLocaleString (js/Date. date)) " | "
                                      verb " | "
                                      object-type " ("
                                      (or (or (:title object) (:name object))) ")")]
        [:a {:class    styles/show-hide
             :href     "#"
             :on-click #(println (swap! expanded? not))}
         (if @expanded? "Hide" "Show")]]

       (when @expanded?
         [:div
          [:pre (str (dissoc act :object))]
          [:pre (str (select-keys act [:object]))]])])))

(defn preview []
  (let [expanded? (r/atom #{})]
    (fn []
      (let [activities (<sub [::subs/activities])]
        [:div.main
         [:h1 "Activities"]
         [:ul
          (for [a activities]
            [activity a])]]))))
