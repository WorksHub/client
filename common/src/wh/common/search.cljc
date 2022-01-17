(ns wh.common.search
  (:require [bidi.bidi :as bidi]
            [clojure.string :as str]
            [wh.util :as util]))

(defn- parse-param [param]
  (if (some? param)
    ;; NB: Probably there is a more elegant way
    ;; of achieving this w/ `bidi` protocols,
    ;; but we don't care much anymore...
    (str/replace (bidi/url-decode param) "+" " ")
    ""))

(defn ->search-term
  "Retrieves the current 'search-term' from both path and query params.
   It first looks for the `query` parameter in the passed `params` map.
   Then for the `param-name` (\"search\" by default) in `query-params`.
   NB: It also decodes the params values, accounting for \"+\" as well."
  ([params query-params]
   (->search-term params query-params "search"))
  ([{:keys [query] :as _params} query-params param-name]
   (if (str/blank? query)
     (or (some-> query-params (get param-name) parse-param) "")
     (parse-param query))))

(defn string-params->boolean [params]
  (let [truthy? #(= % "true")]
    (util/update* params "published" truthy?)))

(defn query-params->filters [query-params]
  (let [{:strs [tags]}        query-params
        query-params          (string-params->boolean query-params)
        location-city         (get query-params "location.city")
        location-region       (get query-params "location.region")
        location-country-code (get query-params "location.country-code")
        manager               (get query-params "manager")
        remun-min             (get query-params "remuneration.min")
        remun-max             (get query-params "remuneration.max")
        remun-competitive     (get query-params "remuneration.competitive")
        remun-currency        (get query-params "remuneration.currency")
        remun-time-period     (get query-params "remuneration.time-period")]
    (cond-> (select-keys query-params ["role-type" "remote" "sponsorship-offered" "published"])
            tags                  (assoc :tags (str/split tags #";"))
            location-city         (assoc-in [:location :cities] (str/split location-city #";"))
            location-region       (assoc-in [:location :regions] (str/split location-region #";"))
            location-country-code (assoc-in [:location :country-codes] (str/split location-country-code #";"))
            manager               (assoc :manager manager)
            remun-min             (assoc-in [:remuneration :min] remun-min)
            remun-max             (assoc-in [:remuneration :max] remun-max)
            remun-competitive     (assoc-in [:remuneration :competitive] remun-competitive)
            remun-currency        (assoc-in [:remuneration :currency] remun-currency)
            remun-time-period     (assoc-in [:remuneration :time-period] remun-time-period))))
