(ns wh.register.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [wh.common.errors :refer [upsert-user-error-message]]
            [wh.db :as db]
            [wh.verticals :as verticals]
            [wh.register.db :as register]))

(reg-sub ::step register/effective-step)

(reg-sub ::sub-db (fn [db _] (::register/sub-db db)))

(reg-sub
  ::skills-info-hidden?
  :<- [::sub-db]
  (fn [db _]
    (::register/skills-info-hidden? db)))

(reg-sub
  ::name
  (fn [db _]
    (get-in db [::register/sub-db ::register/name])))

(reg-sub
  ::preset-name?
  :<- [::sub-db]
  (fn [db _]
    (::register/preset-name? db)))

(reg-sub
  ::location-stage-visible?
  :<- [::sub-db]
  (fn [db [_ stage]]
    (let [current-stage (::register/location-stage db)]
      (and (register/location-stage<= stage current-stage)
           (case stage
             :confirm-preferred-location (::register/preset-location db)
             :ask-for-preferred-location (or (not (::register/skip-ask-for-preferred-location db))
                                             (not (::register/preset-location db)))
             :confirm-current-location (::register/preferred-location db)
             true)))))

(reg-sub
  ::preset-city
  :<- [::sub-db]
  (fn [db _]
    (get-in db [::register/preset-location ::register/city])))

(reg-sub
  ::preferred-city
  :<- [::sub-db]
  (fn [db _]
    (get-in db [::register/preferred-location ::register/city])))

(reg-sub
  ::manual-location?
  (fn [db _]
    (get-in db [::register/sub-db ::register/manual-location?])))

(reg-sub
  ::add-skill-visible?
  (fn [db _]
    (get-in db [::register/sub-db ::register/add-skill-visible?])))

(reg-sub
  ::location-query
  :<- [::sub-db]
  (fn [db [_ type]]
    (if (= type :preferred)
      (::register/preferred-location-query db)
      (::register/current-location-query db))))

(reg-sub
  ::location-search-results
  :<- [::sub-db]
  (fn [db [_ type]]
    (let [key (if (= type :preferred)
                ::register/preferred-location-search-results
                ::register/current-location-search-results)]
      (for [loc (get db key)]
        {:id loc, :label (str (::register/city loc) ", " (::register/country loc))}))))

(reg-sub
  ::location-search-error
  :<- [::sub-db]
  (fn [db [_ type]]
    (if (= type :preferred)
      (::register/preferred-location-search-error db)
      (::register/current-location-search-error db))))

(reg-sub
  ::connected-to-github?
  (fn [db _]
    (get-in db [:wh.user.db/sub-db :wh.user.db/github-id])))

(defn bubble-data [available selected]
  (let [all-skills (distinct (into available selected))]
    (for [skill all-skills]
      {:name skill, :size (if (selected skill) :like :base)})))

(reg-sub
  ::new-skill
  (fn [db _]
    (get-in db [::register/sub-db ::register/new-skill])))

(def autosuggest-skills
  ["Scala" "Clojure" "Haskell" "Erlang" "OCaml" "ClojureScript" "Elixir"
   "Elm" "Rust" "F#" "Python" "Java" "C#" "Ruby" "Node" "C++" "JavaScript"
   "Angular" "Reagent" "React" "HTML" "CSS" "Backbone" "Swift" "Android"
   "Objective-C" "Linux" "Mongo" "Heroku" "Docker" "Apache" "play framework" "PureScript"])

(def ai-works-skills
  ["Artificial Intelligence" "Big Data" "NLP" "Deep Learning" "Neural Networks"
   "Modelling" "Algorithms" "Structured Data" "Unstructured Data" "Architecture"
   "Distributed Systems" "Data Engineering" "Data Visualization"])

(defn matching-first
  "Reorders strings so that these starting with prefix (case-insensitive)
  come first."
  [prefix strings]
  (let [prefix (str/lower-case prefix)
        pred #(str/starts-with? (str/lower-case %) prefix)]
    (concat (filter pred strings) (remove pred strings))))

(reg-sub
  ::suggested-skills
  (fn [db _]
    (let [part (str (get-in db [::register/sub-db ::register/new-skill]))
          available (set (get-in db [::register/sub-db ::register/available-skills]))
          suggested-skills (if (= "ai" (::db/vertical db)) (concat ai-works-skills autosuggest-skills) (concat autosuggest-skills ai-works-skills))]
      (when part
        (->> suggested-skills
             (filter #(and (not (available %))
                           (str/includes? (str/lower-case %) (str/lower-case part))))
             (matching-first part)
             (take 10)
             (map (fn [x] {:id x, :label x})))))))

(reg-sub
  ::skills
  (fn [db _]
    (bubble-data (get-in db [::register/sub-db ::register/available-skills])
                 (get-in db [::register/sub-db ::register/selected-skills]))))

(reg-sub
  ::cannot-proceed-from-skills?
  (fn [db _]
    (not (seq (get-in db [::register/sub-db ::register/selected-skills])))))

(reg-sub
  ::selected-riddle-language
  (fn [db _]
    (get-in db [::register/sub-db ::register/selected-riddle ::register/language])))

(reg-sub
  ::selected-riddle-code
  (fn [db _]
    (get-in db [::register/sub-db ::register/selected-riddle ::register/riddle])))

(reg-sub
  ::blank-code-answer?
  (fn [db _]
    (str/blank? (get-in db [::register/sub-db ::register/code-answer]))))

(reg-sub
  ::code-answer
  (fn [db _]
    (get-in db [::register/sub-db ::register/code-answer])))

(reg-sub
  ::failed-code-riddle-check?
  (fn [db _]
    (get-in db [::register/sub-db ::register/failed-code-riddle-check?])))

(reg-sub
  ::approval-fail?
  (fn [db _]
    (get-in db [::register/sub-db ::register/approval-fail?])))

(reg-sub
  ::selected-skills
  (fn [db _]
    (get-in db [::register/sub-db ::register/selected-skills])))

(reg-sub
  ::remote
  :<- [::sub-db]
  (fn [db _]
    (::register/remote db)))

(reg-sub
  ::all-riddles-languages
  (fn [db _]
    (mapv ::register/language (get-in db [::register/sub-db ::register/code-riddles]))))

(reg-sub
  ::fetch-riddles-error
  (fn [db _]
    (get-in db [::register/sub-db ::register/code-riddle-error])))

(reg-sub
  ::email
  (fn [db _]
    (get-in db [::register/sub-db ::register/email])))

(reg-sub
  ::upsert-user-errors
  :<- [::sub-db]
  (fn [db _]
    (upsert-user-error-message (::register/upsert-user-errors db))))

(reg-sub
  ::progress
  :<- [::step]
  (fn [step _]
    (let [step-idx (.indexOf register/step-order step)]
      (int (* 100 (/ (inc step-idx) (count register/step-order)))))))

(reg-sub
  ::loading?
  (fn [db _]
    (get-in db [::register/sub-db ::register/loading?])))

(reg-sub
  ::consented?
  (fn [db _]
    (boolean (get-in db [::register/sub-db ::register/consented]))))

(reg-sub
  ::subscribed?
  (fn [db _]
    (get-in db [::register/sub-db ::register/subscribed?])))

(reg-sub
  ::vertical-name
  (fn [db _]
    (get-in verticals/vertical-config [(::db/vertical db) :platform-name])))

(reg-sub
  ::stackoverflow-signup?
  (fn [db _]
    (-> (get-in db [:wh.user.db/sub-db :wh.user.db/stackoverflow-info])
        boolean)))
