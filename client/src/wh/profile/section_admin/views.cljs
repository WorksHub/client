(ns wh.profile.section-admin.views
  (:require [clojure.string :as str]
            [wh.common.time :as time]
            [wh.components.common :refer [link]]
            [wh.logged-in.profile.components :as components]
            [wh.routes :as routes]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn approve-buttons [{:keys [approval-info updating-status on-approve on-reject]}]
  (let [rejected? (= (:status approval-info) "rejected")
        approved? (= (:status approval-info) "approved")
        reject-text (if (= updating-status :rejecting)
                      "Rejecting..."
                      "Reject")
        approve-text (if (= updating-status :approving)
                       "Approving..."
                       "Approve")]
    [:div {:class styles/admin__buttons}
     [:button
      {:disabled (or rejected? updating-status)
       :class (util/mc styles/button styles/button--small styles/button--neutral-inverted)
       :on-click on-reject} reject-text]
     [:button
      {:disabled (or approved? updating-status)
       :class (util/mc styles/button styles/button--small styles/button--neutral)
       :on-click on-approve} approve-text]]))

(defn approval [{:keys [status source time]}]
  [:div
   [:div (str/capitalize status)]
   (when source
     [:div {:class styles/admin__secondary-text}
      (time/str->human-time time)
      (str " by " source)])])

(defn job-application [{:keys [job] :as application}]
  [:div
   [components/job-link job]
   [components/application-state application nil nil]])

(defn toggle-view [user admin-view?]
  (let [admin-link (routes/path :user :params {:id (:id user)})
        public-link (routes/path :user :params {:id (:id user)} :query-params {:type "public"})]
    [:div {:class styles/toggle-view}
     [:span {:class styles/toggle-view__title} "Profile view:"]
     [:div
      [:a {:class (util/mc styles/toggle-view__link [admin-view? styles/toggle-view__link--selected])
           :href  admin-link} "Admin"]
      [:a {:class (util/mc styles/toggle-view__link [(not admin-view?) styles/toggle-view__link--selected])
           :href  public-link} "Public"]]]))

(defn controls [{:keys [applications liked-jobs hs-url approval-info] :as opts}]
  [components/section
   [components/sec-title "Admin section"]
   [approve-buttons opts]
   [components/subsection "Status:" [approval approval-info]]
   [components/subsection "Hubspot:" [:div [components/underline-link {:text hs-url
                                                                       :href hs-url
                                                                       :new-tab? true}]]]
   [components/subsection "Applied to:" [components/job-list
                                         (if (seq applications)
                                           (for [application applications]
                                             ^{:key (:timestamp application)}
                                             [:li [job-application application]])
                                           "-")]]
   [components/subsection "Liked jobs:" [components/job-list
                                         (if (seq liked-jobs)
                                           (for [job liked-jobs]
                                             ^{:key (:id job)}
                                             [:li [components/job-link job]])
                                           "-")]]])
