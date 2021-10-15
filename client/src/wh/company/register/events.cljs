(ns wh.company.register.events
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.logo]
    [wh.company.common :as company]
    [wh.company.register.db :as register]
    [wh.company.register.graphql :as graphql]
    [wh.company.register.subs :as register-subs]
    [wh.db :as db]
    [wh.graphql.company :refer [update-company-mutation]]
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

(defn find-first-error-key
  [db fields-maps]
  (some (fn [{:keys [key spec]}] (when (register-subs/error-query db key spec) key)) fields-maps))

(def company-fields-exclusions #{::company-name ::location})

(doseq [[_ k] register/company-fields]
  (when-not (company-fields-exclusions k)
    (reg-event-db
      k
      register-interceptors
      (fn [db [v]]
        (assoc db k v)))))

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

(reg-event-db
  ::logo-upload-failure
  register-interceptors
  (fn [db _]
    (assoc db ::register/logo-uploading? false)))

(reg-event-fx
  ::logo-upload-success
  register-interceptors
  (fn [{db :db} [_filename {:keys [url]}]]
    {:db (assoc db ::register/company-logo url)
     :graphql {:query update-company-mutation
               :variables {:update_company {:logo url :id (::register/company-id db)}}
               :on-success [::update-company-logo-success url]
               :on-failure [::update-company-logo-failure]}}))

(reg-event-db
  ::update-company-logo-success
  db/default-interceptors
  (fn [db [url]]
    (-> db
        (assoc-in [:wh.company.dashboard.db/sub-db :wh.company.dashboard.db/logo] url)
        (assoc-in [::register/sub-db ::register/logo-uploading?] false))))

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
          email (get-in data [:data :create_company_and_user :user :email])
          register-db (::register/sub-db db)
          logo        (::register/company-logo register-db)
          db (-> db
                 (assoc ::register/sub-db (-> register-db
                                              (dissoc ::register/loading?
                                                      ::register/checked-form)
                                              (assoc ::register/company-id company-id
                                                     ::register/logo-uploading? (boolean logo))))
                 (update ::user/sub-db #(-> (get-in data [:data :create_company_and_user :user])
                                            user/translate-user
                                            (update ::user/onboarding-msgs set)
                                            (assoc ::user/company-id (get-in data [:data :create_company_and_user :company :id])))))]
      {:db                        db
       :navigate                  [:homepage]
       :dispatch-n                (concat []
                                          (when logo [[:wh.common.logo/fetch-clearbit-logo logo
                                                       ::logo-upload-success ::logo-upload-failure]]))
       :register/track-account-created {:source :email :email email :type "company"}
       :analytics/track           ["Company Created" {:id   company-id
                                                      :name (::register/company-name register-db)
                                                      :user {:id (get-in data [:data :create_company_and_user :user :id])}}]})))

(reg-event-fx
  ::scroll-to-form-error
  db/default-interceptors
  (fn [_ _]
    {:scroll-into-view ["company-signup-error-desktop" "company-signup-error-mobile"]}))

(reg-event-fx
  ::create-company-and-user-fail
  register-interceptors
  (fn [{db :db} [resp]]
    {:db                (-> db
                            (dissoc ::register/loading?)
                            (assoc ::register/error (util/gql-errors->error-key resp)))
     :dispatch-debounce {:id       :scroll-to-error-message
                         :dispatch [::scroll-to-form-error]
                         :timeout  100}}))

(reg-event-db
  ::check
  register-interceptors
  (fn [db [field]]
    (check-field db field)))

(reg-event-fx
  ::register
  db/default-interceptors
  (fn [{db :db} _]
    (let [sub-db (::register/sub-db db)
          db     (update db :db #(update % ::register/sub-db dissoc ::register/error))]
      (if (register/valid-company-form? sub-db)
        {:db                          (assoc-in db [::register/sub-db ::register/loading?] true)
         :graphql                     (graphql/create-company-and-user-mutation
                                        sub-db
                                        ::create-company-and-user-success
                                        ::create-company-and-user-fail)
         :analytics/agree-to-tracking true}
        (let [checked-db (assoc db ::register/sub-db (toggle-company-field-errors sub-db))
              error      (find-first-error-key checked-db register/company-fields-maps)]
          (merge
            {:db checked-db}
            (when error
              {:scroll-into-view (db/key->id error)})))))))

(reg-event-fx
  ::select-company-suggestion
  register-interceptors
  (fn [_ [new-company]]
    {:dispatch [::register/company-name new-company true]}))

(reg-event-fx
  ::select-source-suggestion
  register-interceptors
  (fn [{db :db} [new-source]]
    (let [other-mode? (= "other" new-source)]
      (cond-> {:dispatch [::register/source (if other-mode? "" new-source) true]
               :db       (assoc db ::register/other-mode? other-mode?)}
              other-mode? (assoc :focus "#_wh_company_register_db_source input")))))

(defn db->analytics-data                                    ;TODO this only works if user arrives from pricing page
  [db]
  {:package        (get-in db [::db/query-params "package"])
   :billing-period (get-in db [::db/query-params "billing"])})

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::register/sub-db (register/default-db db))
     :analytics/track ["Company Registration Started" (db->analytics-data db)]}))

(defmethod on-page-load :register-company [db]
  [[::initialize-db]])
