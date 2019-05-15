(ns wh.common.user
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn absolute-image-url [image-url]
  (if (and (not (str/blank? image-url))
           (str/starts-with? image-url "/images/"))
    (str "https://www.works-hub.com" image-url)
    image-url))

(defn predefined-avatar? [image-url]
  (and (not (str/blank? image-url))
       (or (re-matches #"^https://www.works-hub.com/images/avatar-([0-9])+.png$" image-url)
           (re-matches #"^/images/avatar-([0-9])+.svg$" image-url))))

(defn url->predefined-avatar [url]
  (when-let [[_ i] (predefined-avatar? url)]
    #?(:cljs (js/parseInt i)
       :clj  (Integer/parseInt i))))

(defn avatar-url [i]
  (str "https://www.works-hub.com/images/avatar-" i ".png"))

(defn random-avatar-url []
  (avatar-url (inc (rand-int 5))))

(defn pngify-image-url [image-url]
  ;; We ensure the image is a PNG so the email clients can properly display it
  (when-not (str/blank? image-url)
    (str/replace image-url #"svg" "png")))