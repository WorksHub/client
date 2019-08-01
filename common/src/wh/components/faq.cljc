(ns wh.components.faq
  (:require
    #?(:cljs [reagent.core :as r])
    [wh.components.icons :refer [icon]]
    [wh.components.common :refer [link]]))

(defn nth-question
  [questions n]
  (if (contains? questions n) ;; checks index in questions
    (nth questions n)))

(defn question
  [n question]
  (when question
    #?(:clj
       [:div.faq__question
        {:style {:order n}}
        [:div.faq__question__title  (:title question)]
        [:div.faq__question__answer (:answer question)]]
       :cljs
       (let [collapsed? (r/atom true)]
         (fn [n]
           [:div.faq__question
            {:style {:order n}}
            [:div.faq__question__title
             {:on-click #(swap! collapsed? not)}
             (:title question)
             [:div.is-hidden-desktop
              [icon "roll-down"
               :class (when @collapsed? "collapsed")]]]
            [:div.faq__question__answer
             {:class (when @collapsed? "collapsed")}
             (:answer question)]])))))

(defn intro []
  [:div.faq__intro
   [:h2.subtitle "If you can’t find what you’re looking for here, just drop us an email"]
   [:a {:href "mailto:hello@works-hub.com"
        :target "_blank"
        :rel "noopener"}
    [:button.button.button--inverted
     "hello@works-hub.com"]]
   [:div.faq__terms
    [link "Terms of Service" :terms-of-service :class "a--underlined"]]])

(defn faq-component
  [questions & [key]]
  [:div.faq
   [:div.faq__column.is-hidden-mobile
    [intro]
    (doall
     (for [i (map (comp inc (partial * 2)) (range (/ (count questions) 2)))]
       ^{:key (str key "question" i)}
       [question i (nth-question questions i)]))]
   [:div.faq__column.is-hidden-mobile
    (doall
     (for [i (map (partial * 2) (range (/ (count questions) 2)))]
       ^{:key (str key "question" i)}
       [question i (nth-question questions i)]))]
   [:div.faq__column.is-hidden-desktop
    [intro]
    (doall
     (for [i (range (count questions))]
       ^{:key (str key "question" i)}
       [question i (nth-question questions i)]))]])
