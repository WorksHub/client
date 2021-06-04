(ns wh.components.newsletter.core
  (:require
    #?(:cljs [reagent.core :as r])
    [wh.components.newsletter.styles :as styles]
    [wh.interop :as interop]
    [wh.util :as util]))

(def container-id-for-script "newsletter-subscription-form")
(def container-id-for-script* "newsletter-subscription-success")
(def global-class-to-hide-elements "is-hidden")

(defn newsletter [{:keys [logged-in? type]
                   :or   {type :listing}}]
  (let [render (fn []
                 (when-not logged-in?
                   [:div (util/smc styles/card__wrapper
                                   [(= type :blog-list) styles/card__wrapper--blog-list]
                                   [(= type :blog-content) styles/card__wrapper--blog-content]
                                   [(= type :job-list) styles/card__wrapper--job-list])
                    [:form
                     {:class styles/card
                      :id    container-id-for-script}
                     [:div {:class styles/title} "Join our newsletter"]
                     [:div {:class styles/text} "Join over 111,000 others and get access to exclusive content, job opportunities and more!"]
                     [:div {:class styles/input__wrapper}
                      [:div
                       [:label.visually-hidden {:for "email"} "your email address"]
                       [:input {:class styles/input :type "email" :name "email" :placeholder "email@address.com" :id "email"}]]
                      [:button {:class styles/button} "Subscribe"]]]
                    [:div
                     {:class (util/mc styles/success global-class-to-hide-elements)
                      :id    container-id-for-script*}
                     [:div {:class styles/success__text} "Thanks for signing up. " [:br.is-hidden-desktop] "We'll see you soon \uD83D\uDC4B"]]]))]

    #?(:cljs (r/create-class
               {:component-did-mount (interop/listen-newsletter-form)
                :reagent-render      render})
       :clj  (some-> (render)
                     (conj [:script (interop/listen-newsletter-form)])))))
