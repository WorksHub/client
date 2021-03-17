(ns wh.landing-new.components
  (:require [clojure.string :as str]
            [wh.common.text :as text]
            [wh.components.activities.components :as activities]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.landing-new.events :as events]
            [wh.landing-new.subs :as subs]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.landing :as styles]
            [wh.styles.landing-user-dashboard :as dashboard-styles]
            [wh.util :as util]))

(defn stats-row [title value]
  [:p {:class dashboard-styles/section-row}
   [:span title]
   [:span {:class dashboard-styles/section-row__value} value]])

(defn stats-section [{:keys [title class]} children]
  [:section (util/smc class dashboard-styles/section)
   [:h1 {:class dashboard-styles/section-title} title]
   children])

(defn dashboard-header [{:keys [user-image user-name]}]
  [:div {:class dashboard-styles/dashboard-header}
   (when user-image
     [:img {:class dashboard-styles/user-image
            :src   user-image}])

   [:div {:class dashboard-styles/user-profile}
    [:span {:class dashboard-styles/user-name}
     "Welcome" (when (text/not-blank user-name) (str ", " user-name)) "!"]
    [:a {:class dashboard-styles/edit-profile
         :href  (routes/path :profile)}
     [:span "Edit profile"]
     [icons/icon "pen"
      :class dashboard-styles/edit-profile__icon]]]])

(defn ctas [{:keys [user-name]}]
  [:section {:class dashboard-styles/cta}
   (when (str/blank? user-name)
     [:a {:class dashboard-styles/cta__link
          :href  (routes/path :profile-edit-header)}
      [:span {:class dashboard-styles/cta__link__accent} "What shall we call you?"]])
   [:a {:class dashboard-styles/cta__link
        :href  (routes/path :contribute)}
    [:span "Have something you want to share? "]
    [:span {:class dashboard-styles/cta__link__accent} "Write an article"]]
   [:a {:class dashboard-styles/cta__link
        :href  (routes/path :profile)}
    [:span "Add "]
    [:span {:class dashboard-styles/cta__link__accent} "more skills "]
    [:span "to improve your recommendations"]]
   [:a {:class dashboard-styles/cta__link
        :href  (routes/path :liked)}
    [icons/icon "save"
     :class dashboard-styles/save-icon]
    [:span "  View your saved items"]]])

(defn user-dashboard []
  (let [user-name                (<sub [:user/name])
        user-image               (<sub [::subs/user-image])
        {:keys [blogs-counted
                applications-counted
                issues-counted]} (<sub [::subs/user-details])]

    [:div (merge (util/smc dashboard-styles/dashboard)
                 {:data-test "user-dashboard"})
     [dashboard-header {:user-image user-image
                        :user-name  user-name}]

     [stats-section {:title "Job Applications"
                     :class dashboard-styles/job-applications}
      [:<>
       [stats-row "Submitted" (:submitted applications-counted)]
       [stats-row "In progress" (:in-progress applications-counted)]
       [stats-row "Interview stage" (:interview-stage applications-counted)]]]

     [stats-section {:title "Articles"
                     :class dashboard-styles/articles}
      [:<>
       [stats-row "Published" (:published blogs-counted)]
       [stats-row "In progress" (:in-progress blogs-counted)]]]

     [stats-section {:title "Open Source Issues"
                     :class dashboard-styles/issues}
      [:<>
       [stats-row "Completed" (:completed issues-counted)]
       [stats-row "In progress" (:in-progress issues-counted)]]]

     [ctas {:user-name user-name}]]))


(defn prev-next-buttons [newer-than older-than]
  (let [next-page (<sub [::subs/recent-activities-next-page])
        prev-page (<sub [::subs/recent-activities-prev-page])]
    [:div {:class styles/prev-next-buttons}

     ;; This wrapping :div stays here, because even if prev-page button
     ;; is not present we need some element to fill grid-column and
     ;; keep layout intact
     [:div
      (when prev-page
        [activities/button
         {:type :dark
          :event-handlers
          (interop/on-click-fn
            #?(:clj  (format "setBeforeIdAndRedirect('%s');" newer-than)
               :cljs #(dispatch [::events/set-newer-than newer-than])))}
         [:span {:class styles/prev-next-button__text}
          [icons/icon "chevron_left"]
          [:span "Prev"]]])]

     (when next-page
       [activities/button
        {:type :dark
         :event-handlers
         (interop/on-click-fn
           #?(:clj  (format "setAfterIdAndRedirect('%s');" older-than)
              :cljs #(dispatch [::events/set-older-than older-than])))}
        [:span {:class styles/prev-next-button__text}
         [:span "Next"]
         [icons/icon "chevron_right"]]])]))
