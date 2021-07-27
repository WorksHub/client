(ns wh.common.blog
  (:require [wh.common.text :as text]
            [wh.common.time :as time]))

(defn translate-blog [blog]
  (try
    (-> blog
        (assoc
          :display-date (time/human-time (time/str->time (:creation-date blog) :date-time))))
    (catch #?(:cljs js/Error :clj Exception) _
      (assoc blog :display-date "???"))))

(defn format-reading-time [blog]
  (let [reading-time (max 1 (:reading-time blog))]
    (str reading-time " " (text/pluralize reading-time "min"))))

(defn created-by-company? [blog]
  (boolean (:company-id blog)))

(defn created-by-user? [blog]
  (boolean (:author-id blog)))

;;

(defn can-edit? [{:keys [admin? published? creator-id user-id]}]
  (or admin?
      (and (and creator-id (= creator-id user-id))
           (false? published?))))

;;

(defn show-unpublished? [{:keys [admin? published? creator-id user-id]}]
  (and (false? published?)
       (or admin?
           (and creator-id (= creator-id user-id)))))
