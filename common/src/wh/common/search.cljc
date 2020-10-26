(ns wh.common.search
  (:require [clojure.string :as str]
            [wh.util :as util]))

(defn string-params->boolean [params]
  (let [truthy? #(= % "true")]
    (util/update* params "published" truthy?)))

(defn query-params->filters [query-params]
  (let [{:strs [tags]}        query-params
        query-params          (string-params->boolean query-params)
        location-city         (get query-params "location.city")
        location-country-code (get query-params "location.country-code")
        wh-region             (get query-params "wh-region")
        manager               (get query-params "manager")
        remun-min             (get query-params "remuneration.min")
        remun-max             (get query-params "remuneration.max")
        remun-competitive     (get query-params "remuneration.competitive")
        remun-currency        (get query-params "remuneration.currency")
        remun-time-period     (get query-params "remuneration.time-period")]
    (cond-> (select-keys query-params ["role-type" "remote" "sponsorship-offered" "published"])
            tags                  (assoc :tags (str/split tags #";"))
            location-city         (assoc-in [:location :cities] (str/split location-city #";"))
            location-country-code (assoc-in [:location :country-codes] (str/split location-country-code #";"))
            wh-region             (assoc-in [:location :regions] (str/split wh-region #";"))
            manager               (assoc :manager manager)
            remun-min             (assoc-in [:remuneration :min] remun-min)
            remun-max             (assoc-in [:remuneration :max] remun-max)
            remun-competitive     (assoc-in [:remuneration :competitive] remun-competitive)
            remun-currency        (assoc-in [:remuneration :currency] remun-currency)
            remun-time-period     (assoc-in [:remuneration :time-period] remun-time-period))))
