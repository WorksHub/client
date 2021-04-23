(ns wh.components.buttons-page-navigation
  (:require [re-frame.core :refer [dispatch]]
            [wh.common.data :as data]
            [wh.components.branding :as branding]
            [wh.components.icons :as icons]
            [wh.components.signin-buttons :as signin-button]
            [wh.routes :as routes]
            [wh.styles.buttons-page-navigation :as styles]
            [wh.interop :as interop]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn- button [{:keys [text on-click href icon active?]}]
  (let [classes (util/mc styles/button
                         [active? styles/button--active])]
    [:a (cond
          on-click (merge (interop/on-click-fn on-click)
                          {:class classes})
          href {:href  href
                :class classes}
          :else {:href  "#"
                 :class classes})
     [icons/icon icon :class styles/button__icon]
     [:span text]]))

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
             :icon     "document-filled"}]])

(defn buttons-articles [{:keys [logged-in?]}]
  [buttons [{:text    "All articles"
             :active? true
             :icon    "document"}
            {:text     "Write an article"
             :href     (routes/path :contribute)
             :on-click (when-not logged-in?
                         (interop/show-auth-popup :contribute [:publish]))
             :icon     "plus-circle"}]])
