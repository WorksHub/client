(ns wh.activities.views
  (:require [wh.activities.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.stream-preview :as styles]))

(defn preview []
  (let [activities (<sub [::subs/activities])]
    [:div.main
     [:h1 "Activities"]

     [:ul
      (for [{:keys [id verb actor object object-type date] :as act} activities]
        [:li {:class styles/activity :key id}
         [:p {:class styles/date} (.toLocaleString (js/Date. date))]
         [:p
          [:span {:class styles/actor} actor]
          [:span " did "]
          [:span verb]
          [:span (str " " object-type ": ")]
          [:span {:class styles/object-title}
           (or (:title object) (:name object))]
          [:span (str ", with id: " id)]]])]]))
