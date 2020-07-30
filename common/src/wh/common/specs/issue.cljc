(ns wh.common.specs.issue
  (:require
    [#?(:clj  clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [#?(:clj  clojure.spec.gen.alpha
        :cljs cljs.spec.gen.alpha) :as gen]
    #?(:clj [wh.integrations.leona])
    #?(:clj [wh.spec.common :as sc])
    #?(:clj [wh.spec.company])
    #?(:clj [wh.spec.user :as user])
    #?(:clj [wh.url :as url])
    [wh.common.specs.date]
    [wh.common.specs.primitives]
    [wh.common.specs.repo]
    [wh.common.specs.tags]
    [wh.components.pagination :as pagination]))

(s/def :wh.issue/id #?(:clj  sc/string-uuid
                       :cljs string?))
(s/def :wh.issue/github-id string?)
(s/def :wh.issue/url string?)
(s/def :wh.issue/number int?)
(s/def :wh.issue/title string?)
(s/def :wh.issue/body string?)
(s/def :wh.issue/body-html string?)
(s/def :wh.issue.label/name string?)
(s/def :wh.issue/pr-count nat-int?)
(s/def :wh.issue/prs (s/coll-of #?(:cljs string? :clj :http/url) :distinct true))
(s/def :wh.issue/label (s/keys :req-un [:wh.issue.label/name]))
(s/def :wh.issue/labels (s/coll-of :wh.issue/label))
(s/def :wh.issue/created-at :wh/date)
(s/def :wh.issue.raw/created-at number?)
(s/def :wh.issue/added-at :wh/date)
(s/def :wh.issue/company-id #?(:clj  (s/with-gen
                                       :leona.id/string
                                       (fn [] (gen/fmap
                                                (fn [s] (url/slugify s {}))
                                                (gen/not-empty (s/gen string?)))))
                               :cljs string?))
;; TODO: this should be just :wh/company, but that doesn't fully leonaize atm

;; TODO hack hack hack until all the specs are cljc
#?(:cljs (s/def :wh.company/id string?))
#?(:cljs (s/def :wh.company/name string?))
#?(:cljs (s/def :wh.company/logo string?))
#?(:cljs (s/def :wh.company/slug string?))

(s/def :wh.issue/company (s/keys :req-un [:wh.company/id
                                          :wh.company/name
                                          :wh.company/slug]
                                 :opt-un [:wh.company/logo]))

(s/def :wh.issue.author/login string?)
(s/def :wh.issue.author/name (s/nilable string?))
(s/def :wh.issue/author (s/keys :req-un [:wh.issue.author/login]
                                :opt-un [:wh.issue.author/name]))
(s/def :wh.issue/viewer-contributed boolean?)
(s/def :wh.issue/published boolean?)
(s/def :wh.issue/status #{:open :closed})
(s/def :wh.issue.raw/status #{"open" "closed"})
(s/def :wh.issue/repo-id :wh.repo/id)
(s/def :wh.issue/repo :wh/repo)

(s/def :wh.issue.compensation/amount nat-int?)
(s/def :wh.issue.compensation/currency     #{:EUR  :GBP  :USD  :BTC  :AUD  :CAD  :CHF  :KHD  :NOK  :SEK  :SGD  :PLN  :YEN})
(s/def :wh.issue.raw.compensation/currency #{"EUR" "GBP" "USD" "BTC" "AUD" "CAD" "CHF" "KHD" "NOK" "SEK" "SGD" "PLN" "YEN"})

(s/def :wh.issue/compensation (s/keys :req-un [:wh.issue.compensation/amount
                                               :wh.issue.compensation/currency]))

;; TODO remove this when spec is cljc
#?(:clj (s/def :wh.issue/contributor :wh/user))
#?(:clj (s/def :wh.issue/contributors (s/coll-of :wh.issue/contributor)))

(s/def :wh.issue/level #{:beginner :intermediate :advanced})
(s/def :wh.issue.raw/level #{"beginner" "intermediate" "advanced"})

(s/def :wh/issue #?(:clj  (s/keys :req-un [:wh.issue/body
                                           :wh.issue/body-html
                                           :wh.issue/company-id
                                           :wh.issue/created-at
                                           :wh.issue/level
                                           :wh.issue/number
                                           :wh.issue/title
                                           :wh.issue/url
                                           :wh.issue/github-id
                                           :wh.issue/repo-id
                                           :wh.issue/status]
                                  :opt-un [:wh.issue/added-at
                                           :wh.issue/author
                                           :wh.issue/company
                                           :wh.issue/compensation
                                           :wh.issue/contributors
                                           :wh.issue/id
                                           :wh.issue/labels
                                           :wh.issue/published
                                           :wh.issue/repo
                                           :wh.issue/viewer-contributed
                                           :wh.issue/pr-count
                                           :wh.issue/prs])
                    :cljs (s/keys :opt-un [:wh.issue/author
                                           :wh.issue/body
                                           :wh.issue/body-html
                                           :wh.issue/company
                                           :wh.issue/company-id
                                           :wh.issue/compensation
                                           :wh.issue/contributors
                                           :wh.issue/github-id
                                           :wh.issue/id
                                           :wh.issue/labels
                                           :wh.issue/level
                                           :wh.issue/number
                                           :wh.issue/pr-count
                                           :wh.issue/prs
                                           :wh.issue/published
                                           :wh.issue/repo
                                           :wh.issue/repo-id
                                           :wh.issue/status
                                           :wh.issue/title
                                           :wh.issue/url
                                           :wh.issue/viewer-contributed])))

(def algolia-fields
  [:wh.issue/status
   :wh.issue/title
   :wh.issue/body
   :wh.issue/compensation
   :wh.issue/tags
   ;; company logo and name
   :wh.issue/company
   ;; contributors count
   :wh.issue/contributors
   :wh.issue/id
   ;; TODO: transform labels into tags
   ;; :wh.issue/labels
   :wh.issue/level
   :wh.issue/pr-count
   ;; org name and repo name
   :wh.issue/repo])

(s/def :wh.issue/raw
  #?(:clj  (-> :wh/issue
               (sc/replace-spec
                 {:wh.issue/status :wh.issue.raw/status})
               (sc/replace-spec
                 {:wh.issue/level :wh.issue.raw/level})
               (sc/replace-spec
                 {:wh.issue.compensation/currency :wh.issue.raw.compensation/currency}))
     :cljs :wh/issue))

(s/def :wh/issues (s/coll-of :wh/issue))

(s/def :wh.issues/sort #{:published :compensation})

(s/def :wh.feed.issue/compensation
  (s/keys :req-un [:wh.issue.compensation/amount
                   :wh.issue.raw.compensation/currency]))

(s/def :wh.feed.issue/contributors-count int?)

(s/def :wh.feed.issue/issue-company
  (s/keys :req-un [:wh.company/id
                   :wh.company/name
                   :wh.company/slug]
          :opt-un [:wh.company/logo
                   :wh.company/total-published-job-count
                   :wh.company/total-published-issue-count]))

(s/def :wh.feed.issue/issue-contributor
  (s/keys :req-un [:wh.user/id
                   :wh.user/name]
          :opt-un [:wh.spec.user/image-url]))

(s/def :wh.feed/feed-issue
  (s/keys :req-un [:wh.issue/id
                   :wh.issue/body
                   :wh.issue/body-html
                   :wh.issue/company-id
                   :wh.issue/created-at
                   :wh.issue.raw/level
                   :wh.issue/number
                   :wh.issue/title
                   :wh.issue/url
                   :wh.issue/github-id
                   :wh.issue/repo-id
                   :wh.issue.raw/status]
          :opt-un [:wh.gql/tags
                   :wh.issue/repo
                   :wh.feed.issue/issue-company
                   :wh.feed.issue/issue-contributor
                   :wh.issue/pr-count
                   :wh.feed.issue/compensation
                   :wh.feed.issue/contributors-count]))
