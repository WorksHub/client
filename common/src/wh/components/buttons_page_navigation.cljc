(ns wh.components.buttons-page-navigation
  (:require [re-frame.core :refer [dispatch]]
            [wh.common.data :as data]
            [wh.components.branding :as branding]
            [wh.components.icons :as icons]
            [wh.components.signin-buttons :as signin-button]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.components.buttons-page-navigation-styles :as styles]
            [wh.interop :as interop]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn- button [{:keys [text on-click href icon active? tag type]
                :or   {tag :a}}]
  (let [classes (util/mc styles/button
                         [active? styles/button--active]
                         [(= type :filters) styles/button--filters])]
    [tag (cond
           (and (not active?) on-click)
           (merge (interop/on-click-fn on-click)
                  {:class classes})
           ;;
           (and (not active?) href)
           {:href  href
            :class classes}
           ;;
           :else
           {:href  "#"
            :class classes})
     [icons/icon icon :class styles/button__icon]
     [:span text]]))

(defn filters-toggle []
  [:div {:class styles/filters-toggle}
   [button]])

(defn buttons [button-descriptions]
  [:div {:class styles/buttons}
   (map (fn [description]
          ^{:key (:icon description)}
          [button description])
     button-descriptions)])

(defn add-on-click-for-guest [logged-in? type]
  (when-not logged-in?
    (interop/show-auth-popup type [:jobsboard])))

(defn buttons-jobsboard [{:keys [logged-in?]}]
  [buttons [{:text    "All jobs"
             :active? true
             :icon    "board-rectangles"}
            {:text     "Recommended jobs"
             :href     (routes/path :recommended)
             :on-click (add-on-click-for-guest logged-in? :jobsboard-recommended)
             :icon     "robot-face"}
            {:text     "Saved jobs"
             :href     (routes/path :liked)
             :on-click (add-on-click-for-guest logged-in? :jobsboard-save)
             :icon     "save"}
            {:text     "Applied to"
             :href     (routes/path :applied)
             :on-click (add-on-click-for-guest logged-in? :jobsboard-applied)
             :icon     "document-filled"}
            {:text     "Filters"
             :type     :filters
             :icon     "filters"
             :tag      :button
             :on-click (interop/toggle-jobs-filters-display)}]])


(defn buttons-articles []
  (let [logged-in? (<sub [:user/logged-in?])
        page       (<sub [:wh/page])]
    [buttons [{:text    "All articles"
               :active? (= page :learn)
               :href    (routes/path :learn)
               :icon    "document"}
              {:text     "Saved articles"
               :href     (routes/path :liked-blogs)
               :on-click (when-not logged-in?
                           (interop/show-auth-popup :save-blog [:liked-blogs]))
               :active?  (= page :liked-blogs)
               :icon     "save"}
              {:text     "Write an article"
               :href     (routes/path :contribute)
               :on-click (when-not logged-in?
                           (interop/show-auth-popup :contribute [:contribute]))
               :icon     "plus-circle"}]]))
