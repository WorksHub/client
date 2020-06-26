(ns wh.components.side-card.components
  (:require [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [clojure.string :as str]
            [wh.common.text :as text]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon] :as icons]
            [wh.components.tag :as tag]
            [wh.re-frame :as r]
            [wh.routes :as routes]
            [wh.styles.side-card :as style]
            [wh.util :as util]))

(defn card-tags [tags]
  [tag/tag-list :a (->> tags
                        (take 3)
                        (map #(assoc %
                                :href (routes/path :learn-by-tag :params {:tag (:slug %)})
                                :with-icon? false)))])

(defn card-tag [t]
  [tag/tag :div (assoc t :with-icon? false)])

(defn footer-link [{:keys [text bold-text href]}]
  [:a {:class style/footer__link :href href}
   text [:span {:class style/footer__bold-text} bold-text]])

(defn connected-entity [{:keys [title title-type subtitle href img-src img-type]}]
  (let [wrapper-tag   (if href :a :div)
        wrapper-class (cond-> style/connected-entity
                              href (util/mc style/connected-entity--link))
        img-class     (cond-> style/connected-entity__image
                              (= img-type :rounded) (util/mc style/connected-entity__image--rounded))
        title-class   (cond->
                        style/connected-entity__title
                        (= title-type :primary) (util/mc style/connected-entity__title--primary))]
    [wrapper-tag {:class wrapper-class
                  :href  href}
     [:img {:src img-src :class img-class}]
     [:div {:class style/connected-entity__info}
      [:span {:class title-class} title]
      [:span {:class (util/mc style/connected-entity__title style/connected-entity__title--minor)} subtitle]]]))

(defn section-title [title]
  [:h3 {:class style/section__title} title])

(defn section-elements [elements card-component]
  [:div {:class style/section__elements}
   (for [elm elements]
     ^{:key (:id elm)}
     [card-component elm])])

(defn numeric-info [lines]
  [:div {:class style/numeric-info}
   (for [{:keys [number sing plural icon]} lines]
     (when (and number (pos? number))
       [:div {:key   icon
              :class style/numeric-info__line}
        [icons/icon icon :class style/icon]
        (str number " " (text/pluralize number sing plural))]))])

(defn section-button [{:keys [title href type]}]
  [(if href :a :button)
   (merge {:class (cond-> style/button
                          (= :no-border type) (util/mc style/button--text))}
          (when href {:href href}))
   title])

(defn card-link [{:keys [title href]}]
  [:a {:href  href
       :class style/element__link}
   title])
