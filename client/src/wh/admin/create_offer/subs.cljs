(ns wh.admin.create-offer.subs
  (:require
    [cljs.spec.alpha :as s]
    [clojure.set :refer [rename-keys]]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [reg-sub]]
    [wh.admin.create-offer.db :as create-offer]
    [wh.common.data :as data]
    [wh.common.specs.primitives :as p]
    [wh.subs :refer [error-sub-key]]))

(reg-sub ::sub-db (fn [db _] (::create-offer/sub-db db)))

(doseq [field (keys create-offer/fields)
        :let [sub (keyword "wh.admin.create-offer.subs" (name field))
              db-field (keyword "wh.admin.create-offer.db" (name field))]]
  (reg-sub sub :<- [::sub-db] db-field))

(def error-msgs {::p/non-empty-string "This field can't be empty."
                 :wh.offer/recurring-fee "Fee cannot be empty"
                 :wh.offer/placement-percentage "Percentage must be between 0 and 100 inclusive"})

(defn error-query
  [db k spec]
  (when spec
    (let [value (get db k)]
      (and (not (s/valid? spec value))
           (get error-msgs spec (str "Failed to validate: " spec))))))

(doseq [[k {spec :validate}] create-offer/fields]
  (reg-sub
   (error-sub-key k)
   :<- [::sub-db]
   :<- [::form-errors]
   (fn [[db form-errors] _]
     {:message (error-query db k spec)
      :show-error? (contains? form-errors k)})))

(reg-sub
 ::form-errors
 :<- [::sub-db]
 (fn [db _]
   (::create-offer/form-errors db)))

(reg-sub
  ::company
  :<- [::sub-db]
  (fn [sub-db _]
    (::create-offer/company sub-db)))

(reg-sub
  ::package
  :<- [::company]
  (fn [company _]
    (:package company)))

(reg-sub
  ::billing-period
  :<- [::company]
  (fn [company _]
    (get-in company [:payment :billing-period] data/default-billing-period)))

(reg-sub
  ::existing-offer
  :<- [::company]
  (fn [company _]
    (:offer company)))

(reg-sub
  ::pending-offer
  :<- [::company]
  (fn [company _]
    (:pending-offer company)))

(reg-sub
  ::creating?
  :<- [::sub-db]
  (fn [sub-db _]
    (::create-offer/creating? sub-db)))

(reg-sub
 ::success?
 :<- [::sub-db]
 (fn [sub-db _]
   (::create-offer/success? sub-db)))

(reg-sub
 ::show-custom-offer?
 :<- [::offer]
 (fn [offer _]
   (= :custom offer)))

(reg-sub
 ::offer-already-accepted?
 :<- [::existing-offer]
 (fn [{:keys [accepted-at]} _]
   (boolean accepted-at)))

(reg-sub
 ::offer-suggestions
 :<- [:wh.user/super-admin?]
 (fn [super-admin? _]
   (let [labels (map (fn [[id {:keys [fixed percentage]}]]
                       {:id id :label (gstring/format "$%s recurring + %s%% on placement" fixed percentage)})
                     data/take-off-offers)]
     (concat [{:label "Select an offer..."}]
             labels
             (when super-admin?
               [{:label "Custom offer" :id :custom}])))))
