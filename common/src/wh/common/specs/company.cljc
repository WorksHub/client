(ns wh.common.specs.company
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [wh.common.data :as data]
            [wh.common.specs.date]
            [wh.common.specs.primitives :as p]
            [wh.common.specs.tags]))

(def description-placeholder "Please enter a valid description for your company")

(def sizes #{:micro :small :medium :large})

(def size->range
  {:micro  "1-9"
   :small  "10-49"
   :medium "50-249"
   :large  "250+"})

(s/def :wh.company/auto-approve boolean?)
(s/def :wh.company/connected-github boolean?)
(s/def :wh.company/description ::p/non-empty-string)
(s/def :wh.company/description-html ::p/non-empty-string)
(s/def :wh.company/disabled boolean?)
(s/def :wh.company/managed boolean?)
(s/def :wh.company/domain ::p/non-empty-string)
(s/def :wh.company/free-trial-ended :wh/date)
(s/def :wh.company/free-trial-started :wh/date)
(s/def :wh.company/github-installation-id string?)
(s/def :wh.company/has-jobs boolean?)
(s/def :wh.company/has-published-profile boolean?)
(s/def :wh.company/job-last-modified :wh/date)
(s/def :wh.company/logo ::p/non-empty-string)
(s/def :wh.company/name ::p/non-empty-string)
(s/def :wh.company/paid-offline-until :wh/date)
(s/def :wh.company/profile-enabled boolean?)
(s/def :wh.company/profile-last-modified :wh/date)
(s/def :wh.company/total-published-issue-count nat-int?)
(s/def :wh.company/total-published-job-count nat-int?)

(s/def :wh.company/tags (s/coll-of :wh/tag))
(s/def :wh.company/tag-ids (s/coll-of :wh.tag/id))
(s/def :wh.company/size sizes)

(s/def :wh.company/founded-year pos-int?)
(s/def :wh.company/how-we-work ::p/non-empty-string)
(s/def :wh.company/additional-tech-info ::p/non-empty-string)

(s/def :wh.company/onboarding-msg #{:dashboard_welcome})
(s/def :wh.company/onboarding-msgs (s/coll-of :wh.company/onboarding-msg :distinct true))

(s/def :wh.company.onboarding-task/id #{:complete_profile :add_job :add_integration :add_issue})
(s/def :wh.company.onboarding-task/state #{:read :complete})
(s/def :wh.company/onboarding-task (s/keys :req-un [:wh.company.onboarding-task/id
                                                    :wh.company.onboarding-task/state]))
(s/def :wh.company/onboarding-tasks (s/coll-of :wh.company/onboarding-task))

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
(defn includes-company? [c] ((set (map :type c)) :company))
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
                                       includes-benefit?
                                       includes-company?))

(s/def :wh.company.profile/blogs (s/coll-of map? :min-count 1))
(s/def :wh.company.profile/images (s/coll-of map? :min-count 1))
(s/def :wh.company.profile/videos (s/coll-of map? :min-count 1))
(s/def :wh.company.profile/locations (s/coll-of map? :min-count 1))

(s/def :wh.company/minimum-company (s/keys :req-un [:wh.company.profile/logo
                                                    :wh.company/name
                                                    :wh.company.profile/description-html]))

(s/def :wh.company/top-score-profile (s/keys :req-un [:wh.company.profile/logo
                                                      :wh.company/name
                                                      :wh.company.profile/description-html
                                                      :wh.company.profile/tags
                                                      :wh.company/how-we-work
                                                      :wh.company/size
                                                      :wh.company/founded-year
                                                      :wh.company.profile/blogs
                                                      :wh.company.profile/images
                                                      :wh.company.profile/videos
                                                      :wh.company.profile/locations
                                                      :wh.company/tech-scales]))

(s/def :wh.company/top-company (s/keys :req-un [:wh.company/name
                                                :wh.company/id
                                                :wh.company/slug]
                                       :opt-un [:wh.company/locations
                                                :wh.company/logo
                                                :wh/tags
                                                :wh.company/total-published-issue-count
                                                :wh.company/total-published-job-count]))

;; Amount of jobs that company can create/publish
;; We market some of our packages as giving unlimited jobs, but in backend
;; we represent unlimited as 999 to avoid spoiling this spec and app logic
;; with 'infinity' concept
(s/def :wh.company/job-quota
  (s/int-in data/min-job-quota (inc data/max-job-quota)))
