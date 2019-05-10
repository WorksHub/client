(ns wh.admin.create-offer.views
  (:require
    [re-frame.core :refer [dispatch]]
    [reagent.core :as r]
    [wh.admin.companies.views :as companies]
    [wh.admin.create-offer.db :as create-offer]
    [wh.admin.create-offer.events :as events]
    [wh.admin.create-offer.subs :as subs]
    [wh.components.forms.views :refer [text-field select-field]]
    [wh.components.icons :refer [icon]]
    [wh.db :as db]
    [wh.routes :as routes]
    [wh.subs :refer [<sub error-sub-key]]))

(defn field
  [k & {:as opts}]
  (let [{:keys [disabled? label error]} opts
        {:keys [message show-error?]} (when-not (false? error) (<sub [(error-sub-key k)]))]
    (merge {:value     (<sub [(keyword "wh.admin.create-offer.subs" (name k))])
            :id        (db/key->id k)
            :label     (when label [:span label])
            :error     message
            :read-only disabled?
            :validate  (get-in create-offer/fields [k :validate])
            :dirty?    (when show-error? true)
            :on-change [(keyword "wh.admin.create-offer.events" (str "edit-" (name k)))]}
           (dissoc opts :label))))

(defn company-info
  []
  (companies/company-card (<sub [::subs/company])))

(defn offer-url
  []
  (r/with-let [copied? (r/atom false)
               id (gensym)]
    [:div.create-offer__post-create-info__link
     [:textarea
      {:id id
       :read-only true
       :value (str "https://www.works-hub.com"
                   (routes/path :payment-setup :params {:step :pay-confirm} :query-params {:package "take_off"
                                                                                           :billing (name (<sub [::subs/billing-period]))}))}]
     [:a {:on-click #(do
                       (.select (.getElementById js/document id))
                       (.execCommand js/document "copy")
                       (reset! copied? true))}
      (if @copied? "Copied" "Copy")]]))

(defn select-offer
  []
  [select-field nil (field ::create-offer/offer
                           :label "* Offer"
                           :disabled (not (<sub [::subs/company]))
                           :options (<sub [::subs/offer-suggestions])
                           :on-change [::events/select-offer])])

(defn offer-details
  []
  [:div.create-offer__offer-details
   (let [edit? (<sub [::subs/show-custom-offer?])]
     [:div.columns
      [:div.column
       [text-field nil (field ::create-offer/offer-fixed
                              :type :number
                              :disabled (not edit?)
                              :label "Monthly fee ($)")]]
      [:div.column
       [text-field nil (field ::create-offer/offer-percentage
                              :type :number
                              :disabled (not edit?)
                              :label "Placement percentage (%)")]]])
   (when (zero? (<sub [::subs/offer-fixed]))
     [:p.create-offer__offer-details__zero-fee
      "A zero ($0) fee offer will automatically transition the company to the Take-Off package. Please ensure the company representative has agreed to these terms."])])

(defn submit-button
  []
  [:div
   [:button.button.button--medium
    {:class (when (<sub [::subs/creating?]) "button--inverted button--loading")}
    "Create new offer"]])

(defn post-create-info
  []
  (let [offer-fixed (<sub [::subs/offer-fixed])]
    [:div.create-offer__post-create-info
     [:div
      [:h1 "Success"]
      [:p (if-not (zero? offer-fixed)
            "The offer has been created. Send the client to the following link:"
            "You do not need to do anything else. The company has been transitioned to Take-Off package.")]
      (when-not (zero? offer-fixed)
        [offer-url])]]))

(defn pending-offer
  [offer]
  [:div.create-offer__existing-offer
   [:h2 "An outstanding offer already exists"]
   [:p "There is a pending offer for this company. Creating a new offer will invalidate and override the existing offer:"]
   [:div [:strong "Recurring monthly fee: "] (str "$" (:recurring-fee offer))]
   [:div [:strong "Placement percentage: "] (str (:placement-percentage offer) "%")]
   [:div [:strong "Client URL: "] [offer-url]]])

(defn existing-offer
  [offer]
  [:div.create-offer__existing-offer
   [:h2 "An offer was already accepted"]
   [:p "This company have already accepted an offer."]
   [:div [:strong "Recurring monthly fee: "] (str "$" (:recurring-fee offer))]
   [:div [:strong "Placement percentage: "] (str (:placement-percentage offer) "%")]
   [:div [:strong "Accepted at: "] (:accepted-at (<sub [::subs/existing-offer]))]])

(defn page []
  [:div.main-container
   [:div.main
    [:div.create-offer.wh-formx-page-container
     [:h1 "Create Take-Off offer"]
     [:form.wh-formx.wh-formx__layout
      {:on-submit #(do
                     (dispatch [::events/create-offer])
                     (.preventDefault %))}
      [company-info]
      [:div
       (when-let [offer (<sub [::subs/pending-offer])]
         [pending-offer offer])
       (when-let [offer (<sub [::subs/existing-offer])]
         [existing-offer offer])
       [select-offer]
       [offer-details]
       [submit-button]
       (when (<sub [::subs/success?])
         [post-create-info])]]]]])
