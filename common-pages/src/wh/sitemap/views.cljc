(ns wh.sitemap.views
  (:require
    [wh.sitemap.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]
    [wh.verticals :as verticals]))

(defn sitemap-page [vertical links]
  [:div.main
   [:section.sitemap
    [:h1 (verticals/config vertical :platform-name)]
    [:section
     [:h2 "Jobs by tech"]
     [:div.sitemap__container
      (for [{:keys [title url]} (:tech links)]
        [:span.sitemap__link [:a {:href url} title]])]]
    [:section
     [:h2 "Jobs by location"]
     [:div.sitemap__container
      (for [{:keys [title url]} (:location links)]
        [:span.sitemap__link [:a {:href url} title]])]]
    [:section
     [:h2 "Articles"]
     [:div.sitemap__container
      (for [{:keys [title url]} (:articles links)]
        [:span.sitemap__link [:a {:href url} title]])]]]])

(defn www-sitemap-page [links]
  [:div.main
   [:section.sitemap
    [:h1 (verticals/config "www" :platform-name)]
    [:section
     [:h2 "Companies"]
     [:div.sitemap__container
      (for [{:keys [title url]} (:companies links)]
        [:span.sitemap__link [:a {:href url} title]])]]
    [:section
     [:h2 "Jobs by tech"]
     (for [[_vertical l] (:tech links)]
       [:div
        [:h3.sitemap__vertical [:a {:href (:vertical-link l)} (:vertical-title l)]]
        [:div.sitemap__container
         (for [{:keys [title url]} (:sitemap-links l)]
           [:span.sitemap__link [:a {:href url} title]])]])]
    [:section
     [:h2 "Jobs by location"]
     (for [part (:location links)]
       [:div
        [:h3.sitemap__vertical [:a {:url part} (:title part)]]
        [:div.sitemap__container
         (for [l (:links part)]
           [:span.sitemap__link [:a {:href (:url l)} (:title l)]])]])]
    [:section
     [:h2 "Articles"]
     (for [[_vertical l] (:articles links)]
       [:div
        [:h3.sitemap__vertical [:a {:href (:vertical-link l)} (:vertical-title l)]]
        [:div.sitemap__container
         (for [{:keys [title url]} (:sitemap-links l)]
           [:span.sitemap__link [:a {:href url} title]])]])]]])

(defn page []
  (let [vertical (<sub [:wh/vertical])
        sitemap  (<sub [:wh/sitemap])]
    (if (= "www" vertical)
      (www-sitemap-page sitemap)
      (sitemap-page vertical (sitemap vertical)))))
