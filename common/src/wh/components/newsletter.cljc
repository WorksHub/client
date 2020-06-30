(ns wh.components.newsletter
  (:require
    #?(:cljs [reagent.core :as r])
    [wh.interop :as interop]
    [wh.util :as util]))

(defn newsletter [{:keys [logged-in? type]
                   :or {type :listing}}]
   (let [render (fn []
                  (when-not logged-in?
                    [:section
                     {:class (util/merge-classes
                               "pod"
                               "pod--no-shadow"
                               "newsletter-subscription"
                               (when (= type :landing) "newsletter-subscription--landing"))}
                     [:div [:h3.newsletter-subscription__title "Join our newsletter!"]
                      [:p.newsletter-subscription__description "Join over 111,000 others and get access to exclusive content, job opportunities and more!"]
                      [:form#newsletter-subscription.newsletter-subscription__form
                       [:div.newsletter-subscription__input-wrapper
                        [:label.newsletter-subscription__label {:for "email"} "your email address"]
                        [:input.newsletter-subscription__input {:type "email" :name "email" :placeholder "email@address.com" :id "email"}]]
                       [:button.button.newsletter-subscription__button "Subscribe"]]
                      [:div.newsletter-subscription__success.is-hidden
                       [:div.newsletter-subscription__primary-text "Thanks for signing up. " [:br.is-hidden-desktop] "We'll see you soon"]]]]))]
     #?(:cljs (r/create-class
                {:component-did-mount (interop/listen-newsletter-form)
                 :reagent-render render})
        :clj  (some-> (render)
                      (conj [:script (interop/listen-newsletter-form)])))))
