(ns wh.company.applications.subs
  (:require [cljs-time.format :as tf]
            [clojure.string :as str]
            [goog.string :as gstr]
            [re-frame.core :refer [reg-sub]]
            [wh.common.attachments :as att]
            [wh.common.user :as user-common]
            [wh.company.applications.db :as sub-db]
            [wh.db :as db]))

(defn action->state
  [a]
  (case a
    "approve" "approved"
    "reject" "rejected"
    a))

(reg-sub
  ::db
  (fn [db _]
    db))

(reg-sub
  ::sub-db
  (fn [db _]
    (::sub-db/sub-db db)))

(defn capitalize [s]
  (if (empty? s)
    ""
    (str (str/upper-case (subs s 0 1))
         (subs s 1))))

(defn move-matching-skills-to-front
  [skills job-tags]
  (let [has-skill? (fn [{name :name}] (when name (contains? job-tags (str/lower-case name))))]
    (concat
     (map #(assoc % :highlighted true)
          (filter has-skill? skills))
     (remove has-skill? skills))))

(defn translate-application
  [job-tags application]
  (let [tags (set (->> job-tags (map :label) (map str/lower-case)))]
    (-> application
        (assoc :cv-downloadable? (att/downloadable? (get-in application [:cv :file :name])))
        (assoc :cover-letter-downloadable? (att/downloadable? (get-in application [:cover-letter :file :name])))
        (update :name #(if (str/blank? %)
                         [:i "Unnamed user"]
                         (capitalize %)))
        (update :timestamp (partial tf/unparse (tf/formatters :date)))
        (update :cv #(or (get-in % [:file :url])
                         (get % :link)))
        (update :cover-letter #(or (get-in % [:file :url])
                                   (get % :link)))
        (update :skills move-matching-skills-to-front tags))))

(reg-sub
  ::current-tab
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/current-tab sub-db)))

(defn tab->state-pred
  [tab admin?]
  (fn [state]
    (case tab
      :interviewing (= "get_in_touch" state)
      :pending      (if admin? (= "pending" state) (= "approved" state))
      :approved     (if admin? (= "approved" state) false)
      :pass         (= "pass" state)
      :rejected     (= "rejected" state)
      :hired        (= "hired" state)
      false)))

(defn tab-frequency
  [freqs admin? tab]
  (let [app-state-freqs #(get freqs % 0)]
    (cond (= tab :pending)               (if admin?
                                           (app-state-freqs :pending)
                                           (app-state-freqs :approved))
          (= tab :interviewing)          (app-state-freqs :get_in_touch)
          (= tab :pass)                  (app-state-freqs :pass)
          (and admin? (= tab :approved)) (app-state-freqs :approved)
          (= tab :rejected)              (app-state-freqs :rejected)
          (= tab :hired)                 (app-state-freqs :hired))))

(reg-sub
  ::tab-frequency
  :<- [::state-frequencies]
  :<- [:user/admin?]
  (fn [[freqs admin?] [_ tab]]
    (tab-frequency freqs admin? tab)))

(defn tabs
  [freqs admin?]
  (let [freq (partial tab-frequency freqs admin?)]
    (merge {:pending (gstr/format "Pending (%d)"                   (freq :pending))}
           (when admin?
             {:approved (gstr/format "Approved (%d)"               (freq :approved))})
           {:interviewing (gstr/format "Interviewing (%d)"         (freq :interviewing))
            :hired        (gstr/format "Hired (%d)"                (freq :hired))
            :pass         (gstr/format "Pass (%d)"                 (freq :pass))
            :rejected     (gstr/format (if admin?
                                         "Rejected (%d)"
                                         "Rejected by WorksHub (%d)") (freq :rejected))})))
(reg-sub
  ::state-frequencies
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/frequencies sub-db)))

(reg-sub
  ::tabs
  :<- [::state-frequencies]
  :<- [:user/admin?]
  (fn [[freqs admin?] _]
    (when freqs
      (tabs freqs admin?))))

(reg-sub
  ::applications
  :<- [::sub-db]
  :<- [::current-job]
  :<- [::current-tab]
  :<- [:user/admin?]
  (fn [[sub-db {:keys [tags]} tab admin?] _]
    (when (::sub-db/applications sub-db)
      (->> (::sub-db/applications sub-db)
           (map (partial translate-application tags))))))

(reg-sub
  ::logo
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/logo sub-db)))

(reg-sub
  ::company-name
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/company-name sub-db)))

(reg-sub
  ::job-id
  (fn [db _]
    (get-in db [::db/query-params "job-id"])))

(reg-sub
  ::jobs
  :<- [::sub-db]
  :<- [::company-name]
  :<- [::logo]
  (fn [[sub-db company-name logo] _]
    (map #(assoc % :company-name company-name :logo logo)
         (::sub-db/jobs sub-db))))

(reg-sub
  ::current-job
  :<- [::jobs]
  :<- [::job-id]
  (fn [[jobs job-id] _]
    (some-> (first (filter #(= (:id %) job-id) jobs))
            (update :first-published #(when % (tf/unparse (tf/formatters :date) %))))))

(reg-sub
  ::company-id
  :<- [::db]
  (fn [db _]
    (sub-db/company-id db)))

(reg-sub
  ::jobs-dropdown
  :<- [::company-id]
  :<- [::jobs]
  (fn [[company? jobs] _]
    (when jobs
      (into [{:id nil, :label (if company? "All jobs" "My jobs")}]
            (sort-by :label
                     (for [{:keys [id title company-name]} jobs]
                       {:id id, :label (if company?
                                         title
                                         (str company-name " - " title))}))))))

(reg-sub
  ::companies-dropdown
  :<- [::sub-db]
  (fn [sub-db _]
    (when-let [companies (::sub-db/companies sub-db)]
      (into [{:id nil, :label "My companies"}]
            (sort-by :label
                     (for [{:keys [id name]} companies]
                       {:id id, :label name}))))))

(reg-sub
  ::get-in-touch-overlay-data
  :<- [::sub-db]
  (fn [db  _]
    (::sub-db/get-in-touch-overlay db)))

(reg-sub
  ::show-get-in-touch-overlay?
  :<- [::get-in-touch-overlay-data]
  (fn [data _]
    (boolean data)))

(reg-sub
  ::job-selection-overlay-args
  :<- [::sub-db]
  (fn [db _]
    (get db ::sub-db/job-selection-overlay-args)))

(reg-sub
  ::job-selection-overlay-state
  :<- [::job-selection-overlay-args]
  (fn [[_ state] _]
    state))

(reg-sub
  ::show-job-selection-overlay?
  :<- [::job-selection-overlay-args]
  (fn [args _]
    (boolean args)))

(reg-sub
  ::job-selection-overlay-data
  :<- [::sub-db]
  :<- [::job-selection-overlay-args]
  (fn [[db [user-id action]] _]
    (when (and user-id action)
      (let [job-ids (->> (::sub-db/applications db)
                         (filter #(and (= user-id (:user-id %))
                                       (not= (action->state action) (:state %))
                                       ;; can't reject if already approved
                                       (if (= action "reject")
                                         (not= "approved" (:state %))
                                         true)))
                         (map :job-id)
                         (set))]
        (filter #(contains? job-ids (:id %)) (::sub-db/jobs db))))))

(reg-sub
  ::job-selection-overlay-job-selections
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/job-selection-overlay-job-selections db)))

(reg-sub
  ::notes-overlay-user-args
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/notes-overlay-args db)))

(reg-sub
  ::show-notes-overlay?
  :<- [::notes-overlay-user-args]
  (fn [args _]
    (boolean args)))

(reg-sub
  ::notes-overlay-data
  :<- [::sub-db]
  :<- [::job-id]
  :<- [::company-id]
  :<- [::notes-overlay-user-args]
  (fn [[sub-db job-id-from-params company? [user-id job-id]] _]
    [user-id job-id (if (and company? (not job-id-from-params))
                      (some-> (sub-db/some-application-by-job sub-db job-id #(= user-id (:user-id %))) :note)
                      (some #(when (and (= user-id (:user-id %))
                                        (= job-id (:job-id %))) (:note %))
                            (::sub-db/applications sub-db)))]))

(reg-sub
  ::applications-loading?
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/applications-loading? db)))

(reg-sub
  ::show-load-more-applications?
  :<- [::sub-db]
  :<- [::state-frequencies]
  :<- [::current-tab]
  :<- [::applications]
  :<- [:user/admin?]
  (fn [[db freqs current-tab apps admin?] _]
    (let [tab-states (sub-db/tab->states current-tab admin?)
          state-freq (reduce (fn [a s] (+ a (get freqs s))) 0 tab-states)]
      (and apps (< (count apps) state-freq)))))

(reg-sub
  ::show-load-more-latest-applied-jobs?
  :<- [::latest-applied-jobs]
  :<- [::sub-db]
  (fn [[applied sub-db] _]

    (and applied
         (not= (count applied) (::sub-db/total-latest-applied-jobs sub-db)))))

(reg-sub
  ::admin-dashboard?
  (fn [db _]
    (and (not (sub-db/company-id db))
         (user-common/admin? db))))

(reg-sub
  ::latest-applied-jobs
  :<- [::sub-db]
  :<- [::company-name]
  :<- [::logo]
  (fn [[db company-name logo] _]
    (when (::sub-db/latest-applied-jobs db)
      (map (fn [job] (-> job
                         (assoc :company-name company-name
                                :logo logo)
                         (update :first-published #(when % (tf/unparse (tf/formatters :date) %)))))
           (::sub-db/latest-applied-jobs db)))))

(reg-sub
  ::applications-by-jobs
  :<- [::sub-db]
  :<- [:user/admin?]
  (fn [[sub-db admin?] _]
    (into {}
          (map (fn [[job-id {:keys [current-tab applications frequencies] :as app}]]
                 (let [job-tags (some #(when (= job-id (:id %)) (:tags %)) (::sub-db/latest-applied-jobs sub-db))]
                   [job-id (-> app
                               (update :applications (fn [applications]
                                                       (when applications
                                                         (->> applications
                                                              (map (partial translate-application job-tags))))))
                               (update :frequencies (fn [freqs] (when freqs
                                                                  (tabs freqs admin?))))
                               (assoc  :current-frequency (when frequencies
                                                            (tab-frequency frequencies admin? current-tab))))]))
               (get sub-db ::sub-db/applications-by-job)))))

(reg-sub
  ::select-job-link
  :<- [::db]
  :<- [::company-id]
  (fn [[db company-id] _]
    (merge {:handler (sub-db/get-current-page db)}
           (when company-id {:options {:id company-id}}))))

(reg-sub
  ::has-permission-to-view-applications?
  :<- [::sub-db]
  (fn [sub-db _]
    (::sub-db/has-permission? sub-db)))
