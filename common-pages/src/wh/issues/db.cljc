(ns wh.issues.db
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])
    [wh.common.issue :refer [gql-issue->issue]]
    [wh.components.pagination :as pagination]))

#?(:cljs (s/def :wh.company/name string?)) ; FIXME: share this between client and server
#?(:cljs (s/def :wh.company/logo string?))

#?(:cljs (s/def ::company
           (s/keys :req-un [:wh.company/name]
                   :opt-un [:wh.company/logo])))

#?(:cljs (s/def ::jobs (s/coll-of map?))) ;; TODO get job spec from cljc

(s/def ::loading? boolean?)

#?(:cljs (s/def ::sub-db
           (s/keys :req [::issues
                         ::loading?]
                   :opt [::company
                         ::jobs])))

(def default-db
  {::loading?            false
   ::sorting-by          :created-at
   ::current-page-number 1
   ::issues              {}})

(def default-page-size 12)

(defn update-issues-db [initial-db {:keys [query_issues company me]}]
  (cond-> initial-db
    query_issues (update ::sub-db merge
                         {::issues              (into {} (map (comp (juxt :id identity)
                                                                    gql-issue->issue)
                                                              (:issues query_issues)))
                          ::page-size           default-page-size
                          ::count               (get-in query_issues [:pagination :total])
                          ::current-page-number (get-in query_issues [:pagination :page_number])
                          ::total-pages         (pagination/number-of-pages (get-in query_issues [:pagination :page_size])
                                                                            (get-in query_issues [:pagination :total]))
                          ::loading?            false})
    company (update ::sub-db merge {::company (into {} company)})
    me (update :wh.user.db/sub-db merge
               {:wh.user.db/welcome-msgs (set (:welcomeMsgs me))})))
