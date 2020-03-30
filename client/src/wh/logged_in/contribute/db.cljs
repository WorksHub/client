(ns wh.logged-in.contribute.db
  (:require
    [cljs.spec.alpha :as s]
    [wh.common.specs.primitives :as primitives]
    [wh.db :as db]
    [wh.user.db :as user]))

(defn has-error? [k db]
  (not (s/valid? k (get db k))))

(s/def ::title             ::primitives/non-empty-string)
(s/def ::author            ::primitives/non-empty-string)
(s/def ::feature           ::primitives/non-empty-string)
(s/def ::body              ::primitives/non-empty-string)
(s/def ::associated-jobs   (s/coll-of ::primitives/non-empty-string))
(s/def ::original-source   ::primitives/url)
(s/def ::tag-id            ::primitives/non-empty-string)
(s/def ::tag-search        string?)
(s/def ::selected?         boolean?)
(s/def ::selected-tag-ids  (s/coll-of ::tag-id :distinct true :min-count 1))
(s/def ::published         boolean?)

(s/def ::blog-publish (s/keys :req [::title ::author ::feature ::body ::selected-tag-ids]
                              :opt [::original-source ::company-id]))

(s/def ::blog-save (s/keys :req [::title ::author ::body ::selected-tag-ids]
                           :opt [::feature ::original-source ::company-id]))

(defn select-spec [blog]
  (if (::published blog)
    ::blog-publish
    ::blog-save))

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

(s/def ::sub-db (s/keys :opt [::title ::author ::feature ::body ::save-status ::published
                              ::image-article-upload-status ::hero-upload-status
                              ::body-editing? ::body-rows ::selected-tag-ids
                              ::tag-search ::original-source ::company-id ::company-name]))

(def form-order
  [::feature ::author ::title ::body ::selected-tag-ids ::original-source])

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
          ::associated-jobs []
          ::hide-codi? false
          ::verticals #{(::db/vertical db)}
          ::primary-vertical (::db/vertical db)}
         (when-not (user/admin? db)
           {::author (user/user-name db)})
         (when (user/company? db)
           {::company-id (user/company-id db)})))
