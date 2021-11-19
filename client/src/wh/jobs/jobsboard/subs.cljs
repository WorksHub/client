(ns wh.jobs.jobsboard.subs
  (:require [clojure.string :as str]
            [goog.i18n.NumberFormat :as nf]
            [re-frame.core :refer [reg-sub]]
            [wh.common.data :refer [currency-symbols]]
            [wh.common.job :as job]
            [wh.common.user :as user-common]
            [wh.components.pagination :as pagination]
            [wh.db :as db]
            [wh.graphql-cache :as gqlc]
            [wh.job.db :as job-db]
            [wh.jobs.jobsboard.db :as jobsboard]
            [wh.jobs.jobsboard.events :as events]
            [wh.jobs.jobsboard.search-results :as search-results]
            [wh.jobsboard.db :as jobsboard-ssr]
            [wh.landing-new.events :as landing-events]
            [wh.slug :as slug]
            [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::search-params
  :<- [::results]
  (fn [{:keys [jobs-search] :as _results} _]
    (:search-params jobs-search)))

(reg-sub
  ::ssr-jobs
  (fn [db _]
    (get-in db jobsboard/ssr-jobs-path)))

(reg-sub
  ::db
  (fn [db _] db))

;; used e.g. on preset search pages
(reg-sub
  ::search-tag
  (fn [db _]
    (get-in db [:wh.db/page-params :tag])))

(reg-sub
  ::search-term
  :<- [::search-params]
  :<- [::search-tag]
  (fn [[{:keys [query] :as _search-params} search-tag] _]
    (or (not-empty query) search-tag)))

(reg-sub
  ::search-label
  :<- [::search-params]
  (fn [{:keys [label] :as _search-params} _]
    label))

(reg-sub
  ::jobsboard
  :<- [::results]
  (fn [{:keys [jobs-search] :as _results} _]
    (search-results/get-jobsboard-db jobs-search (:search-params jobs-search))))

(reg-sub
  ::admin?
  (fn [db _]
    (user-common/admin? db)))

(reg-sub
  ::user-email
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/email])))

(reg-sub
  ::search
  :<- [::results]
  :<- [::admin?]
  :<- [::user-email]
  (fn [[{:keys [jobs-search] :as _results} admin? user-email] _]
    (let [params (search-results/organize-search-params
                   user-email jobs-search (:search-params jobs-search))]
      (search-results/get-search-data admin? params))))

(reg-sub
  ::filters
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/filters])))

(reg-sub
  ::result-count
  :<- [::jobsboard]
  (fn [db _]
    (get-in db [::jobsboard/number-of-search-results] 0)))

(reg-sub
  ::current-page
  :<- [::jobsboard]
  (fn [db _]
    (get-in db [::jobsboard/current-page])))

(reg-sub
  ::total-pages
  :<- [::jobsboard]
  (fn [db _]
    (get-in db [::jobsboard/total-pages])))

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
  :<- [::search-term]
  (fn [[vertical search-term] _]
    (or (get (verticals/config vertical :jobsboard-tag-desc) search-term)
        (:description (verticals/config vertical :jobsboard-header)))))

(reg-sub
  ::header-info
  :<- [::header-title]
  :<- [::header-subtitle]
  :<- [::header-description]
  (fn [[title subtitle description] _]
    {:title title :subtitle subtitle :description description}))

(reg-sub
  ::results
  (fn [db _]
    (gqlc/cache-results events/jobs db [])))

(reg-sub
  ::jobs-search
  :<- [::results]
  (fn [results _]
    (:jobs-search results)))

(reg-sub
  :wh.search/searching?
  :<- [::ssr-jobs]
  :<- [::db]
  (fn [[ssr-jobs db] _]
    (if ssr-jobs
      false
      (gqlc/cache-loading? events/jobs db))))

(reg-sub
  ::jobs
  :<- [::jobs-search]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  :<- [::ssr-jobs]
  (fn [[jobs-search liked-jobs applied-jobs ssr-jobs] _]
    (->> (or (:jobs jobs-search) ssr-jobs)
         (job/add-interactions liked-jobs applied-jobs)
         (map job/translate-job))))

(reg-sub
  ::promoted-jobs
  :<- [::jobs-search]
  :<- [:user/liked-jobs]
  :<- [:user/applied-jobs]
  (fn [[jobs-search liked-jobs applied-jobs] _]
    (->> (:promoted jobs-search)
         (job/add-interactions liked-jobs applied-jobs)
         (map job/translate-job))))

(defn- checkbox-description
  [{:keys [value label cnt display-count?]
    :or   {display-count? true}}]
  (let [cnt (or cnt 0)]
    {:value value
     :label (str (or label value) (when (and display-count? (pos? cnt)) (str " (" cnt ")")))}))

(reg-sub
  :wh.search/available-role-types
  (fn [_db _]
    (for [role-type job-db/role-types]
      {:value role-type
       :label role-type})))

(reg-sub
  :wh.search/only-mine-desc
  :<- [::search]
  (fn [search _]
    (checkbox-description {:label "Only my jobs"
                           :cnt   (:wh.search/mine-count search)})))

(reg-sub
  :wh.search/role-types
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/role-types])))

(reg-sub
  :wh.search/query
  :<- [::search]
  (fn [search _]
    (:wh.search/query search)))

(reg-sub
  ::current-query
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/query])))

(reg-sub
  :wh.search/sponsorship
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/sponsorship])))

(reg-sub
  :wh.search/remote
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/remote])))

(reg-sub
  :wh.search/show-competitive?
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/competitive])))

(reg-sub
  :wh.search/tags
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/tags])))

(reg-sub
  :wh.search/selected-tags
  :<- [:wh.search/tags]
  (fn [tags _]
    (map #(str (slug/tag-label->slug %) ":tech") tags)))

(reg-sub
  :wh.search/available-tags
  :<- [::filters]
  (fn [filters _]
    (->> (:wh.search/available-tags filters)
         (map
           (fn [{:keys [value _attr _count] :as tag}]
             (merge
               tag
               {:label value
                :slug  (slug/tag-label->slug value)
                :type  "tech"})))
         (sort-by :count >))))

;; Non-existing tags is a list of tags that appeared in URL
;; but are not available in application DB. We want to display
;; this tags so user may remove these from filters and clear URL
(reg-sub
  :wh.search/non-existing-tags
  :<- [:wh.search/available-tags]
  :<- [:wh.search/tags]
  (fn [[available-tags tags] _]
    (let [available-tags    (set (map :value available-tags))
          non-existing-tags (remove available-tags tags)]
      (for [value non-existing-tags]
        {:label            value
         :slug             (slug/tag-label->slug value)
         :type             "tech"
         :value            value
         :selected         true
         :count            0
         :tag/non-existing true}))))

(reg-sub
  :wh.search/visible-tags
  :<- [:wh.search/non-existing-tags]
  :<- [:wh.search/available-tags]
  (fn [[non-existing-tags available-tags] _]
    (concat non-existing-tags available-tags)))

(reg-sub
  :wh.search/city-info
  :<- [::filters]
  (fn [filters _]
    (:wh.search/city-info filters)))

(reg-sub
  :wh.search/country-names
  :<- [:wh.search/city-info]
  (fn [city-info _]
    (into {} (map (juxt :country-code :country)) city-info)))

(reg-sub
  :wh.search/available-locations
  :<- [::filters]
  :<- [:wh.search/country-names]
  (fn [[{:keys [wh.search/available-countries
                wh.search/available-cities
                wh.search/available-regions]}
        country-names]
       _]
    (->> available-cities
         (concat
           (map
             (fn [{:keys [value] :as region}]
               (assoc region :label value))
             available-regions))
         (concat
           (map
             (fn [{:keys [value] :as country}]
               (assoc country :label (country-names value)))
             available-countries))
         (map
           (fn [{:keys [label value attr count]}]
             {:label (or label value)
              :value value
              :slug  (slug/tag-label->slug value)
              :attr  attr
              :type  "location"
              :count count}))
         (sort-by :count >)
         (sort-by :value (fn [v _]
                           (#{"UK" "GB" "US" "Europe"} v))))))

(reg-sub
  :wh.search/city
  :<- [::current-search]
  (fn [search [_ city]]
    (contains? (get search :wh.search/cities) city)))

(reg-sub
  ::selected-locations
  :<- [::current-search]
  (fn [search _]
    (->> (select-keys search [:wh.search/cities :wh.search/countries :wh.search/regions])
         (mapcat second)
         (map #(str (slug/tag-label->slug %) ":location")))))

(reg-sub
  ::current-search
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search])))

(reg-sub
  ::result-count-str
  :<- [::result-count]
  :<- [::search-label]
  (fn [[n label] _]
    (let [label (if label (str "'" label "'") "your criteria")]
      (if (zero? n)
        (<< "We found no jobs matching ~{label} 😢")
        (<< "We found ~{n} jobs matching ~{label}")))))

(reg-sub
  ::pagination
  :<- [::current-page]
  :<- [::total-pages]
  (fn [[current-page total-pages] _]
    (pagination/generate-pagination current-page total-pages)))

(reg-sub
  ::view-type
  :<- [:wh.subs/query-params]
  (fn [params]
    (keyword (get params jobsboard-ssr/view-type-param "cards"))))

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
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/currency])))

(reg-sub
  :wh.search/currencies
  :<- [::filters]
  (fn [filters _]
    (let [currencies (->> filters
                          :wh.search/salary-ranges
                          (map :currency)
                          distinct)]
      (conj currencies "*"))))

(reg-sub
  :wh.search/salary-type
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/salary-type])))

;; This can return nil when salary ranges are not
;; yet fetched. In this case, the slider is not
;; rendered at all.

(reg-sub
  :wh.search/grouped-salary-ranges
  :<- [::filters]
  (fn [filters _]
    (when-let [ranges (:wh.search/salary-ranges filters)]
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
  :wh.search/current-salary-range
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/salary-range])))

(reg-sub
  :wh.search/salary-from
  (fn [db _]
    (get-in db [::jobsboard/sub-db ::jobsboard/search :wh.search/salary-from])))

(reg-sub
  :wh.search/salary-range
  :<- [:wh.search/current-salary-range]
  :<- [:wh.search/salary-min]
  :<- [:wh.search/salary-max]
  (fn [[current-salary-range min max] _]
    (or current-salary-range [min max])))

(reg-sub
  :wh.search/salary-range-js
  :<- [:wh.search/salary-range]
  (fn [range _]
    (clj->js range)))

(defn format-number [n]
  (let [formatter (goog.i18n.NumberFormat. nf/Format.COMPACT_SHORT)]
    (.format formatter n)))

(reg-sub
  :wh.search/salary-slider-description
  :<- [:wh.search/salary-range]
  :<- [:wh.search/salary-from]
  :<- [:wh.search/currency]
  (fn [[salary-range salary-from currency] _]
    (let [[min max] salary-range
          symbol    (currency-symbols currency)
          local-min (or salary-from min)]
      (str symbol (format-number local-min) " – " symbol (format-number max)))))

(reg-sub
  :wh.search/salary-slider-min-max
  :<- [:wh.search/salary-range]
  :<- [:wh.search/salary-from]
  :<- [:wh.search/currency]
  (fn [[salary-range salary-from currency] _]
    (let [[min max] salary-range
          symbol    (currency-symbols currency)
          local-min (or salary-from min)]
      [(str symbol (format-number local-min)) (str symbol (format-number max))])))

(reg-sub
  :wh.search/salary-slider-min
  :<- [:wh.search/salary-slider-min-max]
  (fn [[min] _] min))

(reg-sub
  :wh.search/salary-slider-max
  :<- [:wh.search/salary-slider-min-max]
  (fn [[_ max] _] max))

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


(reg-sub
  ::salary-pristine?
  :<- [:wh.search/currency]
  :<- [:wh.search/salary-type]
  :<- [:wh.search/salary-min-max]
  :<- [:wh.search/show-competitive?]
  (fn [[currency salary-type salary-min-max competitive?] _]
    (every? not [currency salary-type salary-min-max (not competitive?)])))

(reg-sub
  ::query-pristine?
  :<- [::current-query]
  (fn [current-query _]
    (empty? current-query)))

(reg-sub
  ::tags-pristine?
  :<- [:wh.search/selected-tags]
  (fn [selected-tags _]
    (empty? selected-tags)))

(reg-sub
  ::locations-pristine?
  :<- [::selected-locations]
  :<- [:wh.search/sponsorship]
  :<- [:wh.search/remote]
  (fn [[selected-locations sponsorship remote] _]
    (and (empty? selected-locations)
         (every? not [sponsorship remote]))))

(reg-sub
  ::role-types-pristine?
  :<- [:wh.search/role-types]
  (fn [role-types _]
    (empty? role-types)))

(reg-sub
  ::recommended-jobs
  (fn [db _]
    (some->> (gqlc/cache-results landing-events/recommended-jobs db [:jobs])
             (map #(assoc % :company-info (:company %))))))

(reg-sub
  ::recent-jobs
  (fn [db _]
    (gqlc/cache-results landing-events/recent-jobs db [:recent-jobs :results])))

(reg-sub
  ::side-jobs
  :<- [:user/has-recommendations?]
  :<- [::recommended-jobs]
  :<- [::recent-jobs]
  (fn [[has-recommendations? recommended-jobs recent-jobs]]
    (if has-recommendations? recommended-jobs recent-jobs)))

(reg-sub
  ::recommended-jobs-loading?
  (fn [db _]
    (gqlc/cache-loading? landing-events/recommended-jobs db)))

(reg-sub
  ::recent-jobs-loading?
  (fn [db _]
    (gqlc/cache-loading? landing-events/recent-jobs db)))

(reg-sub
  ::side-jobs-loading?
  :<- [:user/has-recommendations?]
  :<- [::recommended-jobs-loading?]
  :<- [::recent-jobs-loading?]
  (fn [[has-recommendations? recommended-jobs-loading? recent-jobs-loading?]]
    (if has-recommendations? recommended-jobs-loading? recent-jobs-loading?)))
