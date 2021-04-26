(ns wh.blogs.liked.views
  (:require
    [wh.blogs.liked.subs :as subs]
    [wh.blogs.learn.components :as learn-components]
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
     [:div "You don't have bookmarked blogs"]
     ;;
     :else
     (for [blog blogs]
       ^{:key (:id blog)}
       [learn-components/blog-comp ctx blog]))])

(defn page []
  [:div {:class     styles/page
         :data-test "page"}
   (let [{:keys [blogs loading?]} (<sub [::subs/liked-blogs])]
     [:div {:class styles/page-columns}
      [:div {:class styles/main-column}
       [blog-list-comp
        {:loading?    loading?
         :test?       true}
        blogs]]
      [:div.is-hidden-mobile]])])