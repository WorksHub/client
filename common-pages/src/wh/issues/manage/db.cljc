(ns wh.issues.manage.db
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(s/def ::loading? boolean?)

#?(:cljs (s/def ::sub-db
           (s/keys :req [::issues
                         ::loading?]
                   :opt [::company])))

(def default-db
  {::loading?       false
   ::syncing-repos? false
   ::pending        #{}
   ::orgs           []
   ::fetched-repos  #{}
   ::syncing-issues false
   ::issues         {}})