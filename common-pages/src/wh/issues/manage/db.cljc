(ns wh.issues.manage.db
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(s/def ::loading? boolean?)
(s/def ::repo-tuple (s/keys :req-un [:wh.repo/owner
                                     :wh.repo/name]))

(s/def ::repo-syncs (s/map-of ::repo-tuple (s/keys :req-un [:wh.repo.sync/id
                                                            :wh.repo.sync/total-issue-count
                                                            :wh.repo.sync/time-started]
                                                   :opt-un [:wh.repo.sync/running-issue-count
                                                            :wh.repo.sync/time-updated])))

#?(:cljs (s/def ::sub-db
           (s/keys :req [::issues
                         ::loading?
                         ::repo-syncs]
                   :opt [::company])))

(defn default-db
  [db]
  (merge {::loading?       false
          ::syncing-repos? false
          ::pending        #{}
          ::orgs           []
          ::fetched-repos  #{}
          ::syncing-issues false
          ::issues         {}
          ::repo-syncs     {}}
         db))
