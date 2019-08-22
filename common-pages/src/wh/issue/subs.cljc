(ns wh.issue.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw subscribe]]
    [wh.util :as util]
    [wh.issue.db :as issue]
    #?(:clj [clojure.core.strint :refer [<<]]))
  #?(:cljs (:require-macros
             [reagent.ratom :refer [reaction]]
             [clojure.core.strint :refer [<<]])))

(reg-sub ::sub-db (fn [db _] (::issue/sub-db db)))

(reg-sub
  ::issue
  :<- [::sub-db]
  (fn [db _]
    (::issue/issue db)))

(reg-sub
  ::title
  :<- [::issue]
  (fn [issue _]
    (:title issue)))

(reg-sub
  ::body
  :<- [::issue]
  (fn [issue _]
    (:body-html issue)))

(reg-sub
  ::url
  :<- [::issue]
  (fn [issue _]
    (:url issue)))

(reg-sub
  ::level
  :<- [::issue]
  (fn [issue _]
    (:level issue)))

(reg-sub
  ::pending-level
  :<- [::sub-db]
  :<- [::level]
  (fn [[sub-db current-level] _]
    (or (::issue/pending-level sub-db) current-level)))

(reg-sub
  ::author
  :<- [::issue]
  (fn [issue _]
    (:author issue)))

(reg-sub
  ::pr-count
  :<- [::issue]
  (fn [issue _]
    (:pr-count issue)))

(reg-sub
  ::contributors
  :<- [::issue]
  (fn [issue _]
    (:contributors issue)))

(reg-sub
  ::contributor-count
  :<- [::contributors]
  (fn [contributors _]
    (count contributors)))

(reg-sub
  ::labels
  :<- [::issue]
  (fn [issue _]
    (map :name (:labels issue))))

(reg-sub
  ::primary-language
  :<- [::issue]
  (fn [issue _]
    (get-in issue [:repo :primary-language])))

(reg-sub
  ::repo
  :<- [::issue]
  (fn [issue [_ k]]
    (if k
      (get-in issue [:repo k])
      (:repo issue))))

(reg-sub
  ::repo-url
  :<- [::repo]
  (fn [{:keys [name owner]} _]
    (str "https://github.com/" owner "/" name)))

(reg-sub
  ::github-login
  :<- [::sub-db]
  (fn [sub-db _]
    (::issue/github-login sub-db)))

(reg-sub
  ::clone-url
  :<- [::github-login]
  :<- [::issue]
  (fn [[gh-login issue] _]
    (let [gh-login (or gh-login "your-github-login")
          {:keys [name]} (:repo issue)]
      (<< "git@github.com:~{gh-login}/~{name}.git"))))

(reg-sub
  ::fork-url
  :<- [::issue]
  (fn [issue _]
    (let [{:keys [owner name]} (:repo issue)]
      (<< "https://github.com/~{owner}/~{name}/fork"))))

(reg-sub
  ::community
  :<- [::issue]
  (fn [issue _]
    (get-in issue [:repo :community])))

(reg-sub
  ::status
  :<- [::issue]
  (fn [issue _]
    (:status issue)))

(reg-sub
  ::company-id
  :<- [::issue]
  (fn [issue _]
    (get-in issue [:company :id])))

(reg-sub
  ::company-name
  :<- [::issue]
  (fn [issue _]
    (get-in issue [:company :name])))

(reg-sub
  ::company-logo
  :<- [::issue]
  (fn [issue _]
    (get-in issue [:company :logo])))

(reg-sub
  ::viewer-contributed?
  :<- [::issue]
  (fn [issue _]
    (:viewer-contributed issue)))

(reg-sub
  ::start-work-popup-shown?
  :<- [::sub-db]
  (fn [db _]
    (::issue/start-work-popup-shown? db)))

(reg-sub
  ::contribute-in-progress?
  :<- [::sub-db]
  (fn [db _]
    (::issue/contribute-in-progress? db)))

(reg-sub
  ::show-cta-sticky?
  :<- [::sub-db]
  (fn [db _]
    (::issue/show-cta-sticky? db)))

(reg-sub
  ::updating?
  :<- [::sub-db]
  (fn [db _]
    (::issue/updating? db)))

(reg-sub
  ::num-related-jobs-to-show
  (constantly issue/num-related-jobs-to-show))

(reg-sub
  ::num-other-issues-to-show
  (constantly issue/num-other-issues-to-show))

(reg-sub
  ::company-jobs
  :<- [::sub-db]
  :<- [::num-related-jobs-to-show]
  (fn [[db nx] [_ n]]
    (not-empty
     (take (or n nx) (::issue/company-jobs db)))))

(reg-sub
  ::company-issues
  :<- [::sub-db]
  :<- [::num-other-issues-to-show]
  (fn [[db nx] [_ n]]
    (take (or n nx) (::issue/company-issues db))))

(reg-sub
  ::compensation
  :<- [::issue]
  (fn [issue _]
    (when-let [compensation (get-in issue [:compensation :amount])]
      (when (pos? compensation)
        (str "$" compensation)))))

#?(:cljs
   (reg-sub-raw
    ::show-contributors?
    (fn [db _]
      (reaction
       (let [id     @(subscribe [::company-id])
             owner? @(subscribe [:user/owner? id])
             admin? @(subscribe [:user/admin?])]
         (or owner? admin?)))))
   :clj
   (reg-sub
     ::show-contributors?
     (fn [db _]
       false)))
