(ns wh.components.tag-selector.tag-selector
  (:require [wh.common.subs] ;;required for query params sub
            [wh.components.icons :refer [icon]]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.tag-selector :as styles]
            [clojure.string :as str]))

(defn tag->qp-tag-id [tag]
  "Takes tag and returns string representation of this tag for query params"
  (str (:slug tag) ":" (:type tag)))

(defn query-params->qp-tags-ids [query-params]
  (if-let [query-params-tags (get query-params "tags")]
    (str/split query-params-tags #",")
    []))

(defn tag-selected? [qp-tags-ids tag]
  (let [tag-id (tag->qp-tag-id tag)]
    (some #(= % tag-id) qp-tags-ids)))

(defn reset-icon []
  [icon "reset" :class styles/reset-button__icon])

(defn selector [tags qp-tags-ids]
  [:div
   [tag/tag-list :button (map (fn [tag]
                                (assoc tag
                                  :on-click #?(:clj (format "toggleTagAndRedirect('%s');" (tag->qp-tag-id tag))
                                               :cljs nil)
                                  :interactive? true
                                  :with-icon? false
                                  :inverted? (tag-selected? qp-tags-ids tag)
                                  :server-side-invert-on-click? true))
                              tags)]
   [:div {:class styles/status}
    [:span {:class styles/selected-counter} (str (count qp-tags-ids) " selected")]
    [:button (merge {:class styles/reset-button}
                    (interop/on-click-fn #?(:clj "removeSelectedTagsAndRedirect()"
                                            :cljs nil)))
     "Reset selection" [reset-icon]]]])

(defn card-with-selector [tags]
  [:div {:class styles/card}
   [:div {:class styles/title}
    "Show me more of..."]
   [selector tags (query-params->qp-tags-ids (<sub [:wh/query-params]))]])


