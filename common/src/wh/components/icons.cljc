(ns wh.components.icons
  (:require
    [wh.common.url :as url]
    [wh.util :as util]))

(defn icon [name & {:keys [tooltip] :as opts}]
  (let [svg [:svg (-> (dissoc opts :title :tooltip)
                      (assoc :class (util/merge-classes (:class opts) "icon" (str "icon--" name))))
             [:use {#?(:clj :xlink:href) #?(:cljs :xlink-href) (str "#" name)}]]]
    (if tooltip
      [:div.icon__container
       [:span.icon__tooltip tooltip]
       svg]
      svg)))

(defn type->icon-name [type]
  (case type
    :twitter "twitter-circle"
    :linkedin "linkedin-circle"
    (name type)))

(defn url-icons [other-urls class]
  (into
   [:div {:class class}]
   (for [{:keys [url type]} (url/detect-urls-type other-urls)]
     [:a
      {:href url
       :target "_blank"
       :rel "noopener"
       :class "url-icon"}
      [icon (type->icon-name type) :class (str class "__icon")]])))
