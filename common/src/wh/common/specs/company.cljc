(ns wh.common.specs.company
  (:require
    [#?(:clj clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [wh.common.specs.primitives :as p]
    [wh.common.specs.tags]))

(def description-placeholder "Please enter a valid description for your company")

(def sizes #{:micro :small :medium :large})

(def size->range
  {:micro  "1-9"
   :small  "10-49"
   :medium "50-249"
   :large  "250+"})

(s/def :wh.company/name ::p/non-empty-string)

(s/def :wh.company/tags (s/coll-of :wh/tag))
(s/def :wh.company/tag-ids (s/coll-of :wh.tag/id))
(s/def :wh.company/size sizes)

(s/def :wh.company/founded-year pos-int?)
(s/def :wh.company/how-we-work ::p/non-empty-string)
(s/def :wh.company/additional-tech-info ::p/non-empty-string)

(s/def :wh.company.tech-scales/testing        (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company.tech-scales/ops            (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company.tech-scales/time-to-deploy (s/double-in :min 0.0 :max 1.0))
(s/def :wh.company/tech-scales (s/keys :opt-un [:wh.company.tech-scales/testing
                                                :wh.company.tech-scales/ops
                                                :wh.company.tech-scales/time-to-deploy]))

;; these 'profile' keys exist because creating a company without these
;; fields is fine, but publishing a profile is not
(defn includes-tech? [c] ((set (map :type c)) :tech))
(defn includes-industry? [c] ((set (map :type c)) :industry))
(defn includes-funding? [c] ((set (map :type c)) :funding))
(defn includes-benefit? [c] ((set (map :type c)) :benefit))
(defn description-is-not-placeholder? [d]
  (not (or (= d description-placeholder)
           (= d (str "<p>" description-placeholder "</p>")))))
(defn description-is-not-empty-html? [d]
  (not (= d "<p><br></p>")))

(s/def :wh.company.profile/logo ::p/non-empty-string)
(s/def :wh.company.profile/description-html (s/and ::p/non-empty-string
                                                   description-is-not-placeholder?
                                                   description-is-not-empty-html?))
(s/def :wh.company.profile/tags (s/and :wh/tags
                                       includes-industry?
                                       includes-funding?
                                       includes-tech?
                                       includes-benefit?))

(s/def :wh.company/minimum-profile (s/keys :req-un [:wh.company.profile/logo
                                                    :wh.company/name
                                                    :wh.company.profile/description-html
                                                    :wh.company.profile/tags
                                                    :wh.company/size
                                                    :wh.company/founded-year]))
