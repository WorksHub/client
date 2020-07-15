(ns wh.components.side-card.side-card-mobile
  (:require [wh.components.common :refer [link wrap-img img]]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.styles.side-card-mobile :as style]
            [wh.util :as util]))

(defn connected-entity [entity]
  [:div (util/smc style/connected-entity)
   (wrap-img img
             (or (:logo entity) (:image-url entity))
             {:w 16 :h 16 :class style/connected-entity__avatar})
   [:span (util/smc style/connected-entity__title)
    (:name entity)]])

(defn title
  ([text]
   [title text :default])
  ([text type]
   [:div (util/smc style/title
                   (when (= type :extended) style/title--extended))
    text]))

(defn view-button []
  [:div (util/smc style/button)
   "View"])

(defn blog-card [blog]
  [:a {:class style/card
       :href  (routes/path :blog :params {:id (:id blog)})}
   [:div (util/smc style/card__content)
    [title (:title blog) :extended]
    [connected-entity (:author-info blog)]]
   [view-button]])

(defn job-card [{:keys [slug company-info] :as job}]
  [:a {:class style/card
       :href  (routes/path :job :params {:slug slug})}
   [:div (util/smc style/card__content)
    [connected-entity company-info]
    [title (:title job)]]
   [view-button]])

(defn issue-card [issue]
  [:a {:class style/card
       :href  (routes/path :issue :params {:id (:id issue)})}
   [:div (util/smc style/card__content)
    [connected-entity (:company issue)]
    [title (:title issue) :extended]]
   [view-button]])

(defn toggle [icon text type]
  [:button (merge {:class style/toggle}
                  (interop/on-click-fn #?(:clj (format "toggleDisplay(\"%s\")" (name type))
                                          :cljs #(js/toggleDisplay (name type)))))
   [:div (util/smc style/toggle__title-wrapper)
    [icons/icon icon :class style/toggle__icon]
    [:div (util/smc style/toggle__title) text]]
   [:div (util/smc style/toggle__indicator) "Explore"]])

(defn trending-content-footer [{:keys [href text type]}]
  [:div (util/smc style/trending__footer)
   [:button (merge {:class style/trending__hide}
                   (interop/on-click-fn #?(:clj (format "toggleDisplay(\"%s\")" (name type))
                                           :cljs #(js/toggleDisplay (name type)))))
    "Hide"]
   [:a {:href  href
        :class style/trending__all-link}
    text]])

(defn top-content [{:keys [blogs jobs issues show-recommendations?]}]
  [:div (util/smc style/trending)
   [:span (util/smc style/trending__title) "Selected for you"]
   [:div (util/smc style/horizontal-scrolling)
    [toggle "case" (if show-recommendations? "Recommended jobs" "Hiring now") :jobs]
    [toggle "git" "Live open source issues" :issues]
    [toggle "document" "Trending articles" :blogs]]
   [:div
    [:div {:class style/trending__content
           :id    "trending-content-blogs"}
     (for [elm blogs]
       ^{:key (:id elm)}
       [blog-card elm])
     [trending-content-footer {:href (routes/path :learn)
                               :text "See all articles"
                               :type :blogs}]]
    [:div {:class style/trending__content
           :id    "trending-content-jobs"}
     (for [elm jobs]
       ^{:key (:id elm)}
       [job-card elm])
     [trending-content-footer {:href (routes/path :jobsboard)
                               :text "See all jobs"
                               :type :jobs}]]
    [:div {:class style/trending__content
           :id    "trending-content-issues"}
     (for [elm issues]
       ^{:key (:id elm)}
       [issue-card elm])
     [trending-content-footer {:href (routes/path :issues)
                               :text "See all issues"
                               :type :issues}]]]])
