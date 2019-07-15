(ns wh.company.edit.events
  (:require
    [ajax.json :as ajax-json]
    [camel-snake-kebab.core :as c]
    [cljs-time.coerce :as tc]
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [clojure.set :as set]
    [clojure.string :as str]
    [goog.Uri :as uri]
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.data :refer [get-manager-email get-manager-name]]
    [wh.common.logo]
    [wh.common.upload :as upload]
    [wh.company.common :as company]
    [wh.company.edit.db :as edit]
    [wh.company.payment.db :as payment]
    [wh.company.payment.events :as payment-events]
    [wh.db :as db]
    [wh.graphql.company :refer [create-company-mutation update-company-mutation update-company-mutation-with-fields add-new-company-user-mutation delete-user-mutation company-query-with-payment-details all-company-jobs-query update-job-mutation publish-company-profile-mutation delete-integration-mutation]]
    [wh.pages.core :as pages :refer [on-page-load]]
    [wh.user.db :as user]
    [wh.util :as util]))

(defn company-id
  [db]
  (or (get-in db [::db/page-params :id])
      (get-in db [::user/sub-db ::user/company-id])))

(def company-interceptors (into db/default-interceptors
                                [(path ::edit/sub-db)]))

(defn check-field
  [db field]
  (if (edit/check-field db field (get edit/fields field))
    (if (::edit/form-errors db)
      (update db ::edit/form-errors disj field)
      db)
    (if (::edit/form-errors db)
      (update db ::edit/form-errors conj field)
      (assoc db ::edit/form-errors #{field}))))

(reg-event-db
  ::initialize-db
  company-interceptors
  (fn [db _] (edit/initial-db db)))

(doseq [field edit/field-names
        :when (get-in edit/fields [field :event?] true)
        :let [event-name (keyword "wh.company.edit.events" (str "edit-" (name field)))
              db-field (keyword "wh.company.edit.db" (name field))]]
  (reg-event-db event-name
                company-interceptors
                (fn [db [new-value]]
                  (assoc db db-field new-value))))

(reg-event-db
  ::edit-description-html
  company-interceptors
  (fn [db [new-value]]
    ;; Quill returns `<p><br></p>` in place of empty string so we need to
    ;; check for this and replace if we see it
    (let [stripped-value (str/replace new-value #"^\<p\>\<br\>\</p\>" "")]
      (assoc db ::edit/description-html stripped-value))))

(reg-event-fx
  ::edit-name
  company-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::edit/name new-value)}
      (if-not (clojure.string/blank? new-value)
        {:http-xhrio (company/get-company-suggestions new-value ::set-suggestions)}
        {:dispatch [::set-suggestions []]}))))

(reg-event-db
  ::check
  company-interceptors
  (fn [db [field]]
    (check-field db field)))

(reg-event-db
  ::set-suggestions
  company-interceptors
  (fn [db [suggestions]]
    (assoc db ::edit/suggestions suggestions)))

(reg-event-fx
  ::select-suggestion
  company-interceptors
  (fn [{db :db} [id]]
    (let [suggestions (::edit/suggestions db)
          suggestion (first (filter #(= (:domain %) id) suggestions))
          logo (when-let [logo (:logo suggestion)]
                 (str logo "?size=256"))]
      (when suggestion
        (merge
          {:db (assoc db
                      ::edit/logo nil
                      ::edit/logo-uploading? (boolean logo)
                      ::edit/suggestions []
                      ::edit/name (:name suggestion))}
          (when logo
            {:dispatch [:wh.common.logo/fetch-clearbit-logo logo
                        ::logo-upload-success ::logo-upload-failure]}))))))

(reg-event-db
  ::logo-upload-start
  company-interceptors
  (fn [db _]
    (assoc db ::edit/logo-uploading? true)))

(reg-event-fx
  ::logo-upload-success
  company-interceptors
  (fn [{db :db} [_ {:keys [url]}]]
    {:db (assoc db
                ::edit/logo url
                ::edit/logo-uploading? false)
     :dispatch [::check ::edit/logo]}))

(reg-event-db
  ::logo-upload-failure
  company-interceptors
  (fn [{db :db} _]
    (assoc db ::edit/logo-uploading? false)))

(defn naive-diff
  "A top-level comparison of key values across two maps. First arg should be the one you expect to have changed.
   Returns a map containing keys and values that appear in `new` and differ from `old`."
  [new old]
  (if-not old
    new
    (not-empty
     (reduce
      (fn [new k]
        (if (= (get new k) (get old k))
          (dissoc new k)
          new))
      new
      (set (clojure.set/union (keys old) (keys new)))))))

(defn subdb->graphql-company [db id]
  (let [c (some-> (naive-diff
                   {:name             (::edit/name db)
                    :logo             (::edit/logo db)
                    :vertical         (::edit/vertical db)
                    :auto-approve     (::edit/auto-approve db)
                    :profile-enabled  (::edit/profile-enabled db)
                    :description-html (::edit/description-html db)
                    :manager          (get-manager-email (::edit/manager db))
                    :package          (name (or (::edit/package db) ""))}
                   (::edit/original-company db))
                  (set/rename-keys {:description-html :descriptionHtml
                                    :auto-approve     :autoApprove
                                    :profile-enabled  :profileEnabled}))]
    (if (and c id)
      (assoc c :id id)
      (dissoc c :package :profileEnabled))))

(defn subdb->graphql-company-user [db]
  {:name (str/trim (::edit/new-user-name db))
   :email (str/trim (::edit/new-user-email db))
   :companyId (::edit/id db)})

(defn map-keys->kebab-case
  [m]
  (reduce-kv (fn [a k v] (assoc a (c/->kebab-case-keyword k) v)) {} m))

(reg-event-fx
  ::save-company
  db/default-interceptors
  (fn [{db :db} _]
    (if-let [errors (edit/form-errors (::edit/sub-db db) (= :create-company (::db/page db)))]
      {:db (-> db
               (assoc-in [::edit/sub-db ::edit/form-errors] errors)
               (assoc-in [::edit/sub-db ::edit/error] nil))
       :scroll-into-view (db/key->id (first errors))}
      (let [id (get-in db [::edit/sub-db ::edit/id])
            company-update (subdb->graphql-company (::edit/sub-db db) id)
            company-update-kebab (map-keys->kebab-case company-update)]
        (when company-update
          {:db (-> db
                   (assoc-in [::edit/sub-db ::edit/saving?] true)
                   (assoc-in [::edit/sub-db ::edit/error] nil)
                   (update-in [::edit/sub-db ::edit/original-company] merge company-update-kebab))
           :graphql {:query (if id update-company-mutation create-company-mutation)
                     :variables {(if id :update_company :create_company) company-update}
                     :on-success [::save-company-success company-update-kebab]
                     :on-failure [::save-company-failure]}})))))

(reg-event-fx
  ::save-company-success
  db/default-interceptors
  (fn [{db :db} [company {data :data}]]
    (let [new-id (get-in data [:create_company :id])]
      (merge
        {:db (-> db
                 (assoc-in [::edit/sub-db ::edit/saving?] false)
                 (assoc-in [::user/sub-db ::user/company] company))}
        (when new-id
          {:navigate [:company-dashboard :params {:id new-id}]})))))

(reg-event-db
  ::save-company-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db ::edit/saving? false
           ::edit/error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (company-id db)]
      {:db (update db ::edit/sub-db merge {::edit/loading? true
                                           ::edit/error nil})
       :graphql {:query      (company-query-with-payment-details id)
                 :on-success [::fetch-company-success]
                 :on-failure [::fetch-company-failure]}})))

(defn process-invoice
  [invoice]
  (let [i  (str (:amount invoice))
        ds (subs i (- (count i) 2))
        is (subs i 0 (- (count i) 2))]
    (-> invoice
        (assoc  :amount (js/parseFloat (str is "." ds)))
        (update :date #(tf/unparse (tf/formatter "MM/YYYY") (tc/from-long (* % 1000)))))))

(reg-event-fx
  ::fetch-company-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [company (-> (get-in resp [:data :company])
                      (cases/->kebab-case))]
      {:db
       (-> db
           (assoc-in [::user/sub-db ::user/company-connected-github?]
                     (:connected-github company))
           (update ::edit/sub-db
                   (fn [sub-db]
                     (-> sub-db
                         (merge (util/namespace-map "wh.company.edit.db" company))
                         (update ::edit/package keyword)
                         (update ::edit/manager get-manager-name)
                         (update-in [::edit/integrations :email] set)
                         (util/update-in* [::edit/payment :billing-period] keyword)
                         (util/update-in* [::edit/payment :coupon :duration] keyword)
                         (util/update-in* [::edit/invoices] #(map process-invoice %))
                         (assoc  ::edit/original-company company
                                 ::edit/loading? false)))))})))

(reg-event-db
  ::fetch-company-failure
  company-interceptors
  (fn [db _]
    (assoc db
           ::edit/loading? false
           ::edit/error :failed-to-fetch-company)))

(reg-event-db
  ::update-company-success
  db/default-interceptors
  (fn [db [{:keys [package] :as delta} _resp]]
    (let [db (assoc-in db [::edit/sub-db ::edit/disable-loading?] false)]
      (if (user/company? db)
        (update-in db [::user/sub-db ::user/company] merge delta)
        (if (and (user/admin? db) package)
          (assoc-in db [::edit/sub-db ::edit/package] (keyword package))
          (update db ::edit/sub-db merge delta))))))

(reg-event-fx
  ::update-company-failure
  company-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::edit/error (util/gql-errors->error-key resp))}))

(reg-event-db
  ::request-update-company-success
  db/default-interceptors
  (fn [db _] db))

(reg-event-fx
  ::request-update-company-failure
  company-interceptors
  (fn [_ [resp]]
    {:dispatch [:error/set-global "An error occurred while requesting the company's details be updated."]}))

(reg-event-fx
  ::select-manager
  company-interceptors
  (fn [{db :db} [manager]]
    (merge
      {:db (assoc db ::edit/manager manager)}
      (when-let [id (::edit/id db)]
        (when-let [manager-email (get-manager-email manager)]
          {:graphql {:query update-company-mutation
                     :variables {:update_company
                                 {:id id
                                  :manager manager-email}}
                     :on-success [::update-company-success {:manager manager-email}]
                     :on-failure [::update-company-failure]}})))))

(reg-event-fx
  ::add-new-user
  company-interceptors
  (fn [{db :db} _]
    (if-let [errors (edit/user-form-errors db)]
      {:db (-> db
               (assoc ::edit/form-errors errors)
               (assoc ::edit/user-error nil))
       :scroll-into-view (db/key->id (first errors))}
      {:db (-> db
               (assoc ::edit/user-adding? true)
               (assoc ::edit/user-error nil))
       :graphql {:query add-new-company-user-mutation
                 :variables {:create_company_user (subdb->graphql-company-user db)}
                 :on-success [::add-company-user-success]
                 :on-failure [::add-company-user-failure]}})))

(reg-event-fx
  ::delete-integration
  company-interceptors
  (fn [{db :db} [integration]]
    (let []
      {:db (assoc db ::edit/deleting-integration? true)
       :graphql {:query delete-integration-mutation
                 :variables {:company_id (::edit/id db)
                             :integration (name integration)}
                 :on-success [::delete-integration-success integration]
                 :on-failure [::delete-integration-failure integration]}})))

(reg-event-fx
  ::delete-integration-success
  company-interceptors
  (fn [{db :db} [integration]]
    {:db (-> db
             (assoc ::edit/deleting-integration? false)
             (update ::edit/integrations dissoc integration))
     :dispatch-n [[:wh.events/scroll-to-top]
                  [:success/set-global "Integration successfully deleted."]]}))

(reg-event-fx
  ::delete-integration-failure
  company-interceptors
  (fn [{db :db} [integration]]
    {:db (assoc db ::edit/deleting-integration? false)
     :dispatch-n [[:wh.events/scroll-to-top]
                  [:error/set-global "An error occurred while deleting the integration." [::delete-integration integration]]]}))

(reg-event-fx
  ::add-company-user-success
  company-interceptors
  (fn [{db :db} [{{user :create_company_user} :data}]]
    (merge {:db (-> db
                    (assoc ::edit/user-adding? false
                           ::edit/new-user-name ""
                           ::edit/new-user-email "")
                    (update ::edit/users conj user))}
           (when (or (not (::edit/users db))
                     (zero? (count (::edit/users db))))
             {:dispatch [::toggle-user-notification (:email user)]}))))

(reg-event-db
  ::add-company-user-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db
           ::edit/user-adding? false
           ::edit/user-error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::remove-user
  company-interceptors
  (fn [db [id]]
    {:confirm {:message "Are you sure you want to remove this user?"
               :on-ok [::remove-user-confirm id]}}))

(reg-event-fx
  ::remove-user-confirm
  company-interceptors
  (fn [{db :db} [id]]
    (let [email (some #(when (= id (:id %)) (:email %)) (::edit/users db))]
      {:db (-> db
               (update ::edit/users #(remove (fn [user] (= id (:id user))) %))
               (update-in [::edit/integrations :email] disj email))
       :graphql {:query delete-user-mutation
                 :variables {:id id}
                 :on-success [::remove-user-success]
                 :on-failure [::remove-user-failure]}})))

(reg-event-db
  ::remove-user-success
  company-interceptors
  (fn [db [resp]] db))

(reg-event-db
  ::remove-user-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db ::edit/user-error "Failed to remove user")))

(reg-event-db
  ::toggle-integration-popup
  company-interceptors
  (fn [db [show-popup?]]
    (assoc db ::edit/show-integration-popup? show-popup?)))

(reg-event-fx
  ::toggle-user-notification
  company-interceptors
  (fn [{db :db} [email]]
    (let [id (::edit/id db)
          new-db (update-in db [::edit/integrations :email] util/toggle email)]
      {:db new-db
       :graphql {:query update-company-mutation
                 :variables {:update_company
                             {:id id
                              :integrations (select-keys (::edit/integrations new-db) [:email])}}
                 :on-success [::update-notification-success]
                 :on-failure [::update-notification-failure]}})))

(reg-event-db
  ::update-notification-success
  company-interceptors
  (fn [db _] db))

(reg-event-db
  ::update-notification-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db ::edit/user-error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::set-page-selection
  db/default-interceptors
  (fn [{db :db} [page]]
    (if (= :admin-edit-company (::db/page db))
      {:navigate [:admin-edit-company
                  :params {:id (get-in db [::db/page-params :id])}
                  :query-params {:page page}]}
      {:navigate [(::db/page db)
                  :query-params {:page page}]})))

(reg-event-fx
  ::update-card-details
  db/default-interceptors
  (fn [{db :db} _]
    {:db (update db ::edit/sub-db dissoc ::edit/update-card-details-status)
     :graphql  {:query      (update-in update-company-mutation
                                       [:venia/queries 0 2]
                                       conj [:payment [[:card [:last4Digits :brand [:expiry [:month :year]]]]]])
                :variables  {:update_company
                             {:id            (company-id db)
                              :paymentToken  (get-in db [::payment/sub-db ::payment/token])}}
                :on-success [::update-card-details-success]
                :on-failure [::update-card-details-failure]}}))

(defn success-status
  [msg]
  {:status :good
   :message msg})

(defn failure-status
  [msg]
  {:status :bad
   :message msg})

(reg-event-fx
  ::update-card-details-success
  company-interceptors
  (fn [{db :db} [{{company :update_company} :data}]]
    {:db (assoc db
                ::edit/payment (merge-with merge (::edit/payment db) (cases/->kebab-case (:payment company)))
                ::edit/update-card-details-status (success-status "Card details have been updated!"))
     :dispatch [:wh.company.payment.events/set-stripe-card-form-enabled true]}))

(reg-event-fx
  ::update-card-details-failure
  company-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::edit/update-card-details-status (failure-status "Card details could not be updated"))
     :dispatch-n [[:wh.company.payment.events/set-stripe-card-form-enabled true]]}))

(reg-event-db
  ::set-billing-period
  company-interceptors
  (fn [db [billing-period]]
    (assoc db ::edit/pending-billing-period billing-period)))

(reg-event-fx
  ::update-billing-period
  db/default-interceptors
  (fn [{db :db} _]
    {:db (-> db
             (update ::edit/sub-db dissoc ::edit/update-billing-period-status)
             (assoc-in [::edit/sub-db ::edit/update-billing-period-loading?] true))
     :graphql  {:query      update-company-mutation
                :variables  {:update_company
                             {:id            (company-id db)
                              :billingPeriod (get-in db [::edit/sub-db ::edit/pending-billing-period])}}
                :on-success [::update-billing-period-success]
                :on-failure [::update-billing-period-failure]}}))

(reg-event-fx
  ::update-billing-period-success
  company-interceptors
  (fn [{db :db} [{{company :update_company} :data}]]
    {:db (-> db
             (assoc-in [::edit/payment :billing-period] (::edit/pending-billing-period db))
             (assoc ::edit/update-billing-period-loading? false
                    ::edit/pending-billing-period nil
                    ::edit/update-billing-period-status (success-status "Billing period has been updated!")))}))

(reg-event-fx
  ::update-billing-period-failure
  company-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db
                ::edit/update-billing-period-loading? false
                ::edit/pending-billing-period nil
                ::edit/update-billing-period-status (failure-status "Billing period could not be updated"))}))

(reg-event-fx
  ::save-paid-offline-until
  db/default-interceptors
  (fn [{db :db} [year month day]]
    (let [timestamp (tf/unparse (tf/formatters :date-time) (t/date-time year month day))]
      {:db (-> db
               (assoc-in [::edit/sub-db ::edit/paid-offline-until-loading?] true)
               (assoc-in [::edit/sub-db ::edit/paid-offline-until-error] nil))
       :graphql {:query update-company-mutation
                 :variables {:update_company
                             {:id (get-in db [::db/page-params :id]) ;; admins only
                              :paidOfflineUntil timestamp}}
                 :on-success [::save-paid-offline-until-success timestamp]
                 :on-failure [::save-paid-offline-until-failure]}})))

(reg-event-fx
  ::cancel-plan
  db/default-interceptors
  (fn [{db :db} [immediately?]]
    {:db (assoc-in db [::edit/sub-db ::edit/cancel-plan-loading?] true)
     :dispatch [::show-cancel-plan-dialog false]
     :graphql {:query {:venia/operation {:operation/type :mutation
                                         :operation/name "CancelPlan"}
                       :venia/variables [{:variable/name "id"
                                          :variable/type :String!}
                                         {:variable/name "immediately"
                                          :variable/type :Boolean!}]
                       :venia/queries [[:cancel_plan {:id :$id :immediately :$immediately}]]}
               :variables {:id (get-in db [::db/page-params :id])
                           :immediately immediately?}
               :on-success [::cancel-plan-success]
               :on-failure [::cancel-plan-failure]}}))

(reg-event-db
  ::cancel-plan-success
  company-interceptors
  (fn [db [resp]]
    (-> db
        (assoc ::edit/cancel-plan-loading? false)
        (assoc-in [::edit/payment :expires] (get-in resp [:data :cancel_plan])))))

(reg-event-fx
  ::cancel-plan-failure
  company-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::edit/cancel-plan-loading? false)
     :dispatch [:error/set-global "An error occurred while cancelling the company's plan."]}))

(reg-event-db
  ::save-paid-offline-until-success
  company-interceptors
  (fn [db [timestamp _resp]]
    (assoc db
           ::edit/paid-offline-until-loading? false
           ::edit/paid-offline-until timestamp)))

(reg-event-db
  ::save-paid-offline-until-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db
           ::edit/paid-offline-until-loading? false
           ::edit/paid-offline-until-error (some-> resp :errors first :key))))

(reg-event-fx
  ::disable
  company-interceptors
  (fn [{db :db} [disable?]]
    {:db (assoc db ::edit/disable-loading? true)
     :graphql {:query (update-company-mutation-with-fields [:disabled])
               :variables {:update_company
                           {:id (::edit/id db)
                            :disabled disable?}}
               :on-success [::update-company-success {::edit/disabled disable?}]
               :on-failure [::update-company-failure]}}))

(reg-event-db
  ::show-cancel-plan-dialog
  company-interceptors
  (fn [db [show?]]
    (assoc db ::edit/showing-cancel-plan-dialog? show?)))

(reg-event-fx
  ::upgrade
  company-interceptors
  (fn [{db :db} _]
    {:navigate [:payment-setup :params {:step :select-package} :query-params {:action :integrations}]}))

(reg-event-fx
  ::toggle-profile
  company-interceptors
  (fn [{db :db} _]
    (let [new-value (not (::edit/profile-enabled db))]
      {:db (assoc db ::edit/profile-enabled-loading? true)
       :graphql {:query publish-company-profile-mutation
                 :variables {:id (::edit/id db)
                             :profile_enabled new-value}
                 :on-success [::toggle-profile-success]
                 :on-failure [::toggle-profile-failure]}})))

(reg-event-db
  ::toggle-profile-success
  company-interceptors
  (fn [db [resp]]
    (assoc db
           ::edit/profile-enabled-loading? false
           ::edit/profile-enabled (get-in resp [:data :publish_profile :profile_enabled]))))

(reg-event-db
  ::toggle-profile-failure
  company-interceptors
  (fn [db [resp]]
    (assoc db
           ::edit/profile-enabled-loading? false
           ::edit/profile-enabled-error (some-> resp :errors first :key))))

(reg-event-fx
  ::apply-coupon
  db/default-interceptors
  (fn [{db :db} _]
    (let [coupon-code (get-in db [::payment/sub-db ::payment/coupon-code])]
      {:db (-> db
               (assoc-in [::payment/sub-db ::payment/coupon-loading?] true)
               (assoc-in [::edit/sub-db ::edit/coupon-apply-success?] false)
               (update ::payment/sub-db dissoc ::payment/coupon-error))
       :graphql {:query payment-events/coupon-check-query
                 :variables {:code coupon-code}
                 :on-success [::check-coupon-success]
                 :on-failure [::check-coupon-failure {:code coupon-code :message "Code not valid!"} ]}})))

(reg-event-fx
  ::check-coupon-success
  company-interceptors
  (fn [{db :db} [resp]]
    (let [coupon (-> resp
                     (get-in [:data :coupon])
                     (cases/->kebab-case)
                     (update :duration keyword))]
      {:graphql {:query (update-company-mutation-with-fields [[:payment [[:coupon [:discountAmount :discountPercentage :description :duration]]]]])
                 :variables {:update_company
                             {:id (::edit/id db)
                              :couponCode (:code coupon)}}
                 :on-success [::apply-coupon-success coupon] ;; TODO don't send coupon to success, use the one from response...
                 :on-failure [::check-coupon-failure nil]}})))

(reg-event-fx
  ::check-coupon-failure
  db/default-interceptors
  (fn [{db :db} [coupon resp]]
    {:db (-> db
             (assoc-in [::payment/sub-db ::payment/coupon-loading?] false)
             (assoc-in [::payment/sub-db ::payment/coupon-error] (or (:message coupon)
                                                                     "Failed to apply coupon!")))}))

(reg-event-db
  ::apply-coupon-success
  db/default-interceptors
  (fn [db [coupon resp]]
    (-> db
        (assoc-in [::payment/sub-db ::payment/coupon-loading?] false)
        (assoc-in [::edit/sub-db ::edit/payment :coupon] coupon)
        (assoc-in [::edit/sub-db ::edit/coupon-apply-success?] true)
        (util/update-in* [::payment/sub-db ::payment/company] dissoc :id)))) ;; this causes payment screen to re-fetch company

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; THIS WILL MOVE TO COMPANY PROFILE


(defmethod on-page-load :create-company [db]
  [[::initialize-db]])

(defn edit-events
  [db]
  [[::initialize-db]
   [::fetch-company]])

(defmethod on-page-load :edit-company [db]
  (let [integration (get-in db [::db/query-params "integration"])
        status (get-in db [::db/query-params "status"])]
    (concat (edit-events db)
            (when (= status "success")
              [[:success/set-global (str "The " (str/capitalize integration) " integration is now enabled")]])
            (when (= status "error")
              [[:error/set-global (str "There was an error connecting your " (str/capitalize integration))]]))))

(defmethod on-page-load :admin-edit-company [db]
  (edit-events db))
