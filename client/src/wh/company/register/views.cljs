(ns wh.company.register.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.common.data :refer [package-data]]
            [wh.company.register.db :as register]
            [wh.company.register.events :as events]
            [wh.company.register.subs :as subs]
            [wh.components.common :refer [companies-section]]
            [wh.components.forms.views :as f]
            [wh.components.icons :refer [icon]]
            [wh.components.loader :refer [loader]]
            [wh.db :as db]
            [wh.subs :refer [<sub error-sub-key]]))

(defmulti next-button identity)

(defmethod next-button
  :company-details
  [_]
  (let [loading? (<sub [::subs/loading?])]
    [:button.button.button--small.company-signup__next-button
     {:class (str "is-full-width" (if loading? " button--loading button--inverted"))
      :id "company-signup__content--company-details-next"
      :on-click #(when-not loading? (dispatch [::events/next]))}
     (when-not loading? "Next")
     [:img {:src "/arrow-right.svg"}]]))

(defmethod next-button
  :job-details
  [_]
  (let [loading? (<sub [::subs/loading?])]
    [:button.button.button--small.company-signup__next-button
     {:class (str "is-full-width" (if loading? " button--loading button--inverted"))
      :id "company-signup__content--company-details-next"
      :on-click #(dispatch [::events/next])}
     (when-not loading? "I'm all done")]))

(defmethod next-button
  :complete
  [_])

(defn form-row [{:keys [label key input spec suggestions]}
                {:keys [force-dirty? disabled?]}]
  (let [common-opts (merge
                      {:id (db/key->id key)
                       :label label
                       :error (<sub [(error-sub-key key)])
                       :read-only disabled?
                       :validate spec
                       :dirty? (when force-dirty? true)
                       :on-blur #(dispatch [::events/check key])}
                      (when suggestions
                        {:auto-complete "nope"}))]
    (if (= :tags input)
      [f/tags-field
       (<sub [::subs/tag-search])
       (merge common-opts
              {:collapsed? (<sub [::subs/tags-collapsed?])
               :placeholder "Type to search skills that best match your job description"
               :tags (<sub [::subs/matching-tags])
               :on-change [::events/set-tag-search]
               :on-toggle-collapse #(dispatch [::events/toggle-tags-collapsed])
               :on-tag-click #(dispatch [::events/toggle-tag %])})]
      [f/text-field
       (<sub [key])
       (merge common-opts
              {:type input
               :on-change [key]
               :on-blur #(dispatch [::events/check key])}
              (when suggestions
                (let [[suggestions-sub suggestions-event] suggestions]
                  {:suggestions (<sub [suggestions-sub])
                   :on-select-suggestion [suggestions-event]})))])))

(defmulti company-signup-step-content identity)

(defmethod company-signup-step-content
  :company-details
  [_]
  (let [loading? (<sub [::subs/loading?])]
    [:form.form.wh-formx.wh-formx__layout
     {:on-submit #(.preventDefault %)}
     (let [company-form-checked? (<sub [::subs/company-signup-form-checked?])]
       (for [{key :key :as field-opts} register/company-fields-maps]
         ^{:key (str key)}
         [form-row field-opts {:force-dirty? company-form-checked? :disabled? loading?}]))]))

(defmethod company-signup-step-content
  :job-details
  [_]
  (let [loading? (<sub [::subs/loading?])]
    [:form.form.wh-formx.wh-formx__layout
     {:on-submit #(.preventDefault %)}
     (let [job-form-checked? (<sub [::subs/job-form-checked?])]
       (for [{key :key :as field-opts} register/job-fields-maps]
         ^{:key (str key)}
         [form-row field-opts {:force-dirty? job-form-checked? :disabled? loading?}]))]))

(defmethod company-signup-step-content
  :complete
  [_]
  [:div
   [loader]
   [:h2 "Please wait a moment whilst we generate your dashboard..."]])

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
  (let [step (or (<sub [::subs/company-signup-step]) :company-details)
        current-class? #(when (= step %) {:class "company-signup__steps--current"})
        complete? (= step :complete)
        error (<sub [::subs/error])]
    [:div.main-container
     [:div.main
      [:div.company-signup
       [:div.company-signup__steps.is-hidden-mobile
        [:span (current-class? :company-details) "1. Company Details"]
        [:span ">"]
        [:span (current-class? :job-details)     "2. Job Details"]
        [:span ">"]
        [:span (current-class? :complete)     "3. Done"]]
       [:div.company-signup__steps.is-hidden-desktop
        [:span.company-signup__steps--current
         (case step
           :company-details "Step 1 of 2 - Company Details"
           :job-details "Step 2 of 2 - Job Details"
           :complete "Complete"
           "???")]]
       [:div.company-signup__content-container
        {:class (str "company-signup__content-container--step-" (name step))}
        (if complete?
          [:div.columns
           [:div.column.company-signup__content.is-12
            [company-signup-step-content step]
            (when error
              (f/error-component error {:class "is-hidden-mobile is-pulled-left" :id "company-signup-error-desktop"}))
            [next-button step]]]
          [:div.columns
           [:div.column.company-signup__content.is-8
            [company-signup-step-content step]
            (when error
              (f/error-component error {:class "is-hidden-mobile is-pulled-left" :id "company-signup-error-desktop"}))
            [next-button step]]
           [:div.column.company-signup__pods.is-4
            [benefits-pod]]])]]]
     [:div.company-signup-work-with
      (companies-section "Hire Engineers From")]]))
