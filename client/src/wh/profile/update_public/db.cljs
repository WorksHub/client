(ns wh.profile.update-public.db
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [wh.common.specs.primitives :as specs]))

(s/def ::id string?)
(s/def ::name string?)
(s/def ::summary string?)
(s/def ::image-url string?)
(s/def ::other-urls (s/keys :req-un [::specs/url]))
(s/def ::editing-profile? boolean?)
(s/def ::submit-attempted? boolean?)
(s/def ::initial-values (s/keys :opt-un [::id ::name ::summary ::image-url ::other-urls]))
(s/def ::sub-db (s/keys :opt-un [::id ::name ::summary ::image-url ::other-urls
                                 ::initial-values ::editing-profile? ::submit-attempted?]))

(defn form->errors
  [form]
  (cond-> {}
          (str/blank? (:name form))
          (assoc :name "Please fill in your name")

          (->> (:other-urls form)
               (map :url)
               (remove empty?)
               (every? #(s/valid? ::specs/url %))
               not)
          (assoc :other-urls "One of the website links is invalid")

          (> (count (:summary form)) 1000)
          (assoc :summary "Summary should be less than 1000 symbols")))