(ns wh.jobs.jobsboard.subs
  (:require
    [clojure.string :as str]
    [goog.i18n.NumberFormat :as nf]
    [re-frame.core :refer [reg-sub]]
    [wh.common.data :refer [currency-symbols]]
    [wh.common.emoji :as emoji]
    [wh.components.pagination :as pagination]
    [wh.db :as db]
    [wh.graphql.jobs :as jobs]
    [wh.jobs.jobsboard.db :as jobsboard]
    [wh.user.db :as user]
    [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::job-list-header
  (fn [db _]
    (when (get-in db [::jobsboard/sub-db ::jobsboard/all-jobs?])
      "All Jobs")))

(reg-sub
  ::jobsboard
  (fn [db _]
    (::jobsboard/sub-db db)))

(reg-sub
  ::search-term
  (fn [db _]
    (::db/search-term db)))

(reg-sub
  ::search-term-for-results
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search-term-for-results])))

(reg-sub
  ::search-label
  (fn [db _]
    (::db/search-label db)))

(reg-sub
  ::header-title
  :<- [:wh.subs/vertical]
  :<- [::search-label]
  (fn [[vertical search-label] _]
    (or search-label
        (:title (verticals/config vertical :jobsboard-header)))))

(reg-sub
  ::header-subtitle
  :<- [:wh.subs/vertical]
  (fn [vertical _]
    (or (:subtitle (verticals/config vertical :jobsboard-header))
        "Browse jobs for software engineers and developers")))

(reg-sub
  ::header-description
  :<- [:wh.subs/vertical]
  :<- [::search-term-for-results]
  (fn [[vertical search-term] _]
    (or (get (verticals/config vertical :jobsboard-tag-desc) search-term)
        (:description (verticals/config vertical :jobsboard-header)))))

(reg-sub
  ::filter-shown?
  (fn [db _]
    (::db/filter-shown? db)))


(reg-sub
  ::jobs
  :<- [::jobsboard]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[jobsboard liked-jobs applied-jobs] _]
    (->> (::jobsboard/jobs jobsboard)
         (jobs/add-interactions liked-jobs applied-jobs))))

(reg-sub
  ::promoted-jobs
  :<- [::jobsboard]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[jobsboard liked-jobs applied-jobs] _]
    (->> (::jobsboard/promoted-jobs jobsboard)
         (jobs/add-interactions liked-jobs applied-jobs))))

(reg-sub
  ::search
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search])))

(defn- checkbox-description
  [{:keys [value label cnt display-count?]
    :or {display-count? true}}]
  (let [cnt (or cnt 0)]
    {:value    value
     :label    (str (or label value) (when (and display-count? (pos? cnt)) (str " (" cnt ")")))
     :disabled (zero? cnt)}))

(reg-sub
  :wh.search/searching?
  :<- [::search]
  (fn [search _]
    (:wh.search/searching search)))

(reg-sub
  :wh.search/available-role-types
  :<- [::search]
  (fn [search _]
    (let [role-types (->> search
                          :wh.search/available-role-types
                          (map (juxt :value :count))
                          (into {}))]
      (for [role-type ["Full time" "Contract" "Intern"]]
        (checkbox-description {:value          role-type
                               :cnt            (get role-types role-type)
                               :display-count? false})))))

(reg-sub
  :wh.search/sponsorship-desc
  :<- [::search]
  (fn [search _]
    (checkbox-description {:label          "Sponsorship offered"
                           :cnt            (:wh.search/sponsorship-count search)
                           :display-count? false})))

(reg-sub
  :wh.search/remote-desc
  :<- [::search]
  (fn [search _]
    (checkbox-description {:label          "Remote"
                           :cnt            (:wh.search/remote-count search)
                           :display-count? false})))

(reg-sub
  :wh.search/only-mine-desc
  :<- [::search]
  (fn [search _]
    (checkbox-description {:label "Only my jobs"
                           :cnt   (:wh.search/mine-count search)})))

(reg-sub
  :wh.search/available-tags
  :<- [::search]
  (fn [search _]
    (:wh.search/available-tags search)))

(reg-sub
  :wh.search/role-types
  :<- [::search]
  (fn [search _]
    (:wh.search/role-types search)))

(reg-sub
  :wh.search/query
  :<- [::search]
  (fn [search _]
    (:wh.search/query search)))

(reg-sub
  :wh.search/tag-part
  :<- [::search]
  (fn [search _]
    (:wh.search/tag-part search)))

(reg-sub
  :wh.search/tags
  :<- [::search]
  (fn [search _]
    (:wh.search/tags search)))

(reg-sub
  :wh.search/sponsorship
  :<- [::search]
  (fn [search _]
    (:wh.search/sponsorship search)))

(reg-sub
  :wh.search/remote
  :<- [::search]
  (fn [search _]
    (:wh.search/remote search)))

(reg-sub
  :wh.search/tags-collapsed?
  :<- [::search]
  (fn [search _]
    (:wh.search/tags-collapsed? search)))

(reg-sub
  :wh.search/flagged-tags
  :<- [:wh.search/available-tags]
  :<- [:wh.search/tags]
  (fn [[available-tags tags] _]
    (for [{:keys [value count]} available-tags]
      {:tag value, :selected (contains? tags value), :count count})))

(reg-sub
  :wh.search/matching-tags
  :<- [:wh.search/tag-part]
  :<- [:wh.search/flagged-tags]
  :<- [:wh.search/tags]
  (fn [[substring tags] _]
    (->> tags
         (sort-by (juxt (comp not :selected) (comp - :count))) ;; selected first, then count
         (filter #(or (str/blank? substring)
                      (:selected %)
                      (str/includes? (str/lower-case (:tag %)) (str/lower-case substring))))
         (take 20))))

(reg-sub
  :wh.search/wh-region-count
  :<- [::search]
  (fn [search [_ wh-region]]
    (let [wh-regions (->> search
                          :wh.search/available-wh-regions
                          (map (juxt :value :count))
                          (into {}))]
      (wh-regions wh-region))))

(reg-sub
  :wh.search/city-info
  :<- [::search]
  (fn [search _]
    (:wh.search/city-info search)))

(reg-sub
  :wh.search/countries-by-region
  :<- [:wh.search/city-info]
  (fn [city-info _]
    (into {}
          (map (fn [[k v]] [k (set (map :country-code v))]))
          (group-by :region city-info))))

(reg-sub
  :wh.search/cities-by-country
  :<- [:wh.search/city-info]
  (fn [city-info _]
    (into {}
          (map (fn [[k v]] [k (set (filter seq (map :city v)))]))
          (group-by :country-code city-info))))

(reg-sub
  :wh.search/country-names
  :<- [:wh.search/city-info]
  (fn [city-info _]
    (into {} (map (juxt :country-code :country)) city-info)))

(reg-sub
  :wh.search/country-list
  :<- [::search]
  :<- [:wh.search/countries-by-region]
  :<- [:wh.search/cities-by-country]
  (fn [[{:keys [wh.search/available-countries] :as search} countries-by-region cities-by-country] [_ region]]
    (sort-by :count >
             (for [country available-countries
                   :when (contains? (countries-by-region region) (:value country))]
               (assoc country :cities
                      (let [cities (cities-by-country (:value country))]
                        (sort-by :count >
                                 (for [city (:wh.search/available-cities search)
                                       :when (contains? cities (:value city))]
                                   city))))))))

(reg-sub
  :wh.search/city
  :<- [::search]
  (fn [search [_ city]]
    (contains? (:wh.search/cities search) city)))

(reg-sub
  :wh.search/country
  :<- [::search]
  (fn [search [_ country]]
    (contains? (:wh.search/countries search) country)))

(reg-sub
  :wh.search/wh-region
  :<- [::search]
  (fn [search [_ wh-region]]
    (contains? (:wh.search/wh-regions search) wh-region)))

(reg-sub
  :wh.search/show-search-everywhere?
  :<- [::search]
  (fn [{:keys [wh.search/cities wh.search/countries wh.search/wh-regions] :as search} _]
    (not (or (seq cities) (seq countries) (seq wh-regions)))))

(def region-names {:us "United States", :europe "Europe", :rest-of-world "Rest of the World"})
(def region-flags {:us "US", :europe "EU", :rest-of-world "🌏"})

(defn join-non-nils [sep s]
  (when-let [s' (seq (remove nil? s))]
    (str/join sep s')))

;; FIXME: This is ugly, I know. But it works.

(reg-sub
  :wh.search/collapsed-location-description
  :<- [::search]
  (fn [{:keys [wh.search/cities wh.search/countries wh.search/wh-regions wh.search/city-info] :as search} _]
    (if-not (or (seq wh-regions) (seq cities) (seq countries))
      "Everywhere ✈️"
      (let [region->countries (into {} (map (fn [[k v]] [k (distinct (map :country-code v))])) (group-by :region city-info))
            country->cities (into {} (map (fn [[k v]] [k (filter seq (distinct (map :city v)))])) (group-by :country-code city-info))
            country-name (into {} (map (juxt :country-code :country)) city-info)]
        (->> (keys region->countries)
             (map (fn [region]
                    (let [flag (emoji/country-code->emoji (region-flags region))
                          rname (region-names region)]
                      (if (contains? wh-regions region)
                        (str rname " " flag)
                        (when-let [region-desc (join-non-nils "; "
                                                (for [country (region->countries region)]
                                                  (if (contains? countries country)
                                                    (country-name country)
                                                    (when-let [country-desc (join-non-nils ", "
                                                                             (for [city (country->cities country)
                                                                                   :when (contains? cities city)]
                                                                               city))]
                                                      (str
                                                       (when-not (= country "US")
                                                         (str (country-name country) " – "))
                                                       country-desc)))))]
                          (str rname " " flag " (" region-desc ")"))))))
             (join-non-nils ", "))))))

(reg-sub
  :wh.search/result-count
  (fn [db _]
    (or (get-in db [::jobsboard/sub-db ::jobsboard/number-of-search-results]) 0)))


(reg-sub
  ::result-count-str
  :<- [:wh.search/result-count]
  (fn [n _]
    (if (zero? n)
      "We found no jobs matching your criteria 😢"
      (<< "We found ~{n} jobs matching your criteria"))))

(reg-sub
  ::pre-set-search-result-count-str
  :<- [:wh.search/result-count]
  :<- [::header-title]
  (fn [[n title] _]
    (if (zero? n)
      (<< "We found no jobs matching '~{title}' 😢")
      (<< "We found ~{n} jobs matching '~{title}'"))))

(reg-sub
  ::current-page
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/current-page])))

(reg-sub
  ::total-pages
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/total-pages])))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))

(reg-sub
  ::pagination-query-params
  :<- [:wh.subs/query-params]
  :<- [::search-term]
  (fn [[query-params search-term] _]
    (if (str/blank? search-term)
      query-params
      (assoc query-params "search" search-term))))

(reg-sub
  :wh.subs/search-navbar?
  (fn [db _]
    (= (::db/page db) :jobsboard)))

(reg-sub
  :wh.search/currency
  :<- [::search]
  (fn [search _]
    (:wh.search/currency search)))

(reg-sub
  :wh.search/currencies
  :<- [::search]
  (fn [search _]
    (conj (distinct (map :currency (:wh.search/salary-ranges search)))
          "*")))

(reg-sub
  :wh.search/salary-type
  :<- [::search]
  (fn [search _]
    (:wh.search/salary-type search)))

;; This can return nil when salary ranges are not
;; yet fetched. In this case, the slider is not
;; rendered at all.

(reg-sub
  :wh.search/grouped-salary-ranges
  :<- [::search]
  (fn [search _]
    (when-let [ranges (:wh.search/salary-ranges search)]
      (group-by (juxt :currency :time-period) ranges))))

(reg-sub
  :wh.search/salary-min-max
  :<- [:wh.search/grouped-salary-ranges]
  :<- [:wh.search/currency]
  :<- [:wh.search/salary-type]
  (fn [[ranges currency type] _]
    (let [type ({:year "Yearly", :day "Daily"}
                (or type :year))]
      (first (get ranges [currency type])))))

(reg-sub
  :wh.search/salary-min
  :<- [:wh.search/salary-min-max]
  (fn [minmax _]
    (or (:min minmax) 0)))

(reg-sub
  :wh.search/salary-max
  :<- [:wh.search/salary-min-max]
  (fn [minmax _]
    (or (:max minmax) 100000)))

(reg-sub
  :wh.search/salary-range
  :<- [::search]
  :<- [:wh.search/salary-min]
  :<- [:wh.search/salary-max]
  (fn [[search min max] _]
    (or (:wh.search/salary-range search) [min max])))

(reg-sub
  :wh.search/salary-range-js
  :<- [:wh.search/salary-range]
  (fn [range _]
    (clj->js range)))

(let [formatter (goog.i18n.NumberFormat. nf/Format.COMPACT_SHORT)]
  (defn format-number [n]
    (.format formatter n)))

(reg-sub
  :wh.search/salary-range-desc
  :<- [:wh.search/salary-range]
  :<- [:wh.search/currency]
  (fn [[search currency] _]
    (if-not search
      "Unspecified"
      (let [[min max] search
            symbol (currency-symbols currency)]
        (str symbol (format-number min) " – " symbol (format-number max))))))

(reg-sub
  :wh.search/only-mine
  :<- [::search]
  (fn [search _]
    (:wh.search/only-mine search)))

(reg-sub
  :wh.search/published
  :<- [::search]
  (fn [search _]
    (:wh.search/published search)))

(defn- published-option
  [{:keys [wh.search/published-count]} value label]
  (let [cnt (->> published-count
                 (filter #(= (:value %) (str value)))
                 first :count)]
    (checkbox-description {:value value :label label :cnt cnt})))

(reg-sub
  :wh.search/published-options
  :<- [::search]
  (fn [search _]
    [(published-option search true "Published")
     (published-option search false "Unpublished")]))
