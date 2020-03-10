(ns wh.components.newsletter
  (:require
    #?(:cljs [reagent.core :as r])
    [wh.interop :as interop]
    [wh.util :as util]))

(defn newsletter
  [user-logged-in?]
  (let [render (fn []
                 (when-not user-logged-in?
                   [:section
                    {:class (util/merge-classes "pod"
                              "pod--no-shadow"
                              "newsletter-subscription")}
                    [:div [:h3.newsletter-subscription__title "Join our newsletter!"]
                     [:p.newsletter-subscription__description "Join over 111,000 others and get access to exclusive content, job opportunities and more!"]
                     [:form#newsletter-subscription.newsletter-subscription__form
                      [:div.newsletter-subscription__input-wrapper
                       [:label.newsletter-subscription__label {:for "email"} "Your email"]
                       [:input.newsletter-subscription__input {:type "email" :name "email" :placeholder "turing@machine.com" :id "email"}]]
                      [:button.button.newsletter-subscription__button "Subscribe"]]
                     [:div.newsletter-subscription__success.is-hidden
                      [:div.newsletter-subscription__primary-text "Thanks! " [:br.is-hidden-desktop] "See you soon!"]]]]))]
    #?(:cljs (r/create-class
               {:component-did-mount (interop/listen-newsletter-form)
                :reagent-render render})
       :clj  (some-> (render)
                     (conj [:script (interop/listen-newsletter-form)])))))
