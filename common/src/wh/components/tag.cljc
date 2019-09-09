(ns wh.components.tag
  (:require
    [wh.components.icons :as icons]
    [wh.util :as util]))

(defn ->tag
  [m]
  (-> m
      (update :type keyword)
      (util/update-in* [:subtype] keyword)
      (cond-> (nil? (:subtype m)) (dissoc :subtype))))

(defn tag->form-tag
  [{:keys [id label type] :as tag}]
  (merge tag {:tag label
              :key id
              :class (str "tag--type-" (name type))
              :selected false}))

(defn tag
  [element-type {:keys [label type subtype id icon] :as t}]
  [element-type
   {:key id
    :class (util/merge-classes "tag"
                               (str "tag--type-" (if t (name type) "skeleton"))
                               (when subtype (str "tag--subtype-" (name subtype))))}
   (when icon
     [icons/icon icon])
   [:span label]])

(defn tag-list
  [tags]
  (when (not-empty tags)
    (into [:ul.tags.tags--inline.tags--profile]
          (map (fn [t] [tag :li t]) tags))))
