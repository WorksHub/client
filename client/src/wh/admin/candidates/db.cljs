(ns wh.admin.candidates.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.specs.primitives :as p]
            [wh.db :as db]))

(def approval-statuses ["pending" "approved" "rejected"])

(s/def ::search-term string?)
(s/def ::page pos-int?)
(s/def ::verticals (s/coll-of string?))
(s/def ::approval-status (set approval-statuses))
(s/def ::approval-statuses (s/coll-of ::approval-status))
(s/def ::loading-state #{:error :loading :no-results :success})


(s/def ::sub-db (s/keys :req [::search-term]
                        :opt [::verticals
                              ::approval-statuses
                              ::loading-state]))
(def query-param-separator "_")

(defn candidates-query-params [db]
  {:search          (::search-term db)
   :board           (->> (::verticals db)
                         (str/join query-param-separator))
   :approval-status (->> (::approval-statuses db)
                         (str/join query-param-separator))
   :page            (name (::page db))})

(defn default-db [db]
  (let [query-params (::db/query-params db)
        verticals (some-> query-params
                          (get  "board")
                          (str/split (re-pattern query-param-separator)))
        statuses (some-> query-params
                         (get  "approval-status")
                         (str/split (re-pattern query-param-separator)))]
    {::search-term       (get query-params "search" "")
     ::page              (js/parseInt (get query-params "page" "1"))
     ::verticals         (set (or verticals [(::db/vertical db)]))
     ::approval-statuses (set (or statuses [(first approval-statuses)]))}))
