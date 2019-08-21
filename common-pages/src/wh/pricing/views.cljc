(ns wh.pricing.views
  (:require
    #?(:cljs [re-frame.core :refer [dispatch-sync]])
    #?(:cljs [reagent.core :as r])
    #?(:cljs [wh.subs :refer [<sub] :as core-subs])
    [wh.common.data :as data :refer [package-data]]
    [wh.components.common :refer [link]]
    [wh.components.faq :as faq]
    [wh.components.icons :refer [icon]]
    [wh.components.package-selector :refer [package-selector]]
    [wh.components.www-homepage :as www]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]
    [wh.verticals :as verticals]))

(defn demo-button []
  (fn [secondary? label package _billing-period]
    [:a {:href   (if (= :take_off package)
                   verticals/take-off-meeting-link
                   verticals/demo-link)
         :target "_blank"
         :rel    "noopener"}
     [:button#employers_demo-btn
      {:class (util/merge-classes "button"
                                  (when secondary? "button--inverted"))}
      label]]))

(defn signup-button [label package billing-period]
  [:a
   {:href (routes/path :register-company
                       :query-params {:package (name package)
                                      :billing (name billing-period)})
    :id (str "employers_signup-btn-" label
             (when package (str "-" (name package)))
             (when billing-period (str "-" (name billing-period))))}
   (if (#{:essential :launch_pad} package)
     [:button.button "Get Started"]
     [:button.button label])])

(defn fake-radio-buttons
  [value options]
  [:div.radios
   (for [[i {:keys [id label href]}] (map-indexed vector options)]
     ^{:key id}
     [:a
      {:class
       (util/merge-classes "radio"
                           (when (= id value)
                             "radio--checked"))
       :href href
       :id id}
      (when (= id value)
        [:div.radio__checked])
      [:div.radio__label label]])])

(defn page []
  (let [vertical (<sub [:wh/vertical])
        billing-period (keyword (or (<sub [:wh/query-param "billing-period"]) (name data/default-billing-period)))]
    [:div.pricing
     [:div.pricing-content
      [:h2.pricing__subtitle "STRAIGHTFORWARD PRICING"]
      [:div.pricing__billing-period-selector-wrapper
       [:h1.pricing__title "Select a plan for your hiring needs"]
       [:div.pricing__billing-period-selector
        [fake-radio-buttons
         billing-period
         (->> data/billing-data
              (filter (fn [[k v]] (contains? data/billing-periods k)))
              (map (fn [[k v]] {:id k
                                :label [:span (:title v)
                                        (when-let [discount (:discount v)]
                                          [:small (str "(" (* 100 discount) "% off)")])]
                                :href (routes/path :pricing :query-params {:billing-period (name k)})})))]]]
      [package-selector
       {:signup-button signup-button
        :show-billing-period-selector? false
        :billing-period billing-period
        :mobile-fullscreen? true
        :contact-button (demo-button)}]]
     [:div.pricing-content.has-text-centered
      [:div.pricing__request-demo
       [(demo-button) false "Request Demo" nil nil]]]
     [www/animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket"]
     [:div.pricing-content
      [:h2.pricing__subtitle "FAQS"]
      [:h1.pricing__title "What else would you like to know?"]
      [faq/faq-component data/pricing-questions]]
     [www/animated-hr "/images/homepage/globe.svg" "homepage__animated-hr__globe"]
     [:div.pricing-content.has-text-centered
      [www/testimonials]]]))
