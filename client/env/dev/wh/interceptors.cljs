(ns wh.interceptors
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [after trim-v]]))

(defn check-and-throw
      "Throws an exception if `db` doesn't match the Spec `a-spec`."
      [a-spec db]
      (when-not (s/valid? a-spec db)
                (js/console.error (s/explain-data a-spec db))))

(def check-spec-interceptor (after (partial check-and-throw :wh.db/app-db)))

(def default-interceptors [check-spec-interceptor trim-v])
