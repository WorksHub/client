(ns wh.profile.section-admin
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
      (-> time
          (time/str->time :date-time)
          time/human-time)
      (str " by " source)])])

(defn subsection [title content]
  [:div {:class styles/admin__subsection}
   [:div {:class styles/admin__subsection-title} title]
   content])

(defn job-list [content]
  [:ul {:class styles/admin__job-list}
   content])

(defn job-link [job]
  [components/underline-link
   {:text (:title job)
    :href (routes/path :job :params {:slug (:slug job)})}])

(defn job-application [{:keys [job state timestamp]}]
  [:div
   [job-link job]
   [:div {:class styles/admin__secondary-text}
    (str state " , " (-> timestamp
                         (time/str->time :date-time)
                         time/human-time))]])

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
   [components/title "Admin section"]
   [approve-buttons opts]
   [subsection "Status:" [approval approval-info]]
   [subsection "Hubspot:" [:div [components/underline-link {:text hs-url
                                                            :href hs-url
                                                            :new-tab? true}]]]
   [subsection "Applied to:" [job-list
                              (if (seq applications)
                                (for [application applications]
                                  ^{:key (:timestamp application)}
                                  [:li [job-application application]])
                                "-")]]
   [subsection "Liked jobs:" [job-list
                              (if (seq liked-jobs)
                                (for [job liked-jobs]
                                  ^{:key (:id job)}
                                  [:li [job-link job]])
                                "-")]]])
