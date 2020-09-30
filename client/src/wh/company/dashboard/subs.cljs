(ns wh.company.dashboard.subs
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.common.data :refer [package-data]]
            [wh.common.keywords :as keywords]
            [wh.common.specs.company]
            [wh.company.dashboard.db :as sub-db]
            [wh.components.stats.db :as stats]
            [wh.job.db :as job]
            [wh.routes :as routes]
            [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::company-id
  (fn [db _]
    (get-in db [:wh.db/page-params :id])))

(reg-sub
  ::today
  (fn [_ _]
    (tf/unparse (tf/formatter "d MMMM yyyy") (t/now))))

(reg-sub
  ::sub-db
  (fn [db _]
    (::sub-db/sub-db db)))

(reg-sub
  ::name
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/name sub-db)))

(reg-sub
  ::slug
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/slug sub-db)))

(reg-sub
  ::profile-enabled?
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/profile-enabled sub-db)))

(reg-sub
  ::logo
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/logo sub-db)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/error sub-db)))

(reg-sub
  ::disabled?
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/disabled sub-db)))

(reg-sub
  ::permissions
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/permissions sub-db)))

(reg-sub
  ::can-see-applications?
  :<- [::permissions]
  (fn [permissions _]
    (contains? permissions :can_see_applications)))

(reg-sub
  ::view-applications-link
  :<- [:user/admin?]
  :<- [::can-see-applications?]
  :<- [:user/company?]
  :<- [::company-id]
  (fn [[admin? can-see-applications? company? company-id] [_ id]]
    (cond
      (and can-see-applications? company?)
      (routes/path :company-applications :query-params {:job-id id})
      ;;
      admin?
      (routes/path :admin-company-applications
                   :params {:id company-id}
                   :query-params {:job-id id})
      ;;
      :else
      (routes/path :payment-setup
                   :params {:step :select-package}
                   :query-params {:job id :action :applications}))))

(reg-sub
  ::description-html
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/description-html sub-db)))

(reg-sub
  ::package
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/package sub-db)))

(reg-sub
  ::billing-period
  :<- [::sub-db]
  (fn [sub-db _]
    (get-in sub-db [::sub-db/payment :billing-period])))

(reg-sub
  ::package-name
  :<- [::package]
  (fn [package _]
    (get-in package-data [package :name])))

(reg-sub
  ::package-icon
  :<- [::package]
  (fn [package _]
    (get-in package-data [package :img :src])))

(reg-sub
  ::stats
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/stats sub-db)))

(reg-sub
  ::granularity
  :<- [::stats]
  (fn [stats _]
    (if (= (:granularity stats) 7)
      :week
      :month)))

(reg-sub
  ::stats-item
  :<- [::stats]
  (fn [stats [_ stat]]
    {:x-axis (stats/x-axis stats stat)
     :y-axis (stats/y-axis stats stat)
     :values (stats/chart-values stats stat)
     :total  (stats/stat-total stats stat)
     :change (stats/stat-change stats stat)}))

(reg-sub
  ::jobs
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/jobs sub-db)))

(reg-sub
  ::published-jobs
  :<- [::jobs]
  (fn [jobs _]
    (filter :published jobs)))

(reg-sub
  ::unpublished-jobs
  :<- [::jobs]
  (fn [jobs _]
    (remove :published jobs)))

(reg-sub
  ::codi-message
  :<- [::stats]
  :<- [::granularity]
  (fn [[stats granularity] _]
    (let [change (stats/stat-change stats :applications)]
      (cond
        (nil? change) "Here’s an overview of your stats.  Once your role’s been live for a week we will publish a comparison."
        (= change "0%") "Applications to your positions are coming in a steady stream."
        (str/starts-with? change "+")
        (<< "Your stats from last ~(name granularity) look great, with a ~(subs change 1) increase in applicants!")
        :otherwise
        [:span
         (<< "The number of applications has gone down by ~(subs change 1) this ~(name granularity). You can ")
         [:a.a--underlined {:href "mailto:hello@works-hub.com"} "check with us"]
         " what we can do to reverse the trend."]))))

(reg-sub
  ::stats-title
  :<- [::granularity]
  (fn [granularity _]
    (if (= granularity :week)
      "Last 7 Days’ Stats"
      "Last Month’s Stats")))

(reg-sub
  ::activity
  :<- [::sub-db]
  (fn [sub-db _]
    (take (::sub-db/activity-items-count sub-db)
          (::sub-db/activity sub-db))))

(reg-sub
  ::show-more?
  :<- [::sub-db]
  (fn [sub-db _]
    (< (::sub-db/activity-items-count sub-db)
       (count (::sub-db/activity sub-db)))))

(reg-sub
  ::is-job-publishing?
  :<- [::sub-db]
  (fn [sub-db [_ id]]
    (some-> sub-db
            ::sub-db/publishing-jobs
            (contains? id))))

(reg-sub
  ::is-publish-celebration-showing?
  :<- [::sub-db]
  (fn [sub-db [_ id]]
    (some-> sub-db
            ::sub-db/publish-celebrations
            (contains? id))))

(reg-sub
  ::has-offer?
  :<- [::sub-db]
  (fn [sub-db _]
    (and (::sub-db/pending-offer sub-db)
         (get-in sub-db [::sub-db/pending-offer :recurring-fee])))) ;; need to make sure there is a fee, otherwise it's possible the offer wasn't created

(defn sub-db->minimum-company
  [sub-db]
  (-> sub-db
      (keywords/strip-ns-from-map-keys)
      (update :blogs (fn [blogs] (:blogs blogs)))
      (update :tech-scales (fn [ts] (util/remove-nils ts)))
      (update :tags (fn [tags] (map (fn [tag] (cond-> tag
                                                      (nil? (:subtype tag))
                                                      (dissoc :subtype)
                                                      :always
                                                      (util/update* :subtype keyword))) tags)))))

(defn round-up-to
  [base x]
  (* base (Math/ceil (/ x base))))

(reg-sub
  ::profile-completion-percentage
  :<- [::sub-db]
  (fn [sub-db _]
    (let [company (sub-db->minimum-company sub-db)
          max-score (count (reduce concat (filter vector? (s/form (s/get-spec :wh.company/top-score-profile)))))
          penalties (count (::s/problems (s/explain-data :wh.company/top-score-profile company)))]
      (->> (* (/ (- max-score penalties) max-score) 100)
           (round-up-to 5)
           (max 0)
           (min 100)))))

(reg-sub
  ::show-onboarding?
  :<- [:wh.user.subs/company-onboarding-msg-not-seen? :dashboard_welcome]
  :<- [:user/company?]
  (fn [[not-seen? user-is-company?] _]
    (and user-is-company? not-seen?)))

(reg-sub
  ::can-edit-jobs?
  (fn [db _]
    (job/can-edit-jobs? db)))
