(ns wh.admin.create-candidate.subs
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.admin.create-candidate.db :as sub-db]
            [wh.common.errors :refer [upsert-user-error-message]]
            [wh.common.specs.location]
            [wh.common.specs.primitives :as p]
            [wh.common.url :as url]
            [wh.components.tag :as tag]
            [wh.re-frame.subs :refer [<sub]]
            [wh.subs :refer [error-sub-key]]))

(reg-sub ::sub-db (fn [db _] (::sub-db/sub-db db)))

(reg-sub
  ::form-errors
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/form-errors db)))

(def error-msgs {::p/non-empty-string "This field can't be empty."
                 ::p/email            "This is not a valid email."
                 ::sub-db/tags        "You need to pick some tags."})

(defn error-query
  [db k spec]
  (when-let [err (sub-db/field-has-error? db k)]
    (if (string? err)
      err
      (get error-msgs spec (str "Failed to validate: " spec)))))

(doseq [field (keys sub-db/fields)
        :let  [sub (keyword "wh.admin.create-candidate.subs" (name field))
               db-field (keyword "wh.admin.create-candidate.db" (name field))]
        :when (not (false? (:sub? (sub-db/fields field))))]
  (reg-sub sub :<- [::sub-db]
           (fn [db _]
             (get db db-field))))

(reg-sub
  ::other-links
  :<- [::sub-db]
  (fn [db [_ i]]
    (nth (::sub-db/other-links db) i)))

(reg-sub
  ::num-other-links
  :<- [::sub-db]
  (fn [db [_ _i]]
    (count (::sub-db/other-links db))))

(reg-sub
  ::other-links-title
  :<- [::sub-db]
  (fn [db [_ i]]
    (let [s (nth (::sub-db/other-links db) i)]
      (if (str/blank? s)
        "Candidate's website or profile"
        (case (:type (url/detect-page-type s))
          :github        "Candidate's GitHub profile"
          :twitter       "Candidate's Twitter profile"
          :facebook      "Candidate's Facebook profile"
          :linkedin      "Candidate's LinkedIn profile"
          :stackoverflow "Candidate's Stack Overflow profile"
          "Candidate's website")))))

(doseq [[k {spec :validate}] sub-db/fields]
  (reg-sub
    (error-sub-key k)
    :<- [::sub-db]
    :<- [::form-errors]
    (fn [[db form-errors] _]
      {:message     (error-query db k spec)
       :show-error? (contains? form-errors k)})))

(reg-sub
  ::location-suggestions
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/location-suggestions db)))

(reg-sub
  ::company-suggestions
  :<- [::sub-db]
  (fn [db _]
    (when-let [name (::sub-db/current-company-search db)]
      (let [current-company (::sub-db/current-company db)
            companies       (::sub-db/companies db)]
        (when (and (nil? current-company) (seq name))
          (->> companies
               (mapv #(rename-keys % {:name :label}))
               (sort-by :label)))))))

(reg-sub
  ::cv-uploading?
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/cv-uploading? db)))

(reg-sub
  ::cv-url
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/cv-url db)))

(reg-sub
  ::cv-filename
  :<- [::sub-db]
  (fn [db _]
    (::sub-db/cv-filename db)))

(reg-sub
  ::available-tags
  :<- [:graphql/result :tags {:type :tech}]
  (fn [tags _]
    (map tag/->tag
         (get-in tags [:list-tags :tags]))))

(reg-sub
  ::has-location?
  :<- [::sub-db]
  (fn [db _]
    (not (str/blank? (::sub-db/location__country db)))))

(defn match-tag [search selected-tag-ids]
  (fn [tag]
    (and (or (str/blank? search)
             (str/includes? (str/lower-case (:label tag)) search))
         (not (contains? selected-tag-ids (:id tag))))))

;; TODO: refactor, and normalize with other tag selectors [ch4750]
(defn take-tech-tags
  [selected-tags all-tags tag-search]
  (let [selected-tag-ids          (set (map :id selected-tags))
        matching-but-not-selected (filter
                                    (match-tag tag-search selected-tag-ids) all-tags)
        selected-tags             (->> all-tags
                                       (filter
                                         (fn [{id :id}] (contains? selected-tag-ids id)))
                                       (map #(assoc % :selected true)))]

    (->> matching-but-not-selected
         (concat selected-tags)
         (map tag/tag->form-tag)
         (take (+ 20 (count selected-tags))))))

(reg-sub
  ::matching-tech-tags
  :<- [::tech-tag-search]
  :<- [::available-tags]
  :<- [::tech-tags]
  (fn [[tag-search all-tags selected-tags] _]
    (take-tech-tags selected-tags all-tags tag-search)))

(def all-company-tags
  (->> ["Series A" "Startup" "Flexible working"
        "Remote working" "Contract work" "Corporate"
        "Offer sponsorship"]
       (map #(hash-map :tag %))))

;; TODO: refactor, and normalize with other tag selectors [ch4750]
(defn take-company-tags
  [num selected-tags all-tags search]
  (let [search            (some-> search (str/lower-case))
        selected-tags-set (set (map :tag selected-tags))]
    (->> all-tags
         (filter (fn [{:keys [tag]}]
                   (and (or (str/blank? search)
                            (str/includes? tag search))
                        (not (contains? selected-tags-set tag)))))
         (concat selected-tags)
         (take num))))

(reg-sub
  ::matching-company-tags
  :<- [::company-tag-search]
  :<- [::company-tags]
  (fn [[tag-search selected-tags] _]
    (take-company-tags 20 selected-tags all-company-tags tag-search)))

(reg-sub
  ::error
  :<- [::sub-db]
  (fn [db _]
    (when-let [error (::sub-db/error db)]
      (upsert-user-error-message error))))
