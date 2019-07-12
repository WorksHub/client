(ns wh.components.issue
  (:require
    [clojure.string :as str]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.icons :refer [icon]]
    [wh.re-frame.events :refer [dispatch]]
    [wh.util :as util]))

(defn level->str
  [level]
  (case level
    :beginner     "Beginner"
    :intermediate "Intermediate"
    :advanced     "Advanced"
    "Unknown"))

(defn level->icon
  [level]
  (case level
    :beginner     "level1"
    :intermediate "level2"
    :advanced     "level3"
    "circle"))

(defn issue->status [{:keys [status pr-count contributors] :as issue}]
  (if (= :open status)
    (cond
      (and pr-count (pos? pr-count)) "submitted"
      (and contributors (pos? (count contributors))) "started"
      :else "open")
    "closed"))

(def derived-status->str
   {"open" "Issue Open"
   "started" "Work started"
   "submitted" "Work submitted"
   "closed" "Issue closed"})

(defn issue-card
  [{:keys [id title repo pr-count company compensation contributors level] :as issue}
   & [{:keys [edit-fn edit-success-fn]}]]
  (let [skeleton? (empty? (dissoc issue :id))
        logo (:logo company)
        derived-status (issue->status issue)
        amount (or (:amount compensation) 0)
        issue-icon (fn [id] [icon (if skeleton? "circle" id)])
        edit-icon (when edit-fn [icon "edit" :on-click #(dispatch [edit-fn issue edit-success-fn])])]
    [:div
     {:class (util/merge-classes "card issue-card"
                                 (when skeleton? "skeleton"))}
     [:div.header.is-hidden-desktop
      [:span (str (:owner repo) " / " (:name repo))]
      edit-icon]
     [:div.is-flex
      [:div.logo (wrap-img img logo {})]
      [:div.info
       [:div.header.is-hidden-mobile
        [:span (str (:owner repo) " / " (:name repo))]
        edit-icon]
       [:div.title (when-not skeleton? [link title :issue :id id :class "a--hover-red"])]]]
     [:div.issue-details
      [:ul.details
       [:li {:class (util/merge-classes "issue-status" (str "issue-status--" derived-status))} [icon "issue-status"] [:span (when-not skeleton? (str/capitalize derived-status))]]
       [:li.pr (issue-icon "pr") [:span (when-not skeleton? pr-count)]]
       [:li.contributors (issue-icon "contributors") [:span (when-not skeleton? (count contributors))]]
       [:li.level (issue-icon (level->icon level)) [:span.is-hidden-mobile (when-not skeleton? (str "Level: " (level->str level)))]]]
      [:ul.tags.issue-tags
       (when-let [language (:primary-language repo)]
         [:li.tag.tag--selected language])
       (when-not (zero? amount)
         [:li.tag.issue__compensation "$" amount])]]]))
