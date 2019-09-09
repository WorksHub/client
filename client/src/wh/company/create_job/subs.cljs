(ns wh.company.create-job.subs
  (:require [cljs.spec.alpha :as s]
            [clojure.set :refer [rename-keys]]
            [clojure.set :as set]
            [clojure.string :as str]
            [goog.i18n.NumberFormat :as nf]
            [goog.string :as gstring]
            [goog.string.format]
            [re-frame.core :refer [reg-sub reg-sub-raw]]
            [wh.common.data :as data :refer [currency-symbols managers get-manager-email salary-ranges]]
            [wh.common.errors :as errors]
            [wh.common.specs.primitives :as p]
            [wh.company.create-job.db :as create-job]
            [wh.components.tag :refer [->tag tag->form-tag]]
            [wh.db :as db]
            [wh.re-frame.subs :refer [<sub]]
            [wh.subs :refer [with-unspecified-option error-sub-key]]
            [wh.user.subs :as user])
  (:require-macros
    [clojure.core.strint :refer [<<]]
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub ::sub-db (fn [db _] (::create-job/sub-db db)))

(doseq [field (keys create-job/fields)
        :let [sub (keyword "wh.company.create-job.subs" (name field))
              db-field (keyword "wh.company.create-job.db" (name field))]]
  (reg-sub sub :<- [::sub-db] db-field))

(def error-msgs {::p/non-empty-string "This field can't be empty."
                 ::create-job/country "This field can't be empty."
                 ::create-job/city "This must be a valid city."
                 ::p/email "This is not a valid email."
                 ::create-job/selected-tags "One or more skills required."
                 ::create-job/currency "Invalid currency"
                 ::create-job/company-id "Select a valid company from the list."
                 ::create-job/manager "Please select a valid Manager."
                 :wh.location/country "Please enter a country."})

(defn error-query
  [db k spec]
  (when spec
    (let [value (get db k)]
      (and (not (s/valid? spec value))
           (get error-msgs spec (str "Failed to validate: " spec))))))

(doseq [[k {spec :validate}] create-job/fields]
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
    (::create-job/form-errors db)))

(reg-sub
  ::company-suggestions
  :<- [::sub-db]
  :<- [::company__name]
  (fn [[db company-name] _]
    (when company-name
      (let [id (::create-job/company-id db)
            companies (::create-job/companies db)]
        (when (and (nil? id) (seq company-name))
          (->> companies
               (mapv #(rename-keys % {:name :label}))
               (sort-by :label)))))))

(reg-sub
  ::edit?
  (fn [db _]
    (create-job/edit? db)))

(reg-sub
  ::page-title
  :<- [::edit?]
  :<- [::title]
  :<- [::company__name]
  (fn [[edit? title company-name] _]
    (if edit?
      (<< "Editing job: ~{title} @ ~{company-name}")
      "Create a new role")))

(reg-sub
  ::logo-uploading?
  :<- [::sub-db]
  (fn [db _]
    (::create-job/logo-uploading? db)))

(reg-sub
  ::company-loading?
  :<- [::sub-db]
  (fn [db _]
    (::create-job/company-loading? db)))

(reg-sub
  ::saving?
  :<- [::sub-db]
  (fn [db _]
    (::create-job/saving? db)))

(reg-sub
  ::location-suggestions
  :<- [::sub-db]
  (fn [db _]
    (::create-job/location-suggestions db)))

(reg-sub
  ::search-address
  :<- [::sub-db]
  (fn [db _]
    (::create-job/search-address db)))

(reg-sub
  ::search-location-form-open?
  :<- [::sub-db]
  (fn [db _]
    (or (::create-job/search-location-form-open? db)
        (::create-job/editing-address? db)
        (not (every? str/blank? (vals (select-keys db create-job/location-fields)))))))

(reg-sub
  ::city-suggestions
  :<- [::sub-db]
  (fn [db _]
    (::create-job/city-suggestions db)))

(reg-sub
  ::country-suggestions
  :<- [::sub-db]
  (fn [db _]
    (::create-job/country-suggestions db)))

(reg-sub
  ::currencies
  (with-unspecified-option data/currencies "--"))

(reg-sub
  ::time-periods
  (constantly data/time-periods))

(reg-sub
  ::show-salary-details?
  :<- [::sub-db]
  (fn [db _]
    (not (get db ::create-job/remuneration__competitive))))

(reg-sub
  ::package
  :<- [::sub-db]
  (fn [db _]
    (keyword (::create-job/company-package db))))

(reg-sub
  ::salary-min
  :<- [::remuneration__currency]
  :<- [::remuneration__time-period]
  (fn [[currency tp] _]
    (data/get-min-salary currency tp)))

(reg-sub
  ::salary-max
  :<- [::remuneration__currency]
  :<- [::remuneration__time-period]
  (fn [[currency tp] _]
    (data/get-max-salary currency tp)))

(reg-sub
  ::salary-range
  :<-  [::remuneration__min]
  :<- [::remuneration__max]
  :<- [::salary-min]
  :<- [::salary-max]
  (fn [[smin smax min max] _]
    [(or smin min) (or smax max)]))

(reg-sub
  ::salary-range-js
  :<- [::salary-range]
  (fn [range _]
    (clj->js range)))

(let [formatter (goog.i18n.NumberFormat. nf/Format.COMPACT_SHORT)]
  (defn format-number [n]
    (.format formatter n)))

(reg-sub
  ::salary-label
  :<- [::salary-range]
  :<- [::remuneration__currency]
  (fn [[[min max] currency] _]
    (let [symbol (currency-symbols currency)]
      (str symbol  (format-number min) " – " symbol (format-number max)))))

(defn take-tags
  [num selected-tags all-tags search]
  (let [search (some-> search (str/lower-case))
        selected-tags-set (set (map :tag selected-tags))]
    (->> all-tags
         (filter (fn [{:keys [tag]}]
                   (and (or (str/blank? search)
                            (str/includes? tag search))
                        (not (contains? selected-tags-set tag)))))
         (concat selected-tags)
         (take num))))

;; Tags

(reg-sub
  ::tag-search
  :<- [::sub-db]
  (fn [db _]
    (::create-job/tag-search db)))

(reg-sub
  ::available-tags
  :<- [::sub-db]
  (fn [db _]
    (::create-job/available-tags db)))

(reg-sub
  ::tags-collapsed?
  :<- [::sub-db]
  (fn [db _]
    (::create-job/tags-collapsed? db)))

(reg-sub
  ::matching-tags
  :<- [::tag-search]
  :<- [::available-tags]
  :<- [::tags]
  (fn [[tag-search all-tags selected-tags] _]
    (take-tags 20 selected-tags all-tags tag-search)))

(reg-sub
  ::editing-address?
  :<- [::sub-db]
  (fn [db _]
    (::create-job/editing-address? db)))

(reg-sub
  ::condensed-address
  :<- [::location__street]
  :<- [::location__city]
  :<- [::location__state]
  :<- [::location__country]
  :<- [::location__post-code]
  (fn [address-parts _]
    (vec (remove str/blank? address-parts))))

(reg-sub
  ::manager-suggestions
  :<- [::manager]
  (fn [m _]
    (let [m (when m (str/lower-case m))
          results (->> managers
                       (filter (fn [[email manager]] (str/includes? (str/lower-case manager) m)))
                       (map (fn [[email manager]] (hash-map :id manager :label (gstring/format "%s (%s)" manager email))))
                       (take 5)
                       (vec))]
      (if (and (= 1 (count results))
               (= m (some-> results (first) (:label) (str/lower-case))))
        (vector)
        results))))

(defn get-server-error-string
  [k]
  (case k
    :unknown-error "An unknown error or timeout has occurred. Please check your connection and try again."
    (errors/upsert-user-error-message k)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [db _]
    (some-> (::create-job/error db)
            (get-server-error-string))))

(reg-sub
  ::greenhouse-integration?
  :<- [::sub-db]
  (fn [db _]
    (get-in db [::create-job/company__integrations :greenhouse :enabled])))

(reg-sub
  ::greenhouse-jobs
  :<- [::sub-db]
  (fn [db _]
    (->> (get-in db [::create-job/company__integrations :greenhouse :jobs])
         (mapv (fn [{:keys [id name] :as job}]
                 (assoc job :label (str name " (" id ")"))))
         (filter #(str/includes?
                    (str/lower-case (:label %))
                    (str/lower-case (or (::create-job/ats-job-id db) "")))))))

(reg-sub
  ::show-integrations?
  :<- [::greenhouse-integration?]
  (fn [integration _]
    integration))

(reg-sub
  ::logo
  :<- [::sub-db]
  (fn [sub-db _]
    (:logo (::create-job/company sub-db))))

(reg-sub
  ::company-description
  :<- [::sub-db]
  (fn [sub-db _]
    (:description-html (::create-job/company sub-db))))

(reg-sub
  ::pending-company-description
  :<- [::sub-db]
  (fn [sub-db _]
    (::create-job/pending-company-description sub-db)))

(reg-sub
  ::pending-logo
  :<- [::sub-db]
  (fn [sub-db _]
    (::create-job/pending-logo sub-db)))

(defn company-field->spec
  [field]
  (case field
    :logo             :wh.company.profile/logo
    :description-html :wh.company.profile/description-html
    :tags             :wh.company.profile/benefit-tags))

(reg-sub
  ::validate-existing-company-field
  :<- [::sub-db]
  (fn [sub-db [_ field]]
    (let [value (get-in sub-db [::create-job/company field])]
      (s/valid? (company-field->spec field) value))))

(reg-sub
  ::validate-existing-company-fields
  :<- [::sub-db]
  (fn [sub-db _]
    (s/valid? ::create-job/company (set/rename-keys
                                     (::create-job/company sub-db)
                                     {:tags :benefit-tags}))))

(reg-sub
  :wh.company.profile/logo-error
  :<- [::pending-logo]
  :<- [::form-errors]
  (fn [[logo form-errors] _]
    {:message (when-not (s/valid? :wh.company.profile/logo logo)
                "Please upload a logo for your company.")
     :show-error? (contains? form-errors :wh.company.profile/logo)}))

(reg-sub
  :wh.company.profile/description-html-error
  :<- [::pending-company-description]
  :<- [::form-errors]
  (fn [[desc form-errors] _]
    {:message (when-not (s/valid? :wh.company.profile/description-html desc)
                "Please provide a brief description for your company.")
     :show-error? (contains? form-errors :wh.company.profile/description-html)}))

(reg-sub
  :wh.company.profile/benefit-tags-error
  :<- [::selected-benefit-tag-ids]
  :<- [::form-errors]
  (fn [[tag-ids form-errors] _]
    {:message (when (empty? tag-ids)
                "Please provide at least one benefit tag.")
     :show-error? (contains? form-errors :wh.company.profile/benefit-tags)}))

(reg-sub
  ::benefits-search
  :<- [::sub-db]
  (fn [db _]
    (::create-job/benefits-search db)))

(reg-sub
  ::selected-benefit-tag-ids
  :<- [::sub-db]
  (fn [db _]
    (::create-job/selected-benefit-tag-ids db)))

(reg-sub-raw
  ::benefit-tags
  (fn [_ _]
    (->> (get-in (<sub [:graphql/result :tags {:type :benefit}]) [:list-tags :tags])
         (map ->tag)
         (reaction))))

(reg-sub
  ::matching-benefit-tags
  :<- [::benefit-tags]
  :<- [::benefits-search]
  (fn [[tags tag-search] [_ {:keys [include-ids size]}]]
    (let [tag-search (str/lower-case tag-search)
          matching-but-not-included (filter (fn [tag] (and (or (str/blank? tag-search)
                                                               (str/includes? (str/lower-case (:label tag)) tag-search))
                                                           (not (contains? include-ids (:id tag))))) tags)
          included-tags (filter (fn [tag] (contains? include-ids (:id tag))) tags)]
      (->> (concat included-tags matching-but-not-included)
           (map tag->form-tag)
           (take (+ (or size 20) (count include-ids)))))))

(reg-sub
  ::company-slug
  :<- [::sub-db]
  (fn [db _]
    (::create-job/company-slug db)))
