(ns wh.company.register.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.company.register.db :as register]
            [wh.company.register.events :as events]
            [wh.company.register.subs :as subs]
            [wh.components.common :refer [companies-section]]
            [wh.components.forms.views :as f]
            [wh.components.icons :refer [icon]]
            [wh.db :as db]
            [wh.routes :as routes]
            [wh.styles.register :as styles]
            [wh.subs :refer [<sub error-sub-key]]
            [wh.util :as util]))

(defn register-button []
  (let [loading? (<sub [::subs/loading?])]
    [:button.button.button--small.company-signup__next-button
     {:class     (str "is-full-width" (if loading? " button--loading button--inverted"))
      :id        "company-signup__content--company-details-next"
      :data-test "signup-company"
      :on-click  #(when-not loading? (dispatch [::events/register]))}
     (when-not loading? "Register")]))

(defn form-row [{:keys [label key input spec suggestions]}
                {:keys [force-dirty? disabled?]}]
  (let [common-opts (merge
                      {:id        (db/key->id key)
                       :data-test (name key)
                       :label     label
                       :error     (<sub [(error-sub-key key)])
                       :read-only disabled?
                       :validate  spec
                       :dirty?    (when force-dirty? true)
                       :on-blur   #(dispatch [::events/check key])}
                      (when suggestions
                        {:auto-complete "nope"}))]
    [f/text-field
     (<sub [key])
     (merge common-opts
            {:type      input
             :on-change [key]
             :on-blur   #(dispatch [::events/check key])}
            (when suggestions
              (let [[suggestions-sub suggestions-event] suggestions]
                {:suggestions          (<sub [suggestions-sub])
                 :on-select-suggestion [suggestions-event]})))]))


(defn company-signup-step-content []
  (let [loading? (<sub [::subs/loading?])]
    [:form.form.wh-formx.wh-formx__layout
     {:on-submit #(.preventDefault %)
      :data-test "form-signup-company"}
     (let [company-form-checked? (<sub [::subs/company-signup-form-checked?])]
       (for [{key :key :as field-opts} register/company-fields-maps]
         ^{:key (str key)}
         [form-row field-opts
          {:force-dirty? company-form-checked? :disabled? loading?}]))

     [:label#consent-label.is-flex {:for "consent"}
      [:div {:class "checkbox__box"}]
      [:span
       "By submitting this form, you agree to opt-in to the "
       [:a.a--underlined {:href   "/privacy-policy"
                          :target "_blank"
                          :rel    "noopener"}
        "privacy policy"] " of this website and the processing of your data."]]]))

(defn benefits-pod
  []
  [:div.pod.company-signup__benefit-pod
   [:h2 "Welcome"]
   [:p "We focus on helping your company build a high-performing engineering team. Immediately matching your opportunities to a global network of domain experts."]
   [:ul
    [:li [:div [:img {:src "/images/employers/icons/trophy.svg"
                      :alt "Trophy icon"}]] "Olympiad winners"]
    [:li [:div [:img {:src "/images/employers/icons/blockchain.svg"
                      :alt "Blockchain icon"}]] "Blockchain builders"]
    [:li [:div [:img {:src "/images/employers/icons/programming.svg"
                      :alt "Programming icon"}]] "Functional Programming language creators"]
    [:li [:div [:img {:src "/images/employers/icons/machine.svg"
                      :alt "Machine icon"}]]
     [:span "Machine learning, Computer vision and NLP experts"]]
    [:li [:div [:img {:src "/images/employers/icons/js.svg"
                      :alt "JS icon"}]] "Javascript framework builders"]]])

(defn page
  []
  (let [error (<sub [::subs/error])]
    [:div.main-container
     [:div.main
      [:div.company-signup
       [:div.company-signup__steps
        [:span.company-signup__steps--current "Sign Up"]
        [:a {:href  (routes/path :register)
             :class (util/mc "a--underlined" styles/create-profile)}
         [:span "Create personal profile"]
         [icon "arrow-right" :class styles/arrow]]]
       [:div.company-signup__content-container
        [:div.columns
         [:div.column.company-signup__content.is-8
          [company-signup-step-content]
          (when error
            (f/error-component-outdated
              error {:class "is-hidden-mobile is-pulled-left"
                     :id    "company-signup-error-desktop"}))
          [register-button]]
         [:div.column.company-signup__pods.is-4
          [benefits-pod]]]]]]
     [:div.company-signup-work-with
      (companies-section "Hire Engineers From")]]))
