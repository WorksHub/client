(ns workspaces.feed-published-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [wh.components.activities.job-published :as job-published]
            [wh.styles.feed-published-cards :as styles]
            [workspaces.reagent :as wr]))

(def example-tags
  [{:label   "ClojureScript"
    :slug    "clojurescript"
    :type    "tech"
    :subtype "software"
    :weight  0.024}
   {:label   "Clojure"
    :slug    "clojure"
    :type    "tech"
    :subtype "software"
    :weight  0.024}
   {:label   "JavaScript"
    :slug    "javascript"
    :type    "tech"
    :subtype "software"
    :weight  0.02}])

(def example-job
  {:title            "Important job" :slug "job1" :remote true
   :tagline          "My gosh, I love this job so much!
My gosh, I love this job so much! My gosh, I love this job so much!
My gosh, I love this job so much! My gosh, I love this job so much!"
   :display-location "London, Downing Street"
   :display-date     "Yesterday"
   :display-salary   "£40K - 80K"
   :role-type        "Full time"
   :tags             example-tags
   :job-company      {:total-published-job-count 30
                      :logo                      "https://66.media.tumblr.com/a1c20cdefdfb6e94ec24acec2cbc0f6d/357e8e47e3875514-c7/s500x750/7618f350462d15fb20af63bf2156f5bce689d58c.png"
                      :name                      "My best company"}})

(defn bg [children]
  [:div
   {:style {:background-color "pink"
            :padding          20}}

   children])

(ws/defcard job-published-card
  (wr/reagent-card
    [bg
     [job-published/card example-job]]))


(defn issue-published []
  [:div "issue published!"])

(ws/defcard issue-published-card
  (wr/reagent-card [issue-published]))


(defn blog-published []
  [:div "blog published!"])

(ws/defcard blog-published-card
  (wr/reagent-card [blog-published]))

(defonce init (ws/mount))

