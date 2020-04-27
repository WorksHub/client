(ns wh.common.search
  (:require [clojure.string :as str]
            [wh.util :as util]))

(defn string-params->boolean [params]
  (let [truthy? #(= % "true")]
    (util/update* params "published" truthy?)))

(defn query-params->filters [query-params]
  (let [{:strs [tags]} query-params
        query-params (string-params->boolean query-params)]
    (cond-> (select-keys query-params ["role-type" "remote" "sponsorship-offered" "published"])
      tags (assoc :tags (str/split tags #";"))
      (get query-params "location.city") (assoc-in [:location :cities] (str/split (get query-params "location.city") #";"))
      (get query-params "location.country-code") (assoc-in [:location :country-codes] (str/split (get query-params "location.country-code") #";"))
      (get query-params "wh-region") (assoc-in [:location :regions] (str/split (get query-params "wh-region") #";"))
      (get query-params "manager") (assoc :manager (get query-params "manager"))
      (get query-params "remuneration.min") (assoc-in [:remuneration :min] (get query-params "remuneration.min"))
      (get query-params "remuneration.max") (assoc-in [:remuneration :max] (get query-params "remuneration.max"))
      (get query-params "remuneration.currency") (assoc-in [:remuneration :currency] (get query-params "remuneration.currency"))
      (get query-params "remuneration.time-period") (assoc-in [:remuneration :time-period] (get query-params "remuneration.time-period")))))

