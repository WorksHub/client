(ns wh.company.create-job.db
  (:require [cljs.spec.alpha :as s]
            [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.db :refer [app-db]]
            [wh.common.data :as data]
            [wh.common.specs.company :as company-spec]
            [wh.common.specs.location]
            [wh.common.specs.primitives :as p]
            [wh.components.forms.db :as forms]
            [wh.components.tag :as tag]
            [wh.db :as db]
            [wh.graphql-cache :as cache]
            [wh.user.db :as user]
            [wh.verticals :as verticals]))

(def tagline-max-length 110)

(def fields
  {::title               {:order 0 :initial "", :validate ::p/non-empty-string}
   ::company__name       {:order 1 :initial ""}
   ::company-id          {:order 2 :initial nil, :validate ::company-id}
   ::tagline             {:order 3 :initial "", :validate ::p/non-empty-string :event? false}
   ::role-type           {:order 4 :initial "Full time"}
   ::sponsorship-offered {:order 5 :initial false}

   ::remuneration__currency    {:order 10 :initial nil :validate ::currency :event? false}
   ::remuneration__min         {:order 11 :initial nil :validate ::min}
   ::remuneration__max         {:order 12 :initial nil :validate ::max}
   ::remuneration__time-period {:order 13 :initial (first data/time-periods) :event? false}
   ::remuneration__equity      {:order 14 :initial false}
   ::remuneration__competitive {:order 15 :initial false :event? false}

   ::tags {:order 20 :initial #{}, :validate ::selected-tags}

   ::remote                 {:order 30 :initial false :event? false}
   ::location__street       {:order 31 :initial "" :validate (s/nilable :wh.location/street)}
   ::location__city         {:order 32 :initial "" :validate ::city :event? false}
   ::location__post-code    {:order 33 :initial "" :validate (s/nilable :wh.location/post-code)}
   ::location__state        {:order 34 :initial "" :validate (s/nilable :wh.location/state)}
   ::location__country      {:order 35 :initial "" :validate :wh.location/country :event? false}
   ::location__country-code {:order 36 :initial "" :validate (s/nilable :wh.location/country-code)}
   ::location__latitude     {:order 37 :initial nil :validate (s/nilable :wh.location/latitude)}
   ::location__longitude    {:order 38 :initial nil :validate (s/nilable :wh.location/longitude)}

   ::description-html  {:order 40 :initial "", :validate ::p/non-empty-string}

   ::ats-job-id         {:order 50 :initial "" :event? false}

   ::manager            {:order 60 :initial "", :validate ::manager}
   ::promoted           {:order 61 :initial false}
   ::published          {:order 62 :initial false}
   ::approved           {:order 63 :initial false}

   ::verticals (s/coll-of ::p/non-empty-string :distinct true :min-count 1)})

(defn initial-db [db editing?]
  (let [clean-form (merge (forms/initial-value fields)
                          {::verticals (if (not= "www" (::db/vertical db))
                                         #{(::db/vertical db)}
                                         #{verticals/default-vertical})}
                          (when (= "remote" (::db/vertical db))
                            {::remote true}))]
    (merge (when editing? ;; if editing, we want expect values to be overwritten
             clean-form)
           (::sub-db db)
           {::tag-search ""
            ::tags-collapsed? true
            ::benefits-search ""
            ::benefits-collapsed? true
            ::editing-address? false
            ::company-loading? false
            ::pending-logo nil
            ::pending-company-description nil
            ::logo-uploading? false
            ::search-address ""
            ::form-errors nil}
           (when-let [company (get-in db [::user/sub-db ::user/company])]
             {::company-id (:id company)
              ::company-package (keyword (:package company))})
           (when-not editing? ;; if not editing, we want to reset any previous values
             clean-form))))

(defn relevant-fields
  [db]
  (letfn [(remove-admin-fields [f]
            (if-not (user/admin? db)
              (disj f ::manager
                    ::promoted
                    ::published)
              f))
          (remove-currency-fields [f]
            (if (get-in db [::sub-db ::remuneration__competitive])
              (disj f ::remuneration__currency
                    ::remuneration__min
                    ::remuneration__max)
              f))]
    (-> (keys fields)
        (set)
        (remove-admin-fields)
        (remove-currency-fields))))

(defn invalid-fields
  [db]
  (let [sub-db (::sub-db db)]
    (when-let [keys (forms/invalid-fields (select-keys fields (relevant-fields db)) sub-db)]
      (->> keys
           (map #(vector % (get-in fields [% :order])))
           (sort-by second)
           (map first)))))

(def location-fields
  (reduce (fn [a k] (if (str/starts-with? (name k) "location__") (conj a k) a) ) [] (keys fields)))

(defn edit? [db]
  (= (:wh.db/page db) :edit-job))

(s/def ::search-address string?)
(s/def ::location-search-error (s/nilable string?))

(def role-types ["Full time" "Contract" "Intern"])

(s/def ::role-type (set role-types))

(s/def ::default-cities
  (set data/cities))

(s/def ::city-suggestions ::default-cities)

(s/def ::city #(if (get-in @app-db [::sub-db ::remote])
                 (s/valid? (s/or :city ::p/non-empty-string
                                 :empty (s/and string? str/blank?)
                                 :nil nil?) %)
                 (s/valid? ::p/non-empty-string %)))

(s/def ::location-form-open? boolean?)
(s/def ::search-location-form-open? boolean?)

(s/def ::competitive boolean?)

(s/def ::default-currencies
  (set data/currencies))

(s/def ::currency ::default-currencies)

(s/def ::time-period (set data/time-periods))
(s/def ::min (s/nilable nat-int?))
(s/def ::max nat-int?)
(s/def ::equity boolean?)

(s/def ::tag ::p/non-empty-string)
(s/def ::tag-search string?)
(s/def ::selected? boolean?)
(s/def ::tag-container (s/keys :req-un [::tag]
                               :opt-un [::selected]))
(s/def ::available-tags (s/coll-of ::tag-container :distinct true))
(s/def ::selected-tags (s/coll-of ::tag-container :distinct true :kind set? :min-count 1))

(s/def ::company-id ::p/non-empty-string)
(s/def ::manager ::data/manager)


(s/def :wh.company.profile/benefit-tags (s/and :wh/tags company-spec/includes-benefit?))
(s/def ::company
  (s/keys :req-un [:wh.company.profile/logo
                   :wh.company.profile/description-html
                   :wh.company.profile/benefit-tags]))

(def company-required-fields
  [:descriptionHtml :logo
   [:tags [:slug :label :type :subtype :id :weight]]])

(defn db->company
  [db]
  (let [selected-tag-ids (::selected-benefit-tag-ids (::sub-db db))]
    {:logo (or (::pending-logo (::sub-db db))
               (:logo (::company (::sub-db db))))
     :description-html (or (::pending-company-description (::sub-db db))
                           (:description-html (::company (::sub-db db))))
     :tags (distinct
             (concat
               (->> (cache/result db :tags {:type :benefit})
                    :list-tags
                    :tags
                    (filter #(contains? selected-tag-ids (:id %)))
                    (map tag/->tag))
               (->> (:tags (::company (::sub-db db)))
                    (remove #(= :benefit (:type %))))))}))

(defn db->gql-company
  [db]
  (let [selected-tag-ids (::selected-benefit-tag-ids (::sub-db db))]
    {:id (::company-id (::sub-db db))
     :logo (or (::pending-logo (::sub-db db))
               (:logo (::company (::sub-db db))))
     :descriptionHtml (or (::pending-company-description (::sub-db db))
                          (:description-html (::company (::sub-db db))))
     :tagIds (distinct
               (concat
                 selected-tag-ids
                 (->> (:tags (::company (::sub-db db)))
                      (remove #(= :benefit (:type %)))
                      (map :id))))}))

(defn invalid-company-fields
  [db]
  (some->> (set/rename-keys
             (db->company db) {:tags :benefit-tags})
           (s/explain-data ::company)
           ::s/problems
           (map (comp last :path))
           (map (fn [t] (keyword "wh.company.profile" (name t))))
           (set)))
