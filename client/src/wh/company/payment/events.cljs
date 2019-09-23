(ns wh.company.payment.events
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.company.payment.db :as payment]
    [wh.company.payment.subs :as subs]
    [wh.db :as db]
    [wh.graphql.company :refer [company-query job-query update-company-mutation update-job-mutation]]
    [wh.jobs.job.db :as job]
    [wh.jobs.job.events :refer [process-publish-role-intention]]
    [wh.pages.core :as pages :refer [on-page-load force-scroll-to-top!]]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def payment-interceptors (into db/default-interceptors
                                [(path ::payment/sub-db)]))

(def job-fields [:id :slug :title :verticals :valid])

(def company-fields [:id :name :package :permissions :disabled :freeTrialStarted :slug
                     [:jobs {:pageSize 5 :pageNumber 1 :published false}
                      [[:jobs [:id :title :published]]]]
                     [:nextInvoice [:amount [:coupon [:discountAmount :discountPercentage :duration :description]]]]
                     [:payment [:billingPeriod :expires [:card [:last4Digits :brand [:expiry [:month :year]]]]
                                [:coupon [:discountAmount :discountPercentage :duration :description]]]]
                     [:offer [:recurringFee :placementPercentage :acceptedAt]]
                     [:pendingOffer [:recurringFee :placementPercentage]]])

(def update-company-mutation+
  (update-in update-company-mutation [:venia/queries 0] assoc 2 company-fields))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql {:query      (company-query (subs/company-id db) company-fields)
               :on-success [::fetch-company-success]
               :on-failure [::fetch-company-failure]}
     :db      (update db ::payment/sub-db dissoc ::payment/company)}))

(reg-event-fx
  ::fetch-job
  db/default-interceptors
  (fn [{db :db} _]
    {:graphql {:query      (job-query (subs/job-id db) job-fields)
               :on-success [::fetch-job-success]
               :on-failure [::fetch-job-failure]}
     :db      (update db ::payment/sub-db dissoc ::payment/job)}))

(reg-event-fx
  ::fetch-company-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [new-db (update db ::payment/sub-db
                         assoc ::payment/company (-> (get-in resp [:data :company])
                                                     (cases/->kebab-case)
                                                     (update :package keyword)
                                                     (util/update-in* [:payment :coupon :duration] keyword)
                                                     (util/update-in* [:next-invoice :coupon :duration] keyword)
                                                     (update :permissions #(set (map keyword %)))))]
      (merge {:db new-db}
             (when (and (= :pay-confirm (subs/payment-step db)) (subs/upgrading? new-db))
               {:dispatch [::estimate-subscription-change]})))))

(reg-event-db
  ::fetch-company-failure
  payment-interceptors
  (fn [db [resp]]
    (assoc db ::payment/error :failed-to-fetch-company)))

(reg-event-fx
  ::fetch-job-success
  payment-interceptors
  (fn [{db :db} [resp]]
    (let [job (-> (get-in resp [:data :job])
                  (cases/->kebab-case))]
      (if (:valid job)
        {:db (assoc db ::payment/job job)}
        {:navigate [:edit-job
                    :query-params {:save 1}
                    :params {:id (:id job)}]}))))

(reg-event-db
  ::fetch-job-failure
  payment-interceptors
  (fn [db [resp]]
    (assoc db ::payment/error :failed-to-fetch-job)))

(reg-event-fx
  ::set-billing-period
  db/default-interceptors
  (fn [{db :db} [billing-period]]
    {:navigate [:payment-setup
                :query-params (assoc (::db/query-params db) "billing" billing-period)
                :params {:step (subs/payment-step db)} ]}))

(reg-event-fx
  ::confirm-free
  db/default-interceptors
  (fn [{db :db} _]
    {:db      (assoc-in db [::payment/sub-db ::payment/waiting?] true)
     :graphql {:query      update-company-mutation+
               :variables  {:update_company
                            {:id      (subs/company-id db)
                             :package :free}}
               :on-success [::update-company-success (subs/job-id db) (subs/action db)]
               :on-failure [::update-company-failure]}}))

(reg-event-fx
  ::estimate-subscription-change
  db/default-interceptors
  (fn [{db :db} _]
    (when-not (= :free (subs/company-package db))
      (let [package (subs/package db)
            billing-period (subs/billing-period db)]
        {:graphql {:query {:venia/queries [[:estimate_subscription_change {:package package} [:description :amount :proration [:period [:start]]]]]}
                   :on-success [::estimate-subscription-change-success]
                   :on-failure [::estimate-subscription-change-failure]}}))))

(reg-event-db
  ::estimate-subscription-change-success
  payment-interceptors
  (fn [db [{:keys [data]}]]
    (assoc db ::payment/estimate (:estimate_subscription_change data))))

(reg-event-db
  ::estimate-subscription-change-failure
  payment-interceptors
  (fn [db _]
    (assoc db ::payment/error :failed-to-estimate-upgrade)))

(defmulti progress-payment-setup
  (fn [step _ _] step))

(defmethod progress-payment-setup
  :select-package
  [_ db {:keys [package billing-period]}]
  (merge {:navigate [:payment-setup
                     :query-params (cond-> (::db/query-params db)
                                     package        (assoc "package" (name package))
                                     billing-period (assoc "billing" (name billing-period)))
                     :params {:step :pay-confirm}]}))

(defmethod progress-payment-setup
  :pay-confirm
  [_ db args]
  {:graphql {:query      update-company-mutation+
             :variables  {:update_company
                          (merge {:id (subs/company-id db)}
                                 (when-let [p (subs/package db)]
                                   (merge {:package p}
                                          (when (get-in db [::payment/sub-db
                                                            ::payment/company
                                                            :pending-offer])
                                            {:acceptOffer true})))
                                 (when-let [bp (subs/billing-period db)]
                                   {:billingPeriod bp})
                                 (when-let [coupon (get-in db [::payment/sub-db ::payment/current-coupon])]
                                   {:couponCode (:code coupon)})
                                 (when-let [token (get-in db [::payment/sub-db ::payment/token])]
                                   {:paymentToken  token}))}
             :timeout 25000 ;; 25s timeout just for this one, in case Stripe is slow
             :on-success [::update-company-success (subs/job-id db) (subs/action db)]
             :on-failure [::update-company-failure]}})

(reg-event-fx
  ::update-company-success
  db/default-interceptors
  (fn [{db :db} [job-id action resp]]
    (let [company (get-in resp [:data :update_company])]
      (merge {:db (-> db
                      (assoc-in [::user/sub-db ::user/company :permissions] (set (map keyword (:permissions company))))
                      (update ::job/sub-db dissoc ::job/id) ;; causes job to be reloaded
                      (update-in [::payment/sub-db ::payment/company] dissoc :id))} ;; causes company to be reloaded
             (if (and job-id (= :publish action))
               {:graphql {:query      update-job-mutation
                          :variables  {:update_job
                                       {:id        job-id
                                        :companyId (subs/company-id db)
                                        :published true}}
                          :on-success [::publish-job-success]
                          :on-failure [::publish-job-failure]}}
               {:navigate [:payment-setup
                           :query-params (::db/query-params db)
                           :params {:step :pay-success}]})))))

(reg-event-fx
  ::publish-job-success
  db/default-interceptors
  (fn [{db :db} [_resp]]
    {:dispatch [:company/refresh-tasks]
     :navigate [:payment-setup
                :query-params (::db/query-params db)
                :params {:step :pay-success}]}))

(reg-event-db
  ::update-company-failure
  payment-interceptors
  (fn [db [resp]]
    (let [error-message (-> resp :errors first :args :error-message)]
      (assoc db
             ::payment/error :failed-payment
             ::payment/error-message error-message))))

(reg-event-db
  ::publish-job-failure
  payment-interceptors
  (fn [db _resp]
    (assoc db ::payment/error :failed-job-publish)))

(reg-event-fx
  ::setup-step-forward
  db/default-interceptors
  (fn [{db :db} [args]]
    (force-scroll-to-top!)
    (progress-payment-setup (subs/payment-step db) db args)))

(reg-event-db
  ::save-token
  payment-interceptors
  (fn [db [token]]
    (assoc db ::payment/token token)))

(reg-event-db
  ::set-stripe-card-form-enabled
  payment-interceptors
  (fn [db [enabled?]]
    (assoc db ::payment/stripe-card-form-enabled? enabled?)))

(reg-event-db
  ::set-stripe-card-form-error
  payment-interceptors
  (fn [db [error]]
    (assoc db ::payment/stripe-card-form-error error)))

(reg-event-db
  ::set-coupon-code
  payment-interceptors
  (fn [db [coupon]]
    (assoc db ::payment/coupon-code coupon)))

(defquery coupon-check-query
  {:venia/operation {:operation/type :query
                     :operation/name "coupon"}
   :venia/variables [{:variable/name "code" :variable/type :String!}]
   :venia/queries   [[:coupon {:code :$code}
                      [:description :code :duration :discount_amount :discount_percentage]]]})

(reg-event-fx
  ::apply-coupon
  payment-interceptors
  (fn [{db :db} _]
    (let [coupon (::payment/coupon-code db)]
      {:db (-> db
               (assoc ::payment/coupon-loading? true)
               (dissoc ::payment/coupon-error))
       :graphql {:query coupon-check-query
                 :variables {:code coupon}
                 :on-success [::check-coupon-success]
                 :on-failure [::check-coupon-failure]}})))

(reg-event-db
  ::check-coupon-success
  payment-interceptors
  (fn [db [resp]]
    (assoc db
           ::payment/coupon-loading? false
           ::payment/current-coupon (-> resp
                                        (get-in [:data :coupon])
                                        (cases/->kebab-case)
                                        (update :duration keyword)))))

(reg-event-db
  ::check-coupon-failure
  payment-interceptors
  (fn [db [resp]]
    (assoc db
           ::payment/coupon-loading? false
           ::payment/coupon-error "Code not valid!")))

(reg-event-db
  ::reset-coupon-error
  payment-interceptors
  (fn [db _]
    (dissoc db ::payment/coupon-error)))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db _]
    (update db ::payment/sub-db merge payment/default-db)))

(reg-event-db
  ::launch-pad-publish-job-success
  payment-interceptors
  (fn [db [job-id]]
    (assoc-in db [::payment/job-states job-id] :published)))

(reg-event-fx
  ::publish-role
  db/default-interceptors
  (fn [{db :db} [job-id]]
    (let [perms (get-in db [::payment/sub-db ::payment/company :permissions])]
      (process-publish-role-intention
        {:db db
         :job-id job-id
         :permissions perms
         ;;  :pending-offer pending-offer
         :publish-events {:success [::launch-pad-publish-job-success job-id]
                          :failure [::publish-job-failure job-id]
                          :retry   [::publish-role job-id]}
         :on-publish (fn [db]
                       (assoc-in db [::payment/sub-db ::payment/job-states job-id] :loading))}))))

(defmethod on-page-load :payment-setup [db]
  (let [job-id (subs/job-id db)]
    (concat [[::initialize-db]]
            (when (or (not (get-in db [::payment/sub-db ::payment/company :id]))
                      (not= (get-in db [::payment/sub-db ::payment/company :id])
                            (get-in db [::user/sub-db ::user/company :id])))
              [[::fetch-company]])
            (when (and (get-in db [::payment/sub-db ::payment/company :id])
                       (subs/upgrading? db)
                       (= :pay-confirm (subs/payment-step db)))
              [[::estimate-subscription-change]])
            (when (and job-id
                       (not= job-id (get-in db [::payment/sub-db ::payment/job :id])))
              [[::fetch-job]]))))
