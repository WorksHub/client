(ns wh.common.specs.repo
  (:require
    [#?(:clj  clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    #?(:clj [wh.integrations.leona])
    #?(:clj [wh.spec.common :as sc])
    #?(:clj [wh.spec.user :as user])
    [wh.common.specs.date]))

(s/def :wh.repo/id #?(:clj  sc/string-uuid
                      :cljs string?))
(s/def :wh.repo/github-id string?)
(s/def :wh.repo/viewer-can-administer boolean?)
(s/def :wh.repo/name string?)
(s/def :wh.repo/owner string?)
(s/def :wh.repo/description (s/nilable string?))
(s/def :wh.repo/primary-language (s/nilable string?))
(s/def :wh.repo/stargazers nat-int?)
(s/def :wh.repo/hook-id int?)
(s/def :wh.repo/hook-secret string?)
(s/def :wh.repo/readme-url string?)
(s/def :wh.repo/contributing-url string?)
(s/def :wh.repo/community (s/keys :opt-un [:wh.repo/readme-url
                                           :wh.repo/contributing-url]))

(s/def :wh.repo.sync/id #?(:clj  sc/string-uuid
                           :cljs string?))
(s/def :wh.repo.sync/total-issue-count nat-int?)
(s/def :wh.repo.sync/running-issue-count nat-int?)
(s/def :wh.repo.sync/batch-count nat-int?)
(s/def :wh.repo.sync/time-started :wh/date)
(s/def :wh.repo.sync/time-updated :wh/date)
(s/def :wh.repo.sync/time-finished :wh/date)
(s/def :wh.repo.sync/started-by #?(:clj  :wh.user/id
                                   :cljs string?))
(s/def :wh.repo/sync (s/keys :req-un [:wh.repo.sync/id
                                      :wh.repo.sync/total-issue-count
                                      :wh.repo.sync/running-issue-count
                                      :wh.repo.sync/batch-count
                                      :wh.repo.sync/time-started
                                      :wh.repo.sync/started-by
                                      :wh.repo.sync/time-updated]
                             :opt-un [:wh.repo.sync/time-finished]))

(s/def :wh/repo #?(:clj  (s/keys :req-un [:wh.repo/github-id
                                          :wh.repo/viewer-can-administer
                                          :wh.repo/name
                                          :wh.repo/owner
                                          :wh.repo/stargazers]
                                 :opt-un [:wh.repo/id
                                          :wh.repo/description
                                          :wh.repo/community
                                          :wh.repo/primary-language
                                          :wh.repo/hook-id
                                          :wh.repo/hook-secret
                                          :wh.repo/readme-url
                                          :wh.repo/contributing-url
                                          :wh.repo/sync])
                   :cljs (s/keys :opt-un [:wh.repo/github-id
                                          :wh.repo/viewer-can-administer
                                          :wh.repo/name
                                          :wh.repo/owner
                                          :wh.repo/description
                                          :wh.repo/stargazers
                                          :wh.repo/id
                                          :wh.repo/community
                                          :wh.repo/primary-language
                                          :wh.repo/hook-id
                                          :wh.repo/hook-secret
                                          :wh.repo/readme-url
                                          :wh.repo/contributing-url
                                          :wh.repo/sync])))
(s/def :wh/repos (s/coll-of :wh/repo))
