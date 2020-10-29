(ns wh.profile.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [wh.components.loader :refer [loader-full-page]]
    [wh.components.not-found :as not-found]
    [wh.logged-in.profile.components :as components]
    #?(:cljs [wh.profile.section-admin :as section-admin])
    [wh.profile.db :as profile]
    [wh.profile.events :as profile-events]
    [wh.profile.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn section-admin []
  #?(:cljs (let [user (<sub [::subs/profile])
                 admin-view? (<sub [::subs/admin-view?])]
             (when admin-view? [section-admin/controls {:hs-url (<sub [::subs/hs-url])
                                                        :applications (<sub [::subs/applications])
                                                        :liked-jobs (<sub [::subs/liked-jobs])
                                                        :approval-info (<sub [::subs/approval-info])
                                                        :on-approve #(dispatch [::profile-events/set-approval-status (:id user) "approved"])
                                                        :on-reject #(dispatch [::profile-events/set-approval-status (:id user) "rejected"])
                                                        :updating-status (<sub [::subs/updating-status])}]))))

(defn section-stats [{:keys [articles issues]}]
  [components/section-stats {:is-owner?      false
                             :percentile     (<sub [::subs/percentile])
                             :created        (<sub [::subs/created])
                             :articles-count (count articles)
                             :issues-count   (count issues)}])

(defn section-skills []
  [components/section-skills {:type         :public
                              :skills       (<sub [::subs/skills])
                              :interests    (<sub [::subs/interests])
                              :query-params (<sub [:wh/query-params])
                              :max-skills   profile/maximum-skills}])

(defn section-contributions []
  (when (boolean (<sub [::subs/contributions-collection]))
    [components/section-contributions
     (<sub [::subs/contributions-calendar])
     (<sub [::subs/contributions-count])
     (<sub [::subs/contributions-repos])
     (<sub [::subs/contributions-months])]))

(defn content []
  (let [hide-profile? (<sub [::subs/hide-profile?])
        articles (<sub [::subs/blogs])
        issues (<sub [::subs/issues])
        admin-view? (<sub [::subs/admin-view?])
        admin? (<sub [:user/admin?])
        user (<sub [::subs/profile])]
    [:<>
     #?(:cljs (when admin? [section-admin/toggle-view user admin-view?]))
     (if hide-profile?
       [components/profile-hidden-message]
       [components/content
        [section-admin]
        [section-stats {:articles articles
                        :issues issues}]
        [section-skills]
        [section-contributions]
        [components/section-articles articles :public]
        [components/section-issues issues :public]])]))

(defn page []
  (cond
    (<sub [::subs/error?])
    [not-found/not-found-profile]
    (<sub [::subs/loader?])
    [loader-full-page]
    :else
    [components/container
     [components/profile (<sub [::subs/profile])
      {:twitter       (<sub [::subs/social :twitter])
       :stackoverflow (<sub [::subs/social :stackoverflow])
       :github        (<sub [::subs/social :github])
       :last-seen     (<sub [::subs/last-seen])
       :updated       (<sub [::subs/updated])}
      :public]
     [content]]))
