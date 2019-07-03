(ns wh.company.payment.views
  (:require
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [cljsjs.react-stripe-elements]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as r]
    [wh.common.cases :as cases]
    [wh.common.cost :as cost]
    [wh.common.data :as data]
    [wh.common.re-frame-helpers :as rf-helpers]
    [wh.common.text :refer [pluralize]]
    [wh.company.payment.db :as payment]
    [wh.company.payment.events :as events]
    [wh.company.payment.subs :as subs]
    [wh.company.views :refer [int->dollars]]
    [wh.components.common :refer [companies-section link]]
    [wh.components.forms.views :as f :refer [text-field select-field]]
    [wh.components.icons :refer [icon]]
    [wh.components.navbar :as nav-common]
    [wh.components.package-selector :refer [package-selector]]
    [wh.db :as db]
    [wh.subs :refer [<sub] :as core-subs]
    [wh.util :refer [list->sentence]]
    [wh.verticals :as verticals]))

(defonce stripe-form-contexts (r/atom {}))
(defonce stripe-loaded? (r/atom false))

;; Stripe elements
(def Elements (r/adapt-react-class js/ReactStripeElements.Elements))
(def CardElement (r/adapt-react-class js/ReactStripeElements.CardElement))
(def StripeProvider (r/adapt-react-class js/ReactStripeElements.StripeProvider))

(def element-style {:base {:color "#6E7F89"
                           :textTransform "capitalize"
                           :letterSpacing "0.025em"
                           :fontSize "12px"
                           :height "15px"
                           :fontFamily "'Montserrat', sans-serif"
                           "::placeholder" {:color "#adadad"}}
                    :invalid {} #_{:color "#c4000f"}})

(defn submit-stripe-form!
  [id end-event]
  ;; make sure there aren't any errors before submitting
  (if-let [this (get @stripe-form-contexts id)]
    (when (and this (not (<sub [::subs/stripe-card-form-error])))
      (dispatch-sync [::events/set-stripe-card-form-enabled false])
      (.then (this.props.stripe.createToken)
             (fn [payload]
               (let [payload (cases/->kebab-case (js->clj payload))]
                 (if-let [e (:error payload)]
                   (do
                     (swap! (r/state-atom this) assoc-in [:errors :card] e)
                     (dispatch [::events/set-stripe-card-form-error (:message e)])
                     (dispatch [::events/set-stripe-card-form-enabled true]))
                   (do
                     (dispatch [::events/save-token (get-in payload [:token :id])])
                     (dispatch end-event)))))))
    (js/console.error "Couldn't find Stripe form with ID" id)))

(defn StripeForm
  [id end-event]
  (r/adapt-react-class
   (js/ReactStripeElements.injectStripe
    (r/reactify-component
     (r/create-class {:display-name "stripe-reagent-form"
                      :component-did-update
                      (fn [this]
                        (swap! stripe-form-contexts assoc id this))
                      :component-did-mount
                      (fn [this]
                        (swap! stripe-form-contexts assoc id this))
                      :render
                      (fn [this]
                        (let [element-on-change (fn [e]
                                                  (let [e (js->clj e :keywordize-keys true)
                                                        error (:error e)]
                                                    (dispatch [::events/set-stripe-card-form-error (:message error)])
                                                    (swap! (r/state-atom this) assoc-in [:errors (keyword (:elementType e))] error)))]
                          [:form {:id "stripe-card-form"
                                  :on-submit (fn [e]
                                               (.preventDefault e)
                                               (submit-stripe-form! id end-event))
                                  :class "StripeForm"}
                           [CardElement {:style element-style
                                         :on-change element-on-change}]
                           [:div.error
                            @(r/cursor (r/state-atom this) [:errors :card :message])]]))})))))

(defn card-terms
  [package]
  [:p.payment__card-form__terms
   [:i "By authorising payment, you are agreeing to WorksHub standard "]
   [:a {:href   "/terms-of-service"
        :target "_blank"
        :rel    "noopener"} "Terms of Service."]
   (when (= :take_off package)
     [:i " Any hiring fees will be invoiced separately in line with pre-agreed terms."])])

(defn coupon-field
  [enabled? apply-event]
  (let [loading? (<sub [::subs/coupon-loading?])]
    [:div.payment__coupon-form__field
     [:form.wh-formx.wh-formx__layout
      {:on-submit #(.preventDefault %)}
      [text-field nil {:id "coupon-form-input"
                       :on-change [::events/set-coupon-code]
                       :value (<sub [::subs/coupon-code])
                       :error (<sub [::subs/coupon-error])
                       :on-focus #(dispatch [::events/reset-coupon-error])
                       :read-only (or (not enabled?) loading?)}]
      [:button.button.button--inverted
       {:id "coupon-form-apply-button"
        :class (when loading? "button--loading")
        :disabled (not enabled?)
        :on-click #(when (and enabled? (not loading?))
                     (dispatch apply-event)
                     (.preventDefault %))}
       "Apply"]]]))

(defn coupon-form
  []
  (let [expanded? (r/atom false)
        code (r/atom "")]
    (fn []
      [:div.payment__coupon-form
       {:class (when @expanded? "payment__coupon-form--expanded")}
       [:div.is-flex
        {:id "coupon-form-expand"
         :on-click #(swap! expanded? not)}
        [:span.is-not-selectable "Do you have a promotional code?"]
        [:div.payment__coupon-form__roll-down
         {:class (when @expanded? "payment__coupon-form__roll-down--expanded")}
         [icon "roll-down"]]]
       [coupon-field
        (<sub [::subs/stripe-card-form-enabled?])
        [::events/apply-coupon]]])))

(defn card-form
  [opts]
  (fn [{:keys [id terms? event package
               coupon?]}]
    [:div.payment__card-form
     (when @stripe-loaded?
       (when-let [spk (<sub [::subs/stripe-public-key])]
         [StripeProvider {:apiKey spk}
          [Elements [(StripeForm id event)]]]))
     (when coupon?
       [coupon-form])
     (when terms?
       (card-terms package))]))

(defn authorize-card-button
  [_]
  (let [enabled? (<sub [::subs/stripe-card-form-enabled?])
        can-proceed? (<sub [::subs/can-press-authorize?])]
    [:div.button-container
     [:button.button.is-full-width
      {:id "authorize-card-button"
       :class (when-not enabled? "button--inverted button--loading")
       :disabled (or (not can-proceed?) (<sub [::subs/stripe-card-form-error]))
       :on-click #(when (and enabled? can-proceed?)
                    (when-let [el (.getElementById js/document "stripe-card-form")]
                      (submit-stripe-form! :payment-setup-form [::events/setup-step-forward])))}
      "Authorize Card"]]))

(defmulti payment-setup-step-content identity)

(defn select-package-button
  [label package billing-period]
  (let [upgrading? (<sub [::subs/upgrading?])]
    [:button.button
     {:id (str "select-package-btn-" (name package) "-" (name billing-period))
      :on-click #(dispatch [::events/setup-step-forward (merge {:package package}
                                                               (when-not upgrading?
                                                                 {:billing-period billing-period}))])}
     (if upgrading?
       "Upgrade & Pay"
       label)]))

(defn fake-demo-button
  [secondary? label package billing-period]
  (let [upgrading? (<sub [::subs/upgrading?])
        take-off-offer (<sub [::subs/company-new-offer])]
    (if (and (= :take_off package)
             take-off-offer)
      [:button.button
       {:id (str "select-package-btn-" (name package) "-" (name billing-period))
        :on-click #(dispatch [::events/setup-step-forward (merge {:package package}
                                                                 (when-not upgrading?
                                                                   {:billing-period billing-period}))])}
       "View Offer"]
      [:button.button
       {:class (when secondary? "button--inverted")
        :id (str "select-package-btn-take_off-" (name billing-period))
        :on-click #(dispatch [::events/setup-step-forward {:package package :billing-period billing-period}])}
       label])))

(defn restricted-packages
  []
  (let [action                (<sub [::subs/action])
        package               (<sub [::subs/company-package])
        missing-publish?      (and (= :publish action) (not (<sub [::subs/has-permission? :can_publish])))
        missing-integrations? (and (= :integrations action) (not (<sub [::subs/has-permission? :can_use_integrations])))
        missing-applications? (and (= :applications action) (not (<sub [::subs/has-permission? :can_see_applications])))
        can-start-trial?      (<sub [::subs/has-permission? :can_start_free_trial])]
    (cond
      (and (= :take_off package)
           (<sub [::subs/company-new-offer])) [:free :essential :launch_pad]
      (= :take_off package)                   [:free :essential :launch_pad :take_off]
      (= :launch_pad package)                 [:free :essential :launch_pad]
      (and (#{:free :essential} package)
           missing-publish?)                  [:free :essential]
      (and (#{:free :essential} package)
           missing-integrations?)             [:free :essential]
      (and (#{:free :essential} package)
           missing-applications?)             (if (= :free package) [:free] [:free :essential])
      (= :essential package)                  [:free :essential]
      (= :free package)                       [:free]
      (not can-start-trial?)                  [:free]
      missing-applications?                   [:free]
      :else                                   nil)))

(defmethod payment-setup-step-content
  :select-package
  [_]
  (let [upgrading? (<sub [::subs/upgrading?])
        package (<sub [::subs/company-package])]
    [:div
     [package-selector
      (merge {:signup-button select-package-button
              :contact-button fake-demo-button
              :mobile-fullscreen? true
              :show-billing-period-selector? (not upgrading?)
              :restrict-packages (restricted-packages)
              :coupon (<sub [::subs/company-coupon])
              :info-message (<sub [::subs/info-message])
              :current-package (when (and (not= :take_off package)
                                          (<sub [::subs/company-new-offer])) ;; if we have pending offer, even if already take_off we want to show it
                                 package)}
             (when upgrading?
               {:billing-period  (<sub [::subs/company-billing-period])}))]]))

(defn offer-details
  [{:keys [recurring-fee placement-percentage]} show-billing-period-selector?]
  (let [{:keys [discount number]} (get data/billing-data (or (<sub [::subs/billing-period])
                                                             (<sub [::subs/company-billing-period])))
        cpm (- recurring-fee (* recurring-fee discount))]
    [:div.payment-setup__offer-details
     [:p "We have prepared an offer for you. Please check the terms being proposed in the offer before providing payment information."]
     (when show-billing-period-selector?
       [:div
        [:h3 "How often would you like to be billed?"]
        [:form.wh-formx.wh-formx__layout
         [select-field (<sub [::subs/billing-period])
          {:options (<sub [::subs/offer-billing-selection-options])
           :on-change [::events/set-billing-period]}]]])
     [:div [:strong "Monthly fee: "] (int->dollars cpm)
      (when discount [:i (str " (" (* 100 discount) "% off)")])]
     [:div [:strong "Placement percentage: "] (str placement-percentage "%")]
     [:div [:strong "Billed Amount: "] (int->dollars (* cpm number))]]))

(defn period->time
  ([t]
   (tf/unparse (tf/formatter "DD MMM YYYY") (tf/parse (tf/formatters :date-time) t)))
  ([t plus-bp]
   (let [time (tf/parse (tf/formatters :date-time) t)]
     (tf/unparse (tf/formatter "DD MMM YYYY") (t/plus time (t/months (get-in data/billing-data [plus-bp :number])))))))

(defn fallback-period-time
  [bp]
  (tf/unparse (tf/formatter "DD MMM YYYY") (t/plus (t/now) (t/months (get-in data/billing-data [bp :number])))))

(defn upgrading-calculator
  [package billing-period]
  (let [new-offer (<sub [::subs/company-new-offer])
        has-details? (<sub [::subs/has-saved-card-details?])
        package (<sub [::subs/package])
        {:keys [cost]} (<sub [::subs/current-package-data])
        {:keys [discount number description] :as bp} (get data/billing-data billing-period)
        next-invoice (<sub [::subs/next-invoice])
        coupon (or (<sub [::subs/current-coupon])
                   (:coupon next-invoice)
                   #_(<sub [::subs/company-coupon])) ;; TODO shall we remove this field entirely? Do we even want company coupons here???
        latent-cost (* number (cost/calculate-monthly-cost cost discount coupon))
        estimate (<sub [::subs/estimate])
        pro-rated-estimates (filter :proration estimate)
        next-charge (some #(when-not (:proration %) %) estimate)
        first-charge (if (and next-invoice coupon)
                       ;; TODO explain this
                       (* number (cost/calculate-monthly-cost cost discount (assoc coupon :duration :forever)))
                       latent-cost)
        breakdown? (<sub [::subs/breakdown?])]
    [:div.payment-setup__upgrading-calculator
     {:class (rf-helpers/merge-classes
               (when breakdown? "payment-setup__upgrading-calculator--breakdown")
               (when-not estimate "payment-setup__upgrading-calculator--loading"))}
     (if estimate
       [:div
        (when (and (= :take_off package)
                   new-offer)
          [offer-details new-offer (not has-details?)])
        (when (and breakdown? (not-empty pro-rated-estimates))
          [:p "Here is a breakdown of your charges:"])
        (when breakdown?
          (for [est pro-rated-estimates]
            [:div.payment-setup__upgrading-calculator__estimate.is-flex
             {:key (:description est)}
             [:span (:description est)]
             [:span (int->dollars (:amount est) {:cents? true})]]))
        [:hr ]
        (when breakdown?
          [:div.is-flex
           [:strong "Initial charge, debited immediately"]
           [:strong (if (and new-offer (not has-details?))
                      (int->dollars latent-cost)
                      (int->dollars (max 0 (->> estimate
                                                (filter :proration)
                                                (map :amount)
                                                (reduce +))) {:cents? true}))]])
        (when coupon
          [:div.is-flex
           [:i "An active discount will be applied (" (name (:duration coupon)) "): " (:description coupon)]])
        [:div.is-flex
         [:i "The next charge will be " (int->dollars first-charge) ", on "
          (if next-charge
            (some-> (get-in next-charge [:period :start]) (period->time))
            (fallback-period-time billing-period))
          ". A subsequent charge will "
          (when (not= latent-cost first-charge)
            (str "be " (int->dollars latent-cost) " and ")) "recur "
          (if (= 1 number)
            "each month"
            (str "every " number " " (pluralize number "month" )))
          "."]]]
       [:div.is-loading-spinner])]))

(defn initial-payment-details
  [package billing-period]
  (let [{:keys [cost]} (<sub [::subs/current-package-data])
        {:keys [discount number description] :as bp} (get data/billing-data billing-period)
        date (t/plus (t/now) (t/months number))
        date-str (tf/unparse (tf/formatter "d MMM Y") date)
        coupon (<sub [::subs/current-coupon])
        monthly-cost (cost/calculate-monthly-cost cost discount coupon)
        {:keys [before-coupon after-coupon]} (cost/calculate-initial-payment number monthly-cost coupon)]
    [:div
     [:p.li [icon "cutout-tick"] "the cost is " (int->dollars monthly-cost) " per month"]
     [:p.li [icon "cutout-tick"] "you will be " (or description "billed monthly")]
     (if after-coupon
       [:p.li [icon "cutout-tick"] "the first payment will be taken immediately (" (int->dollars after-coupon) " = " (int->dollars before-coupon) " − " (int->dollars (- before-coupon after-coupon)) " promotion)"]
       [:p.li [icon "cutout-tick"] "the first payment will be taken immediately (" (int->dollars before-coupon) ")"])
     [:p.li [icon "cutout-tick"] "the next payment will be taken " date-str]
     (when coupon
       [:p.li.coupon [icon "cutout-tick"] "promotion applied: " (str/lower-case (:description coupon)) ""])]))

(defmulti pay-confirm-content identity)

(defmethod pay-confirm-content :default
  [_package]
  (let [new-card-when-upgrading? (r/atom false)]
    (fn [package]
      (let [billing-period (or (<sub [::subs/billing-period])
                               (<sub [::subs/company-billing-period]))
            {:keys [name]} (get data/package-data package)
            {:keys [title] :as bp} (get data/billing-data billing-period)
            upgrading? (<sub [::subs/upgrading?])
            has-details? (<sub [::subs/has-saved-card-details?])
            enabled? (<sub [::subs/stripe-card-form-enabled?])
            can-proceed? (<sub [::subs/can-press-authorize?])
            existing-billing-period (some->> (<sub [::subs/existing-billing-period])
                                             (get data/billing-data))
            breakdown? (<sub [::subs/breakdown?])]
        [:div.payment-setup__payment-details__calculator
         {:class (if breakdown? "breakdown" "no-breakdown")}
         (cond
           (= existing-billing-period bp)
           [:h1 (str "You are on " name " package, currently on the " (:title existing-billing-period) " plan. Please select a new billing period...")]
           existing-billing-period
           [:h1 (str "You are on " name
                     " package and you are changing from the " (:title existing-billing-period)
                     " plan to the " title " plan.")]
           :else
           [:h1 (str "You have selected the " name
                     " package on the " title " plan.")])
         (if upgrading?
           [upgrading-calculator package billing-period]
           [initial-payment-details package billing-period])
         [:hr
          {:class (when upgrading? "fixed")}]
         (if (and has-details? (not @new-card-when-upgrading?))
           [:div.payment-setup__payment-details__card.payment-setup__payment-details__card--upgrading
            [:div
             [:p "Your card ending " (<sub [::subs/company-card-digits]) " will be used"]
             [:p "If you would like to use a different card or apply a discount code, " [:a {:on-click #(reset! new-card-when-upgrading? true)} "click here."]]
             (card-terms package)
             [:div.button-container
              [:button.button.is-full-width
               {:id "authorize-card-button--no-token"
                :class (when-not enabled? "button--inverted button--loading")
                :disabled (or (not can-proceed?) (<sub [::subs/stripe-card-form-error]))
                :on-click #(when (and enabled? can-proceed?)
                             (dispatch-sync [::events/set-stripe-card-form-enabled false])
                             (dispatch [::events/setup-step-forward]))}
               "Authorize Card"]]]]
           [:div.payment-setup__payment-details__card
            {:class (when upgrading? "payment-setup__payment-details__card--upgrading")}
            [:p "Please enter your payment details"]
            [card-form {:id :payment-setup-form
                        :terms? true
                        :coupon? (not upgrading?) ;; TODO this prevents coupons during upgrades
                        :package package
                        :event [::events/setup-step-forward]}]
            [authorize-card-button]])
         (when (and upgrading? existing-billing-period)
           [link [:button.button.button--inverted.is-full-width "Cancel"]
            :edit-company
            :query-params {:page :payment-details}])]))))

(defmethod pay-confirm-content :take_off
  [_]
  (let [clicked? (r/atom false)]
    (fn [_]
      (let [offer (<sub [::subs/company-new-offer])
            upgrading? (<sub [::subs/upgrading?])]
        (if upgrading?
          (pay-confirm-content :default)
          [:div
           [:h1 "You have selected the Take Off package. This is our premium service which can be tailored to your specific hiring requirements."]
           [:p "Benefits include"]
           [:p.li [icon "cutout-tick"] "tiered pricing structure"]
           [:p.li [icon "cutout-tick"] "unlimited job postings across all hubs"]
           [:p.li [icon "cutout-tick"] "all applicants are screened by WorksHub"]
           [:p.li [icon "cutout-tick"] "guaranteed hires for even the most niche tech hires"]
           (if offer
             [:div
              [offer-details offer true]
              [:div.payment-setup__payment-details__card
               {:class (when upgrading? "payment-setup__payment-details__card--upgrading")}
               [:p "Please enter your payment details"]
               [card-form {:id :payment-setup-form
                           :terms? true
                           :coupon? false
                           :package :take_off
                           :event [::events/setup-step-forward]}]
               [authorize-card-button]]]
             [:div
              [:a {:href   (verticals/config (<sub [::core-subs/vertical]) :take-off-meeting-link)
                   :target "_blank"
                   :rel    "noopener"
                   :on-click #(reset! clicked? true)}
               [:button.button.is-full-width
                {:class (when @clicked? "button--inverted")}
                "Schedule a meeting"]]
              (when @clicked?
                [link [:button.button.is-full-width
                       "Back to Dashboard"] :homepage])])])))))

(defmethod pay-confirm-content :free
  [_]
  (let [job-id (<sub [::subs/job-id])
        action (<sub [::subs/action])
        waiting? (<sub [::subs/waiting?])]
    [:div
     [:h1 "You have selected the trial package so won’t be charged anything right now!"]
     [:p "Benefits include"]
     [:p.li [icon "cutout-tick"] "unlimited published jobs"]
     [:p.li [icon "cutout-tick"] "these can go live across as many of our hubs as you like"]
     [:p.li [icon "cutout-tick"] "access to job analytics"]
     [:button.button.is-full-width
      {:on-click #(dispatch [::events/confirm-free])
       :class (when waiting? "button--inverted button--loading")
       :disabled waiting?}
      "Let's get started"]]))

(defn billing-period-by-idx
  [i]
  (nth (keys data/billing-data) i))

(defn idx-of-billing-period
  [bp]
  (.indexOf (keys data/billing-data) bp))

(defn select-billing-period
  []
  (let [mobile-view-width (r/atom 0)
        reset-mobile-view-width!
        (fn [this]
          (reset! mobile-view-width (.-offsetWidth (aget (.getElementsByClassName (r/dom-node this) "payment-setup__pay-confirm__select-billing-period") 0))))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (reset-mobile-view-width! this))
       :component-did-update
       (fn [this]
         (reset-mobile-view-width! this))
       :reagent-render
       (fn []
         (let [{:keys [cost per]}      (<sub [::subs/current-package-data])
               enabled?                (<sub [::subs/stripe-card-form-enabled?])
               billing-period          (or (<sub [::subs/billing-period])
                                           (<sub [::subs/company-billing-period]))
               existing-billing-period (<sub [::subs/existing-billing-period])
               coupon                  (<sub [::subs/company-coupon])
               mobile-view-idx         (idx-of-billing-period billing-period)
               margin-left             (* mobile-view-idx @mobile-view-width)]

           [:div
            [:div.payment-setup__pay-confirm-container.payment-setup__pay-confirm__select-billing-period.columns.is-mobile
             (for [[idx [k {:keys [title discount]}]] (->> data/billing-data
                                                           (filter (fn [[k v]] (contains? data/billing-periods k)))
                                                           (map-indexed vector))]
               (let [checked? (= k billing-period)]
                 [:div.column
                  {:key      k
                   :class    (rf-helpers/merge-classes
                               (when checked? "checked")
                               (when-not enabled? "disabled")
                               (when (= k existing-billing-period) "existing"))
                   :style    (when (zero? idx) {:margin-left (* -1 margin-left)})
                   :on-click #(when enabled? (dispatch [::events/set-billing-period k]))}
                  [:div.wh-formx.radios
                   [:input {:id        k
                            :type      :radio
                            :checked   checked?
                            :on-change #()}]
                   [:div.radio
                    [:label {:for k}]]]
                  [:span "Pay " (str/lower-case (str/replace title #" " "-"))]
                  [:span (int->dollars (cost/calculate-monthly-cost cost discount coupon)) " per " per ]
                  [:span (when discount (str "save " (* 100 discount) "%"))]]))]
            [:div.package-selector__slide-buttons.payment-setup__slide-buttons.is-hidden-desktop
             [:div.package-selector__slide-buttons-inner
              [:div
               {:class    (when-not (pos? mobile-view-idx) "hidden")
                :on-click #(dispatch [::events/set-billing-period
                                      (billing-period-by-idx (dec mobile-view-idx))])}
               [icon "circle"]
               [icon "arrow-left"]]
              [:div
               {:class    (when (>= (inc mobile-view-idx) (count data/billing-data)) "hidden")
                :on-click #(dispatch [::events/set-billing-period
                                      (billing-period-by-idx (inc mobile-view-idx))])}
               [icon "circle"]
               [icon "arrow-right"]]]]]))})))

(defmethod payment-setup-step-content
  :pay-confirm
  [_]
  (let [p (<sub [::subs/package])]
    [:div
     (when (and (<sub [::subs/upgrading?])
                (<sub [::subs/existing-billing-period]))
       [select-billing-period])
     [:div.payment-setup__pay-confirm-container
      {:class (str "payment-setup__pay-confirm-container__" (name p))}
      [pay-confirm-content p]
      [:img (get-in data/package-data [p :img])]]]))

(defmethod payment-setup-step-content
  :pay-success
  [_]
  (let [role       (<sub [::subs/job-title])
        job-slug   (<sub [::subs/job-slug])
        job-id     (<sub [::subs/job-id])
        action     (<sub [::subs/action])
        company    (<sub [::subs/company-name])
        verts      (<sub [::subs/verticals])
        package    (<sub [::subs/package])
        existing-billing-period (<sub [::subs/existing-billing-period])]
    [:div.payment-setup__pay-success.has-text-centered
     [:h1 "Welcome to the team!"]
     [icon "tick"]
     (cond (and job-id (= :publish action))
           [:p "Your role of '" role "' at '" company "' is now live on " (list->sentence (map (comp #(str % "Works") str/capitalize) verts)) "!"]
           (and job-id (= :applications action))
           [:p "Your can now view the applications for your role '" role "'!"]
           (= :free package)
           [:p "We're glad to have you on board!"]
           existing-billing-period
           [:p "Your update was successful!"]
           :else
           [:p "Your payment was successful!"])
     [:div.columns
      (cond
        job-id
        [:div.column
         (if (= :publish action)
           [link [:button.button.is-full-width
                  "View your role"] :job :slug job-slug]
           [link [:button.button.is-full-width
                  "View applications"] :company-applications :query-params {:job-id job-id}])]
        action
        (when (= :applications action)
          [:div.column
           [link [:button.button.is-full-width
                  "View all applications"] :company-applications]]))
      [:div.column
       [link [:button.button.is-full-width
              {:class (when action "button--inverted")}
              "View your dashboard"] :homepage]]]]))

(defn error-display
  []
  (let [error (<sub [::subs/error])
        error-message (<sub [::subs/error-message])]
    [:div.payment-setup.payment-setup--error.has-text-centered
     [:h2 (str "Unfortunately, something went wrong \uD83D\uDE22")]
     (when (and (= error :failed-payment) error-message)
       [:div.payment-setup--error__message
        [:p "The following message was returned when we tried to process the payment"]
        [:strong error-message]])
     [:button.button.button--large
      {:on-click #(.reload js/location)}
      "Try again"]
     [link [:button.button.button--large.button--inverted
            "Back to Dashboard"] :homepage]
     [:small "(" (name error) ")"]]))

(defn page
  []
  (let [step           (<sub [::subs/payment-setup-step])
        action         (<sub [::subs/action])
        current-class? #(when (= step %) {:class "payment-setup__steps--current"})
        error          (<sub [::subs/error])
        upgrading?     (<sub [::subs/upgrading?])
        step-labels    {:select-package (if upgrading?
                                          "Select Upgrade"
                                          "Select Package")
                        :pay-confirm (if (<sub [::subs/existing-billing-period])
                                       "Selecting Billing & Pay"
                                       "Pay / Confirm")
                        :pay-success (case action
                                       :publish      "Publish Role"
                                       :applications "View Applications"
                                       "Complete")}]
    [:div.main-container
     [:div.main
      (cond
        error
        [error-display]

        (or (not step) (<sub [::subs/loading?]))
        [:div.payment-setup.payment-setup--loading]

        :else
        [:div.payment-setup
         [:div.payment-setup__steps.is-hidden-mobile
          [:span (current-class? :select-package) (str "1. " (:select-package step-labels))]
          [:span ">"]
          [:span (current-class? :pay-confirm)    (str "2. " (:pay-confirm step-labels))]
          [:span ">"]
          [:span (current-class? :pay-success)    (str "3. " (:pay-success step-labels))]]
         [:div.payment-setup__steps.is-hidden-desktop
          [:span.payment-setup__steps--current
           (case step
             :select-package (str "Step 1 of 3 - " (:select-package step-labels))
             :pay-confirm    (str "Step 2 of 3 - " (:pay-confirm step-labels))
             :pay-success    (str "Step 3 of 3 - " (:pay-success step-labels))
             "???")]]
         [:div.payment-setup__content-container
          {:class (str "payment-setup__content-container--step-" (name step))}
          [:div.column.payment-setup__content
           [payment-setup-step-content step]]]])]
     [:div.payment-setup-work-with
      (companies-section "Hire Engineers From")]]))

(defn publish-celebration
  [{:keys [title company-name verticals on-close]}]
  [:div.payment-publish-celebration
   [:div.payment-publish-celebration__close
    {:on-click #(on-close)}
    [icon "close"]]
   [:div.payment-publish-celebration__content
    [:h1 "Yay! \uD83C\uDF89"]
    [:h1 (str "The role of " title " at " company-name " is now live on "
              (list->sentence (map #(get-in verticals/vertical-config [% :platform-name]) verticals)))]]])
