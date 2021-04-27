(ns wh.blogs.learn.components
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.common :as components-common]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.blogs :as styles]
            [wh.components.icons :as icons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.blogs.like :as blogs-like]
            [wh.routes :as routes]
            [wh.slug :as slug]
            [wh.util :as util]))

(defn rocket-icon []
  [:span (util/smc styles/upvotes__icon-wrapper)
   [icons/icon "rocketship" :class styles/upvotes__icon]])

(defn upvotes-comp [ctx {:keys [upvote-count] :as blog}]
  [:div (util/smc styles/upvotes)
   [rocket-icon]
   [:div (util/smc styles/secondary-text) upvote-count " boosts"]])

(defn reading-time-comp [ctx {:keys [reading-time] :as blog}]
  [:div (util/smc styles/secondary-text) reading-time " min read"])

(defn creation-date-comp [ctx {:keys [formatted-creation-date] :as blog}]
  [:div (util/smc styles/secondary-text) formatted-creation-date])

(defn author-comp [ctx {:keys [author] :as blog}]
  [:div (util/smc styles/author)
   #_[:div (util/smc styles/author__avatar)]
   [:div (util/smc styles/secondary-text styles/author__name) author]])

(defn image-comp [ctx {:keys [feature published] :as blog}]
  (let [imgix-url (components-common/base-img feature)]
    (cond
      (:loading? ctx)
      [:div (util/smc styles/blog__image styles/blog__image--skeleton)]
      ;;
      (not published)
      [:div (util/smc styles/blog__image styles/blog__image--unpublished)
       [:img {:src   imgix-url
              :class styles/blog__image}]]
      ;;
      :else
      [:img {:src   imgix-url
             :class styles/blog__image}])))

(defn title-comp [ctx {:keys [id title] :as blog}]
  (cond
    (:loading? ctx)
    [:div (util/smc styles/blog__title styles/blog__title--skeleton)]
    :else
    [:a {:class     styles/blog__title
         :data-test "blog-title"
         :href      (routes/path :blog :params {:id id})} title]))

(defn meta-comp [ctx blog]
  (cond
    (:loading? ctx)
    [:div (util/smc styles/blog__meta styles/blog__meta--skeleton)]
    :else
    [:div (util/smc styles/blog__meta)
     [author-comp ctx blog]
     [reading-time-comp ctx blog]
     [upvotes-comp ctx blog]
     [creation-date-comp ctx blog]]))

(defn tags-comp [ctx blog]
  (cond
    (:loading? ctx)
    [tag/tag-list :a nil {:skeleton? true}]
    :else
    [tag/tag-list
     :a
     (->> (:tags blog)
          (map #(assoc % :href (routes/path :learn-by-tag :params {:tag (:slug %)}))))
     {:class styles/blog__tag}]))

(defn actions-comp
  [{:keys [test? logged-in? loading?] :as ctx} blog]
  (let [executing?   (<sub [::blogs-like/executing? blog])
        liked?       (<sub [::blogs-like/liked-by-user? blog])
        current-page (<sub [:wh/page])]
    [:div (util/smc styles/blog__actions [(not test?) styles/blog__actions--hidden])
     [:button (merge {:class    styles/blog__save
                      :disabled executing?}
                     (when-not loading?
                       (if logged-in?
                         {:on-click (if liked? #(dispatch [::blogs-like/unlike blog])
                                               #(dispatch [::blogs-like/like blog]))}
                         (interop/on-click-fn
                           (interop/show-auth-popup
                             :save-blog
                             [current-page])))))
      [icons/icon "save"
       :class (when liked? styles/blog__save--saved)]]]))


(defn blog-comp [ctx blog]
  [:div {:class     styles/blog
         :data-test "blog-info"}
   [image-comp ctx blog]
   [:div (util/smc styles/blog__body)
    [title-comp ctx blog]
    [meta-comp ctx blog]
    [tags-comp ctx blog]]
   [actions-comp ctx blog]])

;;

(defn tag-picker-comp [tags]
  [:div (util/smc styles/tag-selector)
   (tag/strs->tag-list :a tags
                       {:f #(assoc % :href (routes/path :learn-by-tag :params {:tag (slug/tag-label->slug (:label %))}))})])
