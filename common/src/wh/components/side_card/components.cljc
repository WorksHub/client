(ns wh.components.side-card.components
  (:require [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [clojure.spec.alpha :as s]
            [wh.common.text :as text]
            [wh.components.common :refer [wrap-img img]]
            [wh.components.icons :as icons]
            [wh.components.skeletons.components :as skeletons]
            [wh.components.tag :as tag]
            [wh.routes :as routes]
            [wh.styles.side-card :as style]
            [wh.util :as util]))

(defn card-tags [tags type]
  [tag/tag-list
   :a
   (->> tags
        (take 3)
        (map #(assoc % :href
                     (case type
                       :company (routes/path :companies
                                             :query-params
                                             {:tag         (str (:slug %)
                                                                ":"
                                                                (name (:type %)))
                                              :interaction 1})
                       :article (routes/path :learn-by-tag
                                             :params {:tag (:slug %)}))
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
     (wrap-img img img-src {:w 32 :h 32 :class img-class})
     [:div {:class style/connected-entity__info}
      [:span {:class title-class} title]
      [:span {:class (util/mc style/connected-entity__title style/connected-entity__title--minor)} subtitle]]]))

(defn section-title [title]
  [:h3 {:class style/section__title} title])

(defn section-elements
  ([elements card-component]
   (section-elements elements card-component nil))

  ([elements card-component opts]
   [:div (cond-> {:class style/section__elements}
                 opts (merge opts))
    (for [elm elements]
      ^{:key (:id elm)}
      [card-component elm])]))

(defn card-skeleton
  ([]
   (card-skeleton {:type :default}))
  ([{:keys [type]}]
   [:section {:class style/section__element}
    [skeletons/image-with-info]
    [skeletons/title]
    (when (= :tags type)
      [skeletons/tags 3])]))

(defn section-elements-skeleton [{:keys [type]}]
  [:div {:class style/section__elements}
   (for [i (range 3)]
     ^{:key i}
     [card-skeleton {:type type}])])

(defn numeric-info [lines]
  [:div {:class style/numeric-info}
   (for [{:keys [number sing plural icon]} lines]
     (when (and number (pos? number))
       [:div {:key   icon
              :class style/numeric-info__line}
        [icons/icon icon :class style/icon]
        (str number " " (text/pluralize number sing plural))]))])

(defn section-button [{:keys [title href type on-click text size]
                       :or   {text :capitalize size :default}}]
  [(if href :a :button)
   (merge {:class (util/mc style/button
                           [(= :capitalize text) style/button--capitalize]
                           [(= :small size) style/button--small]
                           (case type
                             :no-border style/button--text
                             :dark      style/button--dark
                             :filled    style/button--filled
                             nil))}
          (when href {:href href})
          (when on-click on-click))
   title])

(defn card-link [{:keys [title href data-test]}]
  [:a (cond-> {:href  href
               :class style/element__link}
              data-test (merge {:data-test data-test}))
   title])
