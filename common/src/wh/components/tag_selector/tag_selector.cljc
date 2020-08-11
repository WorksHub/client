(ns wh.components.tag-selector.tag-selector
  (:require [clojure.string :as str]
            [wh.common.subs] ;;required for query params sub
            [wh.common.text :as text]
            [wh.components.icons :refer [icon]]
            [wh.components.skeletons.components :as skeletons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.tag-selector :as styles]
            [wh.util :as util]))

(defn tag->query-params-tag-id [tag]
  "Takes tag and returns string representation of this tag for query params"
  (str (name (:slug tag)) ":" (name (:type tag))))

(defn query-params->query-params-tags-slugs [query-params]
  (if-let [query-params-tags (get query-params "tags")]
    (str/split query-params-tags #",")
    []))

(defn query-params-tag-slug->partial-tag [query-params-tag-id]
  (let [[slug type] (str/split query-params-tag-id #":")]
    {:slug slug
     :type type}))

(defn tag-selected? [query-params-tags-slugs tag]
  (let [tag-id (tag->query-params-tag-id tag)]
    (some #(= % tag-id) query-params-tags-slugs)))

(defn reset-icon []
  [icon "reset" :class styles/reset-button__icon])


(defn selector [tags query-params-tags-slugs {:keys [admin? company?]}]
  [:div
   [tag/tag-list
    :button
    (map (fn [tag]
           (assoc tag
                  :on-click #?(:clj  (format "toggleTagAndRedirect('%s');" (tag->query-params-tag-id tag))
                               :cljs #(dispatch [:wh.events/nav--set-query-params
                                                 {"tags"
                                                  (->> (tag->query-params-tag-id tag)
                                                       (util/toggle (set query-params-tags-slugs))
                                                       (str/join ",")
                                                       (text/not-blank))

                                                  "older-than" nil
                                                  "newer-than" nil}]))
                  :interactive? true
                  :inverted? (tag-selected? query-params-tags-slugs tag)
                  :server-side-invert-on-click? true))
         tags)]
   [:div {:class styles/status}
    [:span {:class styles/selected-counter} (str (count query-params-tags-slugs) " selected")]
    (let [path (if (or admin? company?)
                 (routes/path :feed)
                 (routes/path :homepage))]
      [:a {:class styles/reset-button
           :href  path}
       "Reset selection" [reset-icon]])]])

(defn card-with-selector [tags loading? user-types]
  [:div {:class styles/card}
   [:div {:class styles/title}
    "Show me more of..."]
   (if loading?
     [skeletons/tags 14]
     [selector
      tags (query-params->query-params-tags-slugs (<sub [:wh/query-params]))
      user-types])])
