(ns wh.issue.edit.db
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(s/def ::displayed-dialog #{nil :edit :confirm})
(s/def ::updating? boolean?)
(s/def ::pending-level (s/nilable keyword?)) ;; TODO use real spec once it's in CLJC
(s/def ::pending-status #{nil :open :closed})
(s/def ::pending-compensation (s/nilable nat-int?))

(s/def ::on-success (s/nilable seq?))

(def default-db
  {::displayed-dialog     nil
   ::pending-level        nil
   ::pending-status       nil
   ::pending-compensation nil
   ::on-success           nil
   ::updating?            false})
