(ns wh.company.common
  (:require
    [ajax.json :as ajax-json]
    [camel-snake-kebab.core :as c]
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.common.fx.google-maps :as google-maps]))

(defn get-company-suggestions
  [n success]
  {:method          :get
   :uri             "https://autocomplete.clearbit.com/v1/companies/suggest"
   :params          {:query n}
   :response-format (ajax-json/json-response-format {:keywords? true})
   :timeout         10000
   :on-success      [success]})
