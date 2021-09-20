(ns wh.job.subs
  (:require [#?(:cljs cljs-time.format :clj clj-time.format) :as tf]
            [bidi.bidi :as bidi]
            [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.common.job :as jobc]
            [wh.components.stats.db :as stats]
            [wh.job.db :as job]
            [wh.routes :as routes]
            [wh.company.listing.db :as listing]
            [wh.common.timezones :as timezones])
  (#?(:cljs :require-macros :clj :require)
    [clojure.core.strint :refer [<<]]))

(reg-sub ::db (fn [db _] db))

(reg-sub ::sub-db (fn [db _] (get-in db [::job/sub-db])))

(reg-sub
  ::id
  (fn [db _]
    (get-in db [::job/sub-db ::job/id])))

(reg-sub
  ::salary
  :<- [::sub-db]
  (fn [{:keys [::job/remuneration]} _]
    (when remuneration
      (jobc/format-job-remuneration remuneration))))

(reg-sub
  ::remote?
  (fn [db _]
    (get-in db [::job/sub-db ::job/remote])))

(reg-sub
  ::liked?
  :<- [::id]
  :<- [:user/liked-jobs]
  (fn [[id liked-jobs] _]
    (contains? liked-jobs id)))

(reg-sub
  ::sponsorship-offered?
  (fn [db _]
    (get-in db [::job/sub-db ::job/sponsorship-offered])))

(reg-sub
  ::company
  :<- [::sub-db]
  (fn [{:keys [::job/company]} _]
    company))

(reg-sub
  ::company-card
  :<- [::company]
  (fn [company _]
    (listing/->company company)))

(reg-sub
  ::logo
  :<- [::company]
  (fn [company _]
    (:logo company)))

(reg-sub
  ::title
  (fn [db _]
    (get-in db [::job/sub-db ::job/title])))

(reg-sub
  ::tagline
  (fn [db _]
    (get-in db [::job/sub-db ::job/tagline])))

(reg-sub
  ::role-type
  (fn [db _]
    (get-in db [::job/sub-db ::job/role-type])))

(reg-sub
  ::company-name
  :<- [::company]
  (fn [company _]
    (:name company)))

(reg-sub
  ::company-id
  (fn [db _]
    (get-in db [::job/sub-db ::job/company-id])))

(reg-sub
  ::description
  (fn [db _]
    (get-in db [::job/sub-db ::job/description-html])))

(reg-sub
  ::remote-info
  (fn [db _]
    (get-in db [::job/sub-db ::job/remote-info])))

(reg-sub
  ::region-restrictions
  :<- [::remote-info]
  (fn [remote-info _]
    (seq (:region-restrictions remote-info))))

(reg-sub
  ::timezone-restrictions
  :<- [::remote-info]
  (fn [remote-info _]
    (->> remote-info
         :timezone-restrictions
         (map (fn [{:keys [timezone-name] :as tz}]
                (assoc tz :gmt
                       (some (fn [[id _ _ gmt]] (when (= id timezone-name) gmt))
                             timezones/timezones))))
         (seq))))

(reg-sub
  ::issues
  :<- [::company]
  (fn [company _]
    (take 2 (:issues company))))

(reg-sub
  ::location
  :<- [::sub-db]
  (fn [{:keys [::job/location ::job/remote]} _]
    (jobc/format-job-location location remote)))

(reg-sub
  ::google-map-url
  :<- [::sub-db]
  :<- [::company-name]
  (fn [[{:keys [::job/location]} company-name] _]
    (->> (select-keys location [:street :city :postcode :state :country])
         vals
         (str/join ", ")
         (str company-name ", ")
         (bidi/url-encode)
         (str "https://www.google.com/maps/search/?api=1&query="))))

(reg-sub
  ::new-location
  (fn [db _]
    (get-in db [::job/sub-db ::job/location])))

(reg-sub
  ::location-description
  (fn [db _]
    (get-in db [::job/sub-db ::job/location-description])))

(reg-sub
  ::show-address?
  :<- [::new-location]
  :<- [:user/logged-in?]
  (fn [[{:keys [city street]} logged-in?] _]
    (and city street logged-in?)))

(reg-sub
  ::location-position
  :<- [::new-location]
  (fn [location _]
    (when location
      (select-keys location [:latitude :longitude]))))

(reg-sub
  ::show-location?
  :<- [::show-address?]
  :<- [::location-position]
  :<- [::location-description]
  (fn [[address? position description] _]
    (or description (and address? position))))

(reg-sub
  ::location-address
  :<- [::new-location]
  (fn [{:keys [street city country]} _]
    (cond
      (and city country (not street)) (<< "~{city}, ~{country}")
      (and city country street) (<< "~{street}, ~{city}, ~{country}")
      :otherwise nil)))

(reg-sub
  ::location-address-parts
  :<- [::new-location]
  (fn [{:keys [street city country]} _]
    (keep identity [street city country])))

(reg-sub
  ::tags
  (fn [db _]
    (get-in db [::job/sub-db ::job/tags])))

(reg-sub
  ::benefits
  :<- [::company]
  (fn [company _]
    (some->> (:tags company)
             (filter #(= :benefit (some-> % :type keyword)))
             (seq)
             (map (comp str/capitalize :label)))))

(reg-sub
  ::error
  (fn [db _]
    (get-in db [::job/sub-db ::job/error])))

(defn sort-applications [criterion items]
  (if criterion
    (let [{:keys [column direction]} criterion
          cmp (comp ({:asc +, :desc -} direction) compare)]
      (sort-by column cmp items))
    items))

(def state-display-map
  {:not-integrated "Not integrated"
   :pending "Pending"
   :approved "Approved"
   :rejected "Rejected"})

(reg-sub
  ::applied?
  :<- [::sub-db]
  :<- [:user/applied-jobs]
  (fn [[sub-db applied-jobs] _]
    (contains? applied-jobs (::job/id sub-db))))

(reg-sub
  ::published?
  (fn [db _]
    (get-in db [::job/sub-db ::job/published])))

(reg-sub
  ::publishing?
  (fn [db _]
    (get-in db [::job/sub-db ::job/publishing?])))

(reg-sub
  ::like-icon-shown?
  :<- [::loaded?]
  :<- [:user/candidate?]
  (fn [[loaded? candidate?] _]
    (and candidate? loaded?)))

(reg-sub
  ::loaded?
  :<- [::description]
  (fn [description _]
    (boolean description)))

(reg-sub
  ::owner?
  (fn [db _]
    (if-let [job-company-id (get-in db [::job/sub-db ::job/company-id])]
      (= job-company-id (get-in db [:wh.user.db/sub-db :wh.user.db/company-id]))
      false)))

(reg-sub
  ::show-unpublished?
  :<- [:user/admin?]
  :<- [::owner?]
  :<- [::published?]
  (fn [[admin? owner? published?] _]
    (job/show-unpublished? admin? owner? published?)))

(reg-sub
  ::apply-job
  :<- [::sub-db]
  :<- [::company-name]
  (fn [[{:keys [::job/id ::job/location ::job/slug] :as db} company-name] _]
    {:id id :company-name company-name :location location :slug slug}))

(reg-sub
  :google/maps-loaded?
  (fn [db _]
    (:google/maps-loaded? db)))

(reg-sub
  ::manager
  (fn [db _]
    (get-in db [::job/sub-db ::job/manager])))

(reg-sub
  ::apply-on-behalf
  :<- [::sub-db]
  (fn [db _]
    (::job/apply-on-behalf db)))

(reg-sub
  ::apply-on-behalf-id
  :<- [::sub-db]
  (fn [db _]
    (::job/apply-on-behalf-id db)))

(reg-sub
  ::apply-on-behalf-suggestions
  :<- [::sub-db]
  (fn [db _]
    (::job/apply-on-behalf-suggestions db)))

(reg-sub
  ::show-notes-overlay?
  :<- [::sub-db]
  (fn [db _]
    (::job/show-notes-overlay? db)))

(reg-sub
  ::applying?
  :<- [::sub-db]
  (fn [db _]
    (::job/applying? db)))

(reg-sub
  ::note
  :<- [::sub-db]
  (fn [db _]
    (::job/note db)))

(reg-sub
  ::apply-on-behalf-button-active?
  :<- [::apply-on-behalf-id]
  (fn [id _]
    (boolean id)))

(reg-sub
  ::applied-on-behalf
  :<- [::sub-db]
  (fn [db _]
    (::job/applied-on-behalf db)))

(reg-sub
  ::view-applications-link
  :<- [::db]
  :<- [::id]
  :<- [::company-id]
  :<- [:user/admin?]
  (fn [[db id company-id admin?] _]
    (if admin?
      (routes/path :admin-company-applications
                   :params {:id company-id}
                   :query-params {:job-id id})
      (if (contains? (job/company-permissions db) :can_see_applications)
        (routes/path :company-applications
                     :query-params {:job-id id})
        (routes/path :payment-setup
                     :params {:step :select-package}
                     :query-params {:job id :action :applications})))))

(reg-sub
  ::apply-on-behalf-error
  :<- [::sub-db]
  (fn [db _]
    (when-let [err (::job/apply-on-behalf-error db)]
      (case err
        :incomplete-profile "This user has incomplete profile. Required fields are name, email, current location and CV."
        :incorrect-user-type "Only candidates can apply for jobs, this user has other type (admin or company)"
        :application-already-exists "This user has already applied for this job."
        "An unexpected error occurred."))))

(reg-sub
  ::matching-users
  :<- [::sub-db]
  (fn [db _]
    (::job/matching-users db)))

(reg-sub
  ::user-score
  :<- [::sub-db]
  (fn [db _]
    (::job/user-score db)))

(reg-sub
  ::recommended-jobs
  :<- [::sub-db]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[sub-db liked-jobs applied-jobs] _]
    (if (::job/recommended-jobs sub-db)
      (->> (::job/recommended-jobs sub-db)
           (filter #(not= (:id %) (::job/id sub-db)))
           (take 3)
           (map jobc/translate-job)
           (jobc/add-interactions liked-jobs applied-jobs))
      (map #(hash-map :id %) (range 3)))))

(reg-sub
  ::show-apply-sticky?
  :<- [::applied?]
  :<- [:user/company?]
  :<- [:user/admin?]
  (fn [[applied? company? admin?] _]
    (not (or applied? company? admin?))))

(reg-sub
  ::candidate-message
  :<- [::applied?]
  :<- [::user-score]
  (fn [[applied? score] _]
    (cond applied?
          "You have applied for this role. Good luck!"
          (>= score 0.75) "You're a great match for this role! What are you waiting for?"
          :else nil)))

(reg-sub
  ::stats
  :<- [::sub-db]
  (fn [db _]
    (::job/analytics db)))

(reg-sub
  ::granularity
  :<- [::stats]
  (fn [stats _]
    (if (= (:granularity stats) 7)
      :week
      :month)))

(reg-sub
  ::stats-title
  :<- [::granularity]
  (fn [granularity _]
    (if (= granularity :week)
      "Last 7 Days’ Stats"
      "Last Month’s Stats")))

(reg-sub
  ::stats-item
  :<- [::stats]
  (fn [stats [_ stat]]
    (stats/stat-item-data stats stat)))

(reg-sub
  ::show-issues?
  :<- [::issues]
  (fn [issues _]
    (seq issues)))

(reg-sub
  :wh.job/show-admin-publish-prompt?
  :<- [::sub-db]
  (fn [db _]
    (::job/show-admin-publish-prompt? db)))

(reg-sub
  ::admin-publish-prompt-loading?
  :<- [::sub-db]
  (fn [db _]
    (::job/admin-publish-prompt-loading? db)))

(reg-sub
  ::company-permissions
  (fn [db _]
    (job/company-permissions db)))

(reg-sub
  ::profile-enabled?
  :<- [::company]
  (fn [company _]
    (:profile-enabled company)))

(reg-sub
  ::company-slug
  :<- [::company]
  (fn [company _]
    (:slug company)))

(reg-sub
  ::last-modified
  :<- [::sub-db]
  (fn [sub-db _]
    (some->> (::job/last-modified sub-db)
             (tf/parse   (tf/formatters :date-time))
             (tf/unparse (tf/formatter "dd MMMM, YYYY")))))

(reg-sub
  ::can-edit-jobs?
  (fn [db _]
    (job/can-edit-jobs? db)))
