(ns wh.blogs.liked.views
  (:require
    [wh.blogs.liked.subs :as subs]
    [wh.blogs.learn.components :as learn-components]
    [wh.components.buttons-page-navigation :as buttons-page-navigation]
    [wh.styles.blogs :as styles]
    [wh.re-frame.subs :refer [<sub]]))

(defn blog-list-comp [ctx blogs]
  [:div {:class styles/blog-list}
   (cond
     (:loading? ctx)
     (for [i (range 10)]
       ^{:key i}
       [learn-components/blog-comp ctx nil])
     ;;
     (empty? blogs)
     [:div {:class styles/not-found} "You don't have any saved blogs"]
     ;;
     :else
     (for [blog blogs]
       ^{:key (:id blog)}
       [learn-components/blog-comp ctx blog]))])

(defn page []
  [:div {:class     styles/page
         :data-test "page"}
   [:div {:class styles/navigation-buttons-wrapper}
    [buttons-page-navigation/buttons-articles]]
   (let [{:keys [blogs loading?]} (<sub [::subs/liked-blogs])]
     [:div {:class styles/page-columns}
      [:div {:class styles/main-column}
       [blog-list-comp
        {:loading?   loading?
         :logged-in? (<sub [:user/logged-in?])
         :test?      true}
        blogs]]
      [:div.is-hidden-mobile]])])
