(ns wh.logged-in.contribute.db
  (:require
    [cljs.spec.alpha :as s]
    [wh.common.specs.primitives :as primitives]
    [wh.db :as db]
    [wh.user.db :as user]))

(defn has-error? [k db]
  (not (s/valid? k (get db k))))

(s/def ::title           ::primitives/non-empty-string)
(s/def ::author          ::primitives/non-empty-string)
(s/def ::feature         ::primitives/non-empty-string)
(s/def ::body            ::primitives/non-empty-string)
(s/def ::original-source ::primitives/url)
(s/def ::tag             ::primitives/non-empty-string)
(s/def ::tag-search      string?)
(s/def ::selected?       boolean?)
(s/def ::tag-container   (s/keys :req-un [::tag]
                                 :opt-un [::selected]))
(s/def ::tags            (s/coll-of ::tag-container :distinct true :min-count 1))
(s/def ::available-tags  (s/coll-of ::tag-container :distinct true))
(s/def ::published       boolean?)

(s/def ::blog (s/keys :req [::title ::author ::feature ::body ::tags]
                      :opt [::original-source ::company-id]))

(s/def ::company-id   (s/nilable ::primitives/non-empty-string))
(s/def ::company-name (s/nilable ::primitives/non-empty-string))


(s/def ::save-status #{:success :failure :tried})

(s/def ::upload-status #{:not-started :started :success :failure :failure-too-big})
(s/def ::image-article-upload-status ::upload-status)
(s/def ::hero-upload-status ::upload-status)

(s/def ::body-cursor-position nat-int?)
(s/def ::body-editing? boolean?)
(s/def ::body-rows int?)

(s/def ::hide-codi? boolean?)

(s/def ::tag-search (s/nilable string?))

(s/def ::sub-db (s/keys :opt [::title ::author ::feature ::body ::tags ::save-status ::published
                              ::save-status ::image-article-upload-status ::hero-upload-status
                              ::body-editing? ::body-rows ::available-tags
                              ::tag-search ::original-source ::company-id ::company-name]))

(def form-order
  [::feature ::author ::title ::body ::tags ::original-source])

(defn form-field-id
  [k]
  (str "contribute__form__" (name k)))

(defn default-db
  [db]
  (merge {::image-article-upload-status :not-started
          ::hero-upload-status :not-started
          ::body-editing? true
          ::published false
          ::body-cursor-position 0
          ::body-rows 10
          ::hide-codi? false
          ::verticals #{(::db/vertical db)}
          ::primary-vertical (::db/vertical db)}
         (when (or (user/candidate? db)
                   (user/company? db))
           {::author (user/user-name db)})
         (when (user/company? db)
           {::company-id (user/company-id db)})))
