(ns wh.admin.create-offer.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.admin.create-offer.db :as create-offer]
            [wh.common.cases :as cases]
            [wh.common.data :as data]
            [wh.db :as db]
            [wh.pages.core :as pages :refer [on-page-load]]))

(def create-offer-interceptors (into db/default-interceptors
                                     [(path ::create-offer/sub-db)]))

(doseq [[field {:keys [event?] :or {event? true}}] create-offer/fields
        :when event?
        :let [event-name (keyword "wh.admin.create-offer.events" (str "edit-" (name field)))
              db-field (keyword "wh.admin.create-offer.db" (name field))]]
  (reg-event-db event-name
                create-offer-interceptors
                (fn [db [new-value]]
                  (assoc db db-field new-value))))

(defn company-id
  [db]
  (get-in db [::db/page-params :id]))


(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    (let [new-db (create-offer/initial-db db)]
      {:db (assoc db ::create-offer/sub-db new-db)})))

(reg-event-fx
  ::fetch-company
  db/default-interceptors
  (fn [{db :db} _]
    (let [id (company-id db)]
      {:graphql {:query      {:venia/queries
                              [[:company {:id id}
                                [:id :name :logo :manager :vertical :package
                                 [:payment [:billingPeriod]]
                                 [:offer [:recurringFee :placementPercentage :acceptedAt]]
                                 [:pendingOffer [:recurringFee :placementPercentage]]]]]}
                 :on-success [::fetch-company-success]
                 :on-failure [::fetch-company-failure]}})))

(reg-event-fx
  ::fetch-company-success
  create-offer-interceptors
  (fn [{db :db} [resp]]
    {:db (->> (get-in resp [:data :company])
              (cases/->kebab-case)
              (assoc db ::create-offer/company))}))

(reg-event-fx
  ::fetch-company-failure
  create-offer-interceptors
  (fn [{db :db} _]
    {:dispatch [:error/set-global "Company could not be loaded."]}))

(reg-event-db
  ::select-offer
  create-offer-interceptors
  (fn [db [offer]]
    (let [{:keys [fixed percentage]} (get data/take-off-offers offer)]
      (assoc db
             ::create-offer/offer offer
             ::create-offer/offer-fixed fixed
             ::create-offer/offer-percentage percentage))))

(reg-event-fx
  ::create-offer
  db/default-interceptors
  (fn [{db :db} _]
    {:db      (-> db
                  (assoc-in [::create-offer/sub-db ::create-offer/creating?] true)
                  (assoc-in [::create-offer/sub-db ::create-offer/company :pending-offer] nil))
     :graphql {:query      {:venia/operation {:operation/type :mutation
                                              :operation/name "update_company"}
                            :venia/variables [{:variable/name "update_company"
                                               :variable/type :UpdateCompanyInput!}]
                            :venia/queries   [[:update_company {:update_company :$update_company}
                                               [:id]]]}
               :variables  {:update_company
                            {:id           (company-id db)
                             :pendingOffer {:recurringFee        (get-in db [::create-offer/sub-db
                                                                             ::create-offer/offer-fixed])
                                            :placementPercentage (get-in db [::create-offer/sub-db
                                                                             ::create-offer/offer-percentage])}}}
               :on-success [::create-offer-success]
               :on-failure [::create-offer-failure]}}))

(reg-event-fx
  ::create-offer-success
  create-offer-interceptors
  (fn [{db :db} [resp]]
    (let [company (get-in resp [:data :company])]
      {:db (assoc db
                  ::create-offer/success? true
                  ::create-offer/creating? false)})))

(reg-event-fx
  ::create-offer-failure
  create-offer-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::create-offer/creating? false)
     :dispatch [:error/set-global "There was an error whilst creating the offer."]}))

(defmethod on-page-load :create-company-offer [db]
  [[::initialize-db]
   [::fetch-company]])
