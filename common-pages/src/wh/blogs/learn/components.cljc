(ns wh.blogs.learn.components
  (:require [re-frame.core :refer [dispatch]]
            [wh.components.card-actions.components :as card-actions]
            [wh.components.common :as components-common]
            [wh.re-frame.subs :refer [<sub]]
            [wh.styles.blogs :as styles]
            [wh.components.icons :as icons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.blogs.like :as blogs-like]
            [wh.routes :as routes]
            [wh.slug :as slug]
            [wh.common.url :as url]
            [wh.util :as util]))

(defn rocket-icon []
  [:span (util/smc styles/upvotes__icon-wrapper)
   [icons/icon "rocketship" :class styles/upvotes__icon]])

(defn upvotes-comp [ctx {:keys [upvote-count] :as blog}]
  [:div (util/smc styles/upvotes)
   [rocket-icon]
   [:div (util/smc styles/secondary-text) upvote-count " boosts"]])

(defn date-and-read-time-comp [ctx {:keys [formatted-creation-date reading-time] :as blog}]
  [:div (util/smc styles/secondary-text) formatted-creation-date " • " reading-time " min read"])

(defn author-comp [ctx {:keys [author-info] :as blog}]
  (when author-info
    [:a {:class styles/author
         :href  (routes/path :user :params {:id (:id author-info)})}
     (when-let [image-url (:image-url author-info)]
       [:img {:class styles/author__avatar
              :src   image-url}])
     [:div (util/smc styles/secondary-text styles/author__name) (:name author-info)]]))

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
     [date-and-read-time-comp ctx blog]
     [upvotes-comp ctx blog]]))

(defn tags-comp [ctx blog]
  (cond
    (:loading? ctx)
    [tag/tag-list :a nil {:skeleton? true}]
    :else
    [tag/tag-list
     :a
     (->> (:tags blog)
          (map #(assoc % :href (routes/path :learn-by-tag :params {:tag (:slug %)}))))
     {:class         styles/blog__tag
      :class-wrapper styles/blog__tags}]))

(defn actions-comp
  [{:keys [test? logged-in? loading?] :as ctx}
   {:keys [id title author-info] :as blog}]
  (let [executing?      (<sub [::blogs-like/executing? blog])
        liked?          (<sub [::blogs-like/liked-by-user? blog])
        current-page    (<sub [:wh/page])
        vertical        (<sub [:wh/vertical])
        environment     (<sub [:wh/env])
        facebook-app-id (<sub [:wh/facebook-app-id])
        base-uri        (url/vertical-homepage-href environment vertical)]
    [:div (util/smc styles/blog__actions [(not test?) styles/blog__actions--hidden])
     [card-actions/actions
      (cond-> {:saved?        liked?
               :class-wrapper styles/blog__actions-wrapper
               :save-opts     (merge {:disabled executing?}
                                     (when-not loading?
                                       (if logged-in?
                                         {:on-click (if liked? #(dispatch [::blogs-like/unlike blog])
                                                               #(dispatch [::blogs-like/like blog]))}
                                         (interop/on-click-fn
                                           (interop/show-auth-popup
                                             :save-blog
                                             [current-page])))))}
        (:published blog) (assoc :share-opts {:url             (str (url/strip-path base-uri)
                                                                    (routes/path :blog :params {:id id}))
                                              :id              id
                                              :content-title   title
                                              :content         (str "'" title "'" " an article from " (:name author-info))
                                              :vertical        vertical
                                              :facebook-app-id facebook-app-id}))]
     [:button {:class (util/mc styles/button styles/button--inverted-highlighted)} "View Article"]]))


(defn blog-comp [ctx blog]
  [:div {:class     styles/blog
         :data-test "blog-info"}
   [:div (util/smc styles/blog__body)
    [title-comp ctx blog]
    [meta-comp ctx blog]
    [tags-comp ctx blog]]
   [image-comp ctx blog]
   [actions-comp ctx blog]])

;;

(defn tag-picker-comp [tags]
  [:div (util/smc styles/tag-selector)
   (tag/strs->tag-list :a tags
                       {:f #(assoc % :href (routes/path :learn-by-tag :params {:tag (slug/tag-label->slug (:label %))}))})])
