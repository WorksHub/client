(ns wh.common.user
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn absolute-image-url [image-url]
  (if (str/starts-with? image-url "/images/")
    (str "https://www.works-hub.com" image-url)
    image-url))

(defn predefined-avatar? [image-url]
  (re-matches #"^/images/avatar-[0-9]+.svg$" image-url))

(defn avatar-url [i]
  (str "/images/avatar-" i ".svg"))

(defn random-avatar-url []
  (avatar-url (inc (rand-int 5))))

(defn pngify-image-url [image-url]
  ;; We ensure the image is a PNG so the email clients can properly display it
  (str/replace image-url #"svg" "png"))