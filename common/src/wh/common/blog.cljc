(ns wh.common.blog
  (:require [wh.common.time :as time]))

(defn translate-blog [blog]
  (-> blog
      (assoc
        :display-date (time/human-time (time/str->time (:creation-date blog) :date-time)))))
