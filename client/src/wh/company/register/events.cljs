(ns wh.company.register.events
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.fx.google-maps :as google-maps]
    [wh.company.common :as company]
    [wh.company.events]
    [wh.company.register.db :as register]
    [wh.company.register.graphql :as graphql]
    [wh.company.register.subs :as register-subs]
    [wh.db :as db]
    [wh.graphql.company :refer [fetch-tags update-company-mutation]]
    [wh.graphql.jobs]
    [wh.pages.core :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util]))

(def register-interceptors (into db/default-interceptors
                                 [(path ::register/sub-db)]))

(defn check-field
  [db field]
  (assoc-in db [::register/checked-form field] (get db field)))

(defn toggle-company-field-errors
  [db]
  (reduce (fn [db [_ k]] (check-field db k)) db register/company-fields))

(defn toggle-job-field-errors
  [db]
  (reduce (fn [db [_ k]] (check-field db k)) db register/job-fields))

(defn find-first-error-key
  [db fields-maps]
  (some (fn [{:keys [key spec]}] (when (register-subs/error-query db key spec) key)) fields-maps))

(defmulti progress-sign-up identity)

(defmethod progress-sign-up
  :company-details
  [_ db]
  (let [sub-db (::register/sub-db db)]
    (if (register/valid-company-form? sub-db)
      {:db      (assoc-in db [::register/sub-db ::register/loading?] true)
       :graphql (graphql/create-company-and-user-mutation
                 sub-db
                 ::create-company-and-user-success
                 ::create-company-and-user-fail)}
      (let [checked-db (assoc db ::register/sub-db (toggle-company-field-errors sub-db))
            error (find-first-error-key checked-db register/company-fields-maps)]
        (merge
          {:db checked-db}
          (when error
            {:scroll-into-view (db/key->id error)}))))))

(defmethod progress-sign-up
  :job-details
  [_ db]
  (let [sub-db (::register/sub-db db)
        valid-form? (register/valid-job-form? sub-db)
        valid-location? (register/location-details-valid? sub-db)]
    (cond (and valid-form? valid-location?)
          {:db      (assoc-in db [::register/sub-db ::register/loading?] true)
           :graphql (graphql/create-job-mutation
                     db
                     ::create-job-success
                     ::create-job-fail)}
          (not valid-form?)
          (let [checked-db (assoc db ::register/sub-db (toggle-job-field-errors sub-db))
                error (find-first-error-key checked-db register/job-fields-maps)]
            (merge
              {:db checked-db}
              (when error
                {:scroll-into-view (db/key->id error)})))
          (not valid-location?)
          {:db (assoc-in db [::register/sub-db ::register/location-error?] true)})))

(def company-fields-exclusions #{::company-name ::location})

(doseq [[_ k] register/company-fields]
  (when-not (company-fields-exclusions k)
    (reg-event-db
      k
      register-interceptors
      (fn [db [v]]
        (assoc db k v)))))

(doseq [[_ k] register/job-fields]
  (reg-event-db
    k
    register-interceptors
    (fn [db [v]]
      (assoc db k v))))

;; these events are manually specified
(reg-event-fx
  ::register/company-name
  register-interceptors
  (fn [{db :db} [v prevent-search?]]
    (let [searching? (not (or prevent-search? (clojure.string/blank? v)))
          cb-company (some #(when (= v (:name %)) %) (::register/company-suggestions db))
          logo       (when (and cb-company (:logo cb-company)) (str (:logo cb-company) "?size=128"))
          domain     (when (and cb-company (:domain cb-company)) (:domain cb-company))]
      (merge
        {:db  (assoc db
                     ::register/company-name v
                     ::register/company-logo logo
                     ::register/company-domain domain)}
        (if searching?
          {:http-xhrio (company/get-company-suggestions v ::set-company-suggestions)}
          {:dispatch [::set-company-suggestions []]})))))

(reg-event-fx
  ::register/location
  register-interceptors
  (fn [{db :db} [v prevent-search?]]
    (let [searching? (not (or prevent-search? (clojure.string/blank? v)))
          details (some #(when (= v (:description %)) %) (::register/location-suggestions db))]
      (merge
        {:db  (assoc db
                     ::register/location v
                     ::register/location-details nil
                     ::register/location-error? false)}
        (if searching?
          {:google/place-predictions {:input      v
                                      :on-success [::set-location-suggestions]
                                      :on-failure [:error/set-global "Failed to fetch address suggestions"
                                                   [::register/location v prevent-search?]]}}
          (merge {:dispatch [::set-location-suggestions []]}
                 (when details
                   {:google/place-details {:place-id   (:place_id details)
                                           :on-success [::fetch-place-details-success]
                                           :on-failure [:error/set-global "Failed to find address"
                                                        [::register/location v prevent-search?]]}})))))))

(reg-event-db
  ::fetch-place-details-success
  register-interceptors
  (fn [db [google-response]]
    (assoc db ::register/location-details (company/google-place->location google-response))))

(reg-event-db
  ::set-location-suggestions
  register-interceptors
  (fn [db [suggestions]]
    (assoc db ::register/location-suggestions (or suggestions []))))

(reg-event-db
  ::logo-upload-failure
  register-interceptors
  (fn [db _]
    (assoc db ::register/logo-uploading? false)))

(reg-event-fx
  ::logo-upload-success
  register-interceptors
  (fn [{db :db} [_filename {:keys [url]}]]
    {:db (assoc db
                ::register/company-logo url)
     :graphql {:query update-company-mutation
               :variables {:update_company {:logo url :id (::register/company-id db)}}
               :on-success [::update-company-logo-success url]
               :on-failure [::update-company-logo-failure]}}))

(reg-event-db
  ::update-company-logo-success
  register-interceptors
  (fn [db _]
    (assoc db ::register/logo-uploading? false)))

(reg-event-db
  ::update-company-logo-failure
  register-interceptors
  (fn [db _]
    (assoc db ::register/logo-uploading? false)))

(reg-event-db
  ::set-company-suggestions
  register-interceptors
  (fn [db [suggestions]]
    (assoc db ::register/company-suggestions suggestions)))

(reg-event-fx
  ::create-company-and-user-success
  db/default-interceptors
  (fn [{db :db} [data]]
    (let [company-id  (get-in data [:data :create_company_and_user :company :id])
          register-db (::register/sub-db db)
          logo        (::register/company-logo register-db)]
      {:db              (-> db
                            (assoc ::register/sub-db (-> register-db
                                                         (dissoc ::register/loading?
                                                                 ::register/checked-form)
                                                         (assoc ::register/company-id company-id
                                                                ::register/logo-uploading? (boolean logo))))
                            (update ::user/sub-db #(-> (get-in data [:data :create_company_and_user :user])
                                                       user/translate-user
                                                       (update ::user/welcome-msgs set)
                                                       (assoc ::user/company-id (get-in data [:data :create_company_and_user :company :id])))))
       :navigate        [:register-company
                         :params {:step :job-details}]
       :dispatch-n      (concat [[:user/init]]
                                (when logo [[:wh.company.events/fetch-clearbit-logo logo
                                             ::logo-upload-success ::logo-upload-failure]]))
       :analytics/track ["Company Created" {:id      company-id
                                            :name    (::register/company-name register-db)
                                            :user    {:id (get-in data [:data :create_company_and_user :user :id])}}]})))

(reg-event-fx
  ::create-job-success
  db/default-interceptors
  (fn [{db :db} _]
    {:navigate          [:register-company
                         :params {:step :complete}]
     :analytics/track   ["Job Created" (graphql/db->graphql-create-job-input db)]
     :dispatch-debounce {:id       :navigate-to-company-dashboard-after-pause
                         :dispatch [::complete-sign-up]
                         :timeout  (+ 3000 (rand-int 2000))}}))

(reg-event-fx
  ::complete-sign-up
  register-interceptors
  (fn [{db :db} _]
    {:db (register/basic-db {::register/step :complete})
     :navigate [:homepage]}))

(reg-event-fx
  ::scroll-to-form-error
  db/default-interceptors
  (fn [_ _]
    {:scroll-into-view ["company-signup-error-desktop" "company-signup-error-mobile"]}))

(defn fail
  [db resp]
  {:db                (-> db
                          (dissoc ::register/loading?)
                          (assoc ::register/error (util/gql-errors->error-key resp)))
   :dispatch-debounce {:id       :scroll-to-error-message
                       :dispatch [::scroll-to-form-error]
                       :timeout  100}})

(reg-event-fx
  ::create-company-and-user-fail
  register-interceptors
  (fn [{db :db} [resp]]
    (fail db resp)))

(reg-event-fx
  ::create-job-fail
  register-interceptors
  (fn [{db :db} [resp]]
    (fail db resp)))

(reg-event-db
  ::check
  register-interceptors
  (fn [db [field]]
    (check-field db field)))

(reg-event-fx
  ::next
  db/default-interceptors
  (fn [{db :db} _]
    (-> (progress-sign-up (register-subs/step db) db)
        (update :db #(update % ::register/sub-db dissoc ::register/error)))))

(reg-event-fx
  ::select-location-suggestion
  register-interceptors
  (fn [_ [new-location]]
    {:dispatch [::register/location new-location true]}))

(reg-event-fx
  ::select-company-suggestion
  register-interceptors
  (fn [db [new-company]]
    {:dispatch [::register/company-name new-company true]}))

(reg-event-db
  ::toggle-tags-collapsed
  register-interceptors
  (fn [db _]
    (update db ::register/tags-collapsed? not)))

(reg-event-fx
  ::toggle-tag
  register-interceptors
  (fn [{db :db} [tag]]
    {:db (update db ::register/tags util/toggle {:tag tag :selected true})
     :dispatch [::set-tag-search ""]}))

(reg-event-fx
  ::fetch-tags
  db/default-interceptors
  (fn [{db :db} _]
    (when-not (get-in db [::register/sub-db ::register/available-tags])
      {:graphql (fetch-tags ::fetch-tags-success)})))

(reg-event-db
  ::fetch-tags-success
  register-interceptors
  (fn [db [results]]
    (let [results (group-by :attr (get-in results [:data :jobs_search :facets]))
          results (->> (get results "tags")
                       (sort-by :count)
                       (map #(hash-map :tag (:value %)))
                       (reverse))]
      (assoc db ::register/available-tags results))))

(reg-event-db
  ::set-tag-search
  register-interceptors
  (fn [db [tag-search]]
    (assoc db ::register/tag-search tag-search)))

(defn db->analytics-data
  [db]
  {:package (get-in db [::db/query-params "package"])
   :billing-period (get-in db [::db/query-params "billing"])})

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::register/sub-db (register/default-db db))
     :analytics/track ["Company Registration Started" (db->analytics-data db)]}))

(defmethod on-page-load :register-company [db]
  [[::initialize-db]
   [:google/load-maps]
   [::fetch-tags]])
