(ns wh.components.stat-card
  (:require [wh.components.icons :refer [icon]]
            [wh.routes :as routes]
            [wh.styles.stat-card :as styles]
            [wh.verticals :as verticals]))

(defn button [{:keys [text href]}]
  [:a {:class styles/button :href href} text])

(defn text [{:keys [text]}]
  [:div {:class styles/text} text])

(defn chart [{:keys [icon-name percentage]}]
  [:div {:class styles/chart}
   (when percentage [:div {:class styles/chart--percentage} percentage])
   [icon icon-name :class styles/chart--icon]])

(defn about-applications [vertical]
  [:div {:class styles/card}
   [chart {:icon-name "stat-card-applications" :percentage "62%"}]
   [text {:text (str "62% of applicants who applied through " (verticals/config vertical :platform-name) " were hired within 3-6 weeks")}]
   [button {:text "See all jobs" :href (routes/path :jobsboard)}]])

(defn about-open-source [_vertical]
  [:div {:class styles/card}
   [chart {:icon-name "stat-card-issues" :percentage "88%"}]
   [text {:text "88% of our companies encourage their engineers to make Open Source contributions"}]
   [button {:text "See open source issues" :href (routes/path :issues)}]])

(defn about-salary-increase [vertical]
  [:div {:class styles/card}
   [chart {:icon-name "stat-card-salary"}]
   [text {:text (str "Engineers hired through " (verticals/config vertical :platform-name) " can typically achieve a 10-15% salary increase")}]
   [button {:text "See all jobs" :href (routes/path :jobsboard)}]])
