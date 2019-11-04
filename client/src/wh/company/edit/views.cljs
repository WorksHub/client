(ns wh.company.edit.views
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [re-frame.core :refer [dispatch]]
    [reagent.core :as r]
    [wh.common.cost :as cost]
    [wh.common.data :as data :refer [package-data billing-data]]
    [wh.common.logo]
    [wh.common.text :refer [pluralize]]
    [wh.common.upload :as upload]
    [wh.company.components.forms.views :refer [rich-text-field]]
    [wh.company.edit.db :as edit]
    [wh.company.edit.events :as events]
    [wh.company.edit.subs :as subs]
    [wh.company.payment.db :as payment-db]
    [wh.company.payment.views :as payment]
    [wh.company.views :refer [double->dollars int->dollars]]
    [wh.components.common :refer [link]]
    [wh.components.ellipsis.views :refer [ellipsis]]
    [wh.components.forms.views :as f :refer [labelled-checkbox field-container select-field text-field select-input logo-field]]
    [wh.components.github :as github]
    [wh.components.icons :refer [icon]]
    [wh.components.overlay.views :refer [popup-wrapper]]
    [wh.components.selector :refer [selector]]
    [wh.db :as db]
    [wh.routes :as routes]
    [wh.subs :refer [<sub error-sub-key]]
    [wh.user.subs :as user-subs]
    [wh.util :as util]
    [wh.verticals :as verticals]))

(defn field [k & {:as args}]
  (merge
    {:id        (db/key->id k)
     :value     (<sub [(keyword "wh.company.edit.subs" (name k))])
     :on-change [(keyword "wh.company.edit.events" (str "edit-" (name k)))]
     :on-blur #(dispatch [::events/check k])}
    (when-let [error (<sub [(error-sub-key k)])]
      {:error error
       :validate (get-in edit/fields [k :validate])
       :dirty? true})
    args))

(defn manager-pod
  [admin?]
  (let [{:keys [name email]} (<sub [::subs/manager-name-and-email])]
    [:div.pod.company-edit__manager-pod
     [:span (if admin? "WorksHub manager details:" "Your WorksHub manager:")]
     [:div.company-edit__manager-pod__details
      [:h1 (or name "Pending")]
      [:span (or email "No manager is currently assigned")]]
     (if admin?
       [:form.wh-formx.wh-formx__layout
        [text-field nil (field ::edit/manager
                               :label [:span "Assign manager:"]
                               :placeholder "Type to search Managers"
                               :suggestions (<sub [::subs/manager-suggestions])
                               :on-select-suggestion [::events/select-manager])]]
       (when email
         [:a.company-edit__manager-pod__contact
          {:href (if email (str "mailto:" email) "#")
           :id "company-edit__manager-pod__contact"}
          "Get in touch"]))]))

(defn invoices-pod
  [admin?]
  (let [invoices (<sub [::subs/invoices])]
    (when-not (empty? invoices)
      [:div.pod.company-edit__invoices-pod
       [:span (if admin? "Company invoices:" "Your most recent invoices:")]
       [:div
        [:table.table.is-bordered
         [:thead
          [:tr
           [:th "Date"]
           [:th "Amount"]]]
         [:tbody
          (for [{:keys [date url amount] :as i} invoices]
            ^{:key url}
            [:tr
             {:class (when-not amount "skeleton")}
             [:th (if (not amount)
                    [:span]
                    [:a.a--underlined (when url {:href url :target "_blank" :rel "noopener"}) date])]
             [:td
              [:span
               (double->dollars amount)]]])]]]
       [:div.company-edit__invoices-pod__contact
        [:span "Can't find what you need?"]
        [:a.a--underlined
         {:id "company-edit__invoices-pod__email"}
         "Just drop us an email"]]])))

(defn admin-pod
  []
  (let [pou          (<sub [::subs/paid-offline-until])
        pou-loading? (<sub [::subs/paid-offline-until-loading?])
        pou-error    (<sub [::subs/paid-offline-until-error])
        id           "company-edit__paid-offline-pod__input"
        disabled?    (<sub [::subs/disabled?])]
    [:div.pod.company-edit__admin-pod
     [:h2 "Offline Payment"]
     [:p "If the company has paid outside of the platform, this should indicate the date their subscription will terminate."]
     (if (str/blank? pou)
       [:i [:h3 "Not set"]]
       [:h3 pou])
     (when (<sub [:wh.user/super-admin?])
       [:form.wh-formx
        {:on-submit #(let [v (.-value (.getElementById js/document id))]
                       (when-not (str/blank? v)
                         (dispatch (vec (concat [::events/save-paid-offline-until] (map js/parseInt (str/split v #"\-"))))))
                       (.preventDefault %))}
        [:label {:for id} "Set date:"]
        [:div.is-flex
         [:input {:type "date" :id id :disabled pou-loading?}]
         [:button.button.button--inverted
          {:class (when pou-loading? "button--loading")}
          "Save"]]])
     (when pou-error
       [:span.is-error (str/capitalize pou-error) "!"])
     (when (<sub [:wh.user/super-admin?])
       [:div
        [:h2 "Super Admin Controls"]
        (if disabled?
          [:p "Enabling the company will reinstate permissions and approve all associated users."]
          [:p "Disabling the company will remove all permissions, unpublish all the company jobs and reject all associated users."])
        (if disabled?
          [:button.button
           {:class    (when (<sub [::subs/disable-loading?]) "button--loading button--inverted")
            :on-click #(when (js/confirm (str "Are you sure you want to enable " (<sub [::subs/name]) "?"))
                         (dispatch [::events/disable false]))}
           "Enable Company"]
          [:button.button.button--inverted
           {:class    (when (<sub [::subs/disable-loading?]) "button--loading")
            :on-click #(when (js/confirm (str "Are you sure you want to disable " (<sub [::subs/name]) "?"))
                         (dispatch [::events/disable true]))}
           "Disable Company"])])]))

(defn company-details
  [edit? admin?]
  [:form.wh-formx.wh-formx__layout
   [:h2 "Company details"]
   [:fieldset
    (when-not edit?
      [:div.columns.is-mobile.company-edit__name-and-logo
       [:div.column.is-narrow
        [logo-field (field ::edit/logo
                           :loading? (<sub [::subs/logo-uploading?])
                           :on-select-file (upload/handler
                                             :launch [:wh.common.logo/logo-upload]
                                             :on-upload-start [::events/logo-upload-start]
                                             :on-success [::events/logo-upload-success]
                                             :on-failure [::events/logo-upload-failure]))]]
       [:div.column
        [text-field nil (field ::edit/name
                               :label [:span "* Company name"]
                               :suggestions (<sub [::subs/suggestions])
                               :on-select-suggestion [::events/select-suggestion])]]])

    (when-not edit?
      [rich-text-field (field ::edit/description-html
                              :placeholder "Tell our community all about your company and how great it is to work for you…"
                              :class "company-edit__description"
                              :label [:span "* Company introduction"])])
    (when admin? [select-field nil (field ::edit/vertical
                                          :label [:span "Vertical"]
                                          :options verticals/future-job-verticals)])
    (when admin? [labelled-checkbox nil (field ::edit/auto-approve
                                               :class "company-edit__checkbox"
                                               :label [:span "Automatically approve candidates?"])])

    [:div.is-flex.company-edit__field-footer
     (let [saving? (<sub [::subs/saving?])]
       [:button.button.button--small.company-edit__save-details
        {:on-click #(do (.preventDefault %)
                        (dispatch [::events/save-company]))
         :class    (str (when edit? "button--inverted ")
                        (when saving? "button--inverted button--loading"))}
        (when-not saving? (if edit? "Save" "Create"))])
     (when-let [error (<sub [::subs/error])]
       (f/error-component error { :id "company-edit-error-desktop"}))]]])

(defn users
  [admin?]
  [:form.wh-formx.wh-formx__layout
   [:h2 "Users & notifications"]
   [:p "Below is a list of your authorised account users. Grant access to new users by entering their name and email below."]
   (when-let [users (not-empty (<sub [::subs/users]))]
     (let [notifs (<sub [::subs/user-notifications])]
       [:table.company-edit__users
        [:tbody
         [:tr.company-edit__user-header
          [:th.company-edit__user-header--name "Name"]
          [:th.company-edit__user-header--email "Email"]
          [:th.company-edit__user-header--notify "Notify"]
          (when admin? [:th.company-edit__user-header--admin ""])]
         (for [{:keys [email name id]} users]
           [:tr.company-edit__user
            {:key email}
            [:td.company-edit__user--name name]
            [:td.company-edit__user--email email]
            [:td.company-edit__user--notify [labelled-checkbox nil
                                             {:label ""
                                              :id (str "notify_" id)
                                              :value (contains? notifs email)
                                              :label-class "is-pulled-right"
                                              :class "is-pulled-right"
                                              :on-change [::events/toggle-user-notification email]}]]
            (when admin? [:td.company-edit__user--admin
                          [icon "close"
                           :on-click #(dispatch [::events/remove-user id])]])])]]))
   [:fieldset
    [text-field nil (field ::edit/new-user-name
                           :label [:span "* Name"])]
    [text-field nil (field ::edit/new-user-email
                           :label [:span "* Email"])]
    [:div.is-flex.company-edit__field-footer
     (let [saving? (<sub [::subs/user-adding?])]
       [:button.button.button--small.button--inverted.company-edit__add-user
        {:on-click #(do (.preventDefault %)
                        (dispatch [::events/add-new-user]))
         :class    (str (when saving? "button--inverted button--loading"))}
        (when-not saving? "Add User")])
     (when-let [error (<sub [::subs/user-error])]
       (f/error-component error {:id "company-edit-user-error-desktop"}))]]])

(defn integrations []
  [:div.wh-formx.wh-formx__layout
   (when (<sub [::subs/some-integrations-not-connected?])
     [:div
      [:h2 "Available Integrations"]
      [:p "Connect your WorksHub account to Slack or Greenhouse to receive application notifications via these channels"]
      [:div.company-edit__integrations
       (when-not (<sub [::subs/slack-connected?])
         [:a {:href (when-not (<sub [:user/admin?])
                      (routes/path :oauth-slack))}
          [:button.button.company-edit__integration
           (merge
             {:id "company-edit__integration--slack"}
             (if (<sub [:user/admin?])
               {:on-click #(do (.preventDefault %)
                               (dispatch [::events/toggle-integration-popup true]))}
               {:on-click #(dispatch [:company/track-install-integration-clicked "Slack"])}))
           [:img {:src "/images/company/slack.svg"}]]])
       (when-not (<sub [::subs/greenhouse-connected?])
         [:a {:href (when (and (not (<sub [:user/admin?]))
                               (<sub [:wh.user/can-use-integrations?]))
                      (routes/path :oauth-greenhouse))}
          [:button.button.company-edit__integration
           (merge
             {:id "company-edit__integration--greenhouse"}
             (if (<sub [:user/admin?])
               {:on-click #(do (.preventDefault %)
                               (dispatch [::events/toggle-integration-popup true]))}
               {:on-click #(dispatch [:company/track-install-integration-clicked "Greenhouse"])})
             (when (and (not (<sub [:user/admin?]))
                        (not (<sub [:wh.user/can-use-integrations?])))
               {:on-click #(do (.preventDefault %)
                               (dispatch [::events/upgrade]))}))
           [:img {:src "/images/company/greenhouse.svg"}]]])
       (when (and (not (<sub [::subs/workable-connected?]))
                  (<sub [:wh.user/workshub?]))
         [:a {:href (when (and (not (<sub [:user/admin?]))
                               (<sub [:wh.user/can-use-integrations?]))
                      (routes/path :oauth-workable))}
          [:button.button.company-edit__integration
           (merge
             {:id "company-edit__integration--workable"}
             (if (<sub [:user/admin?])
               {:on-click #(do (.preventDefault %)
                               (dispatch [::events/toggle-integration-popup true]))}
               {:on-click #(dispatch [:company/track-install-integration-clicked "Workable"])})
             (when (and (not (<sub [:user/admin?]))
                        (not (<sub [:wh.user/can-use-integrations?])))
               {:on-click #(do (.preventDefault %)
                               (dispatch [::events/upgrade]))}))
           [:img {:src "/images/company/workable.svg"}]]])
       (when (and (not (<sub [:user/admin?]))
                  (not (<sub [:user/company-connected-github?])))
         [github/install-github-app
          {:id "company-edit__integration--github"}])]])
   (when (<sub [::subs/some-integrations-connected?])
     (let [deleting (<sub [::subs/deleting-integration?])]
       [:div.company-edit__connected-integrations
        [:h2 "Connected Integrations"]
        (when (<sub [::subs/slack-connected?])
          [:div.company-edit__connected-integration
           [:img {:alt "Slack" :title "Slack" :src "/images/company/slack-icon.svg"}]
           [:button.button.button--small.button--inverted.company-edit__add-user
            {:on-click #(do (.preventDefault %)
                            (dispatch [::events/delete-integration :slack]))
             :class    (str (when deleting "button--inverted button--loading"))}
            (when-not deleting "Delete Integration")]])
        (when (<sub [::subs/greenhouse-connected?])
          [:div.company-edit__connected-integration
           [:img {:alt "Greenhouse" :title "Greenhouse" :src "/images/company/greenhouse-icon.svg"}]
           [:button.button.button--small.button--inverted.company-edit__add-user
            {:on-click #(do (.preventDefault %)
                            (dispatch [::events/delete-integration :greenhouse]))
             :class    (str (when deleting "button--inverted button--loading"))}
            (when-not deleting "Delete Integration")]
           (when (<sub [:user/admin?])
             [:span "Company manager will be set as referrer/source for each candidate."])])
        (when (<sub [::subs/workable-connected?])
          [:div.company-edit__connected-integration
           [:img {:alt "Workable" :title "Workable" :src "/images/company/workable-icon.svg"}]
           [:button.button.button--small.button--inverted.company-edit__add-user
            {:on-click #(do (.preventDefault %)
                            (dispatch [::events/delete-integration :workable]))
             :class    (str (when deleting "button--inverted button--loading"))}
            (when-not deleting "Delete Integration")]])
        (when (<sub [:user/company-connected-github?])
          [:div.company-edit__connected-integration
           [:img {:alt "GitHub" :title "GitHub" :src "/images/company/github-icon.svg"}]
           [link "Manage issues" :manage-issues :class "button button--small button--inverted"]])]))])

(defn integration-popup []
  [:div.is-full-panel-overlay
   [:div.company-edit__integration-placeholder.has-text-centered
    [:div
     [:h1 "Admins cannot connect integrations"]
     [:p "Only company users can connect integrations"]
     [:button.button.button--small
      {:on-click #(dispatch [::events/toggle-integration-popup false])}
      "Back"]]]])

(defn cancel-plan-dialog
  []
  [popup-wrapper
   {:id :cancel-plan
    :codi? false
    :on-close #(dispatch [::events/show-cancel-plan-dialog false])}
   (let [exp (<sub [::subs/payment-expires])]
     (if (<sub [:user/admin?])
       [:div
        [:h1 "When would you like to cancel this subscription?"]
        [:p "The company will be moved to the Unselected package and all jobs will be unpublished."]
        [:button.button
         {:disabled exp
          :on-click #(dispatch [::events/cancel-plan false])}
         "At the end of the period"]
        [:button.button.button--inverted
         {:on-click #(dispatch [::events/cancel-plan true])}
         "Immediately"]]
       [:div
        [:h1 "Are you sure you want to cancel your subscription?"]
        [:p "Your active subscription will be cancelled and all your jobs will be taken offline."]
        [:button.button.button--inverted
         {:on-click #(dispatch [::events/cancel-plan true])}
         "Cancel immediately"]
        [:button.button
         {:on-click #(dispatch [::events/show-cancel-plan-dialog false])}
         "Don't cancel"]]))])

(defn card-data
  [{:keys [last-4-digits brand expiry] :as card}]
  [:div.card-data
   {:class (when (nil? card) "skeleton")}
   [:div.card-data__brand
    (if brand
      [:img {:src (str "/cards/" (str/replace (str/lower-case brand) #"\s" "") ".png")}]
      [:div.empty])]
   [:div.card-data__info
    [:div
     [:span brand " (ends " last-4-digits ")"]]
    [:div
     [:span (gstring/format "Expires %02d/%d " (:month expiry) (:year expiry))]]]])

(defn status-button [{:keys [prefix] :as args}]
  (let [make-sub #(keyword (namespace ::subs/here) (str (name prefix) "-" %))]
    (f/status-button
      (assoc args
             :enabled? (<sub [(make-sub "button-enabled?")])
             :waiting? (<sub [(make-sub "button-waiting?")])
             :status   (<sub [(make-sub "status")])))))

(defmulti billing-paragraph identity)
(defmethod billing-paragraph :default [_]
  "You are currently being billed on a monthly basis but you could save more by switching to six-monthly billing \uD83D\uDCB0")
(defmethod billing-paragraph :one [_]
  "You are currently being billed on a monthly basis but you could save more by switching to six-monthly billing \uD83D\uDCB0")
(defmethod billing-paragraph :three [_]
  "You are currently being billed on a quarterly basis but you could save more by switching to six-monthly billing \uD83D\uDCB0")
(defmethod billing-paragraph :six [_]
  "You're on the most cost-effective billing plan!")

(defn cancel-payment
  []
  (when (<sub [::subs/has-subscription?])
    (let [exp (<sub [::subs/payment-expires])
          admin? (<sub [:wh.user/super-admin?])]
      [:div.company-edit__payment-details__cancel-plan
       [:h2 "Cancel your plan"]
       [:p "We'll be very sorry to see you go \uD83D\uDE15"]
       [:p "Please drop us an email to let us know why you’d like to cancel and then we’ll get it sorted for you."]
       [:p "Your account will close on the last day of your current plan and no further payments will be taken."]
       [:div
        (when exp
          [:p.is-error (str "This subscription is set to expire on " exp)])
        (if
            (or (<sub [::subs/can-cancel-sub?])
                admin?)
          [:button.button
           {:class (when (<sub [::subs/cancel-plan-loading?]) "button--loading button--inverted")
            :disabled (and exp (not admin?))
            :on-click #(dispatch [::events/show-cancel-plan-dialog true])}
           "Cancel plan"]
          [:a {:href (str "mailto:hello@works-hub.com?subject=[" (<sub [::subs/name]) "]+I+wish+to+cancel+my+plan")
               :target "_blank"
               :rel    "noopener"}
           [:button.button
            "Contact WorksHub"]])]])))

(defn payment-details
  [edit? admin?]
  (let [{:keys [card billing-period] :as payment} (<sub [::subs/payment])
        package                                   (<sub [::subs/package-kw])
        event                                     [::events/update-card-details]
        has-sub?                                  (<sub [::subs/has-subscription?])]
    [:div.company-edit__payment-details
     (when card
       [:div.company-edit__payment-details__card-details
        [:h2 "Payment Details"]
        [card-data card]
        (when payment
          [:div
           [:p "If you'd like to update your card details, please re-enter them below:"]
           [payment/card-form {:id     :company-settings-update
                               :terms? false
                               :event  event}]
           [status-button
            {:id       "update-card-details"
             :prefix   ::update-card-details
             :on-click #(when-let [el (.getElementById js/document "stripe-card-form")]
                          (payment/submit-stripe-form! :company-settings-update event))
             :class    "button--inverted"
             :text     "Update Payment Details"}]])])
     (when has-sub?
       [:div.company-edit__payment-details__billing-and-pricing
        [:h2 "Change billing period"]
        [:div
         [:p (billing-paragraph billing-period)]
         [link
          [:button.button "Change billing period"]
          :payment-setup
          :step :pay-confirm
          :query-params {:billing          billing-period
                         :package          package
                         :existing-billing billing-period
                         :breakdown        false}]]])
     (when has-sub?
       [:div.company-edit__payment-details__coupons
        [:h2 "Discounts"]
        [:div
         [:p "Details of any existing discounts will be listed here. You can also apply new discount by entering your code and pressing Apply."]
         [:p.company-edit__payment-details__coupons__description
          (if-let [coupon (<sub [::subs/coupon])]
            [:span "Currently applied: " [:span (str (:description coupon) ", " (name (:duration coupon)))]]
            [:i "You don't currently have an active discount."])]
         (when (<sub [::subs/coupon-apply-success?])
           [:p.company-edit__payment-details__coupons__new-coupon
            [:i "Success! A new coupon has been applied to your subscription."]])
         [payment/coupon-field true [::events/apply-coupon]]]])
     (when edit?
       [cancel-payment])]))

(defmulti error->message identity)
(defmethod error->message :default [_]
  "Unfortunately we could not load the page you were looking for. If the issue persists, please contact us.")
(defmethod error->message :company-with-same-name-already-exists [_]
  "The company with the same name already exists.")
(defmethod error->message :failed-to-fetch-company [_]
  "Unfortunately we could not load company information at this time. If the issue persists, please contact us.")

(defn hiring-pod
  [admin?
   {:keys [cost extras description img perks] :as package}
   {:keys [discount number] :as billing-period :or {discount 0}}
   coupon
   {:keys [recurring-fee placement-percentage accepted-at]}]
  [:div.pod.company-edit__hiring-pod
   (if package
     [:span.company-edit__hiring-pod__your-package "Your package"]
     [:p "You don't currently have a package selected. Select one of our packages to begin your hiring adventure."])
   [:div.company-edit__hiring-pod__title
    [:h1 (:name package)]
    (cond (pos? cost)
          [:h2 (int->dollars (cost/calculate-monthly-cost cost discount coupon)) [:i "/" (:per package)]]
          (and accepted-at recurring-fee)
          [:h2 (int->dollars (cost/calculate-monthly-cost recurring-fee discount coupon)) [:i "/month" ]]
          :else [:div.empty])
    (when (and (or (pos? cost) (and accepted-at recurring-fee)) (:description billing-period))
      [:div.company-edit__hiring-pod__title__billing-description
       [:span "Billed every "]
       (when (> number 1) [:strong number " "])
       [:strong (pluralize number "month")]])
    (when accepted-at
      [:div.company-edit__hiring-pod__title__billing-description
       [:span [:strong placement-percentage "%"] " placement fee"]])]
   [:div.company-edit__hiring-pod__extras extras]
   (when description
     [:div.company-edit__hiring-pod__description description])
   [:div.company-signup__hiring_pod__logo-container
    [:div.company-signup__hiring_pod__logo-circle
     [:img img]]]
   (when perks
     [:ul.company-edit__hiring-pod__perks
      (for [perk perks]
        ^{:key perk}
        [:li.company-edit__hiring-pod__perk
         [icon "tick"]
         [:span perk]])])
   (if admin?
     [link [:button.button "Create Take-Off Offer"]
      :create-company-offer
      :id (<sub [::subs/id])]
     [link (if package "Upgrade/View package options" "Select a package")
      :payment-setup
      :step :select-package
      :class "a--underlined"])])

(defn disabled-banner
  []
  [:div.company-edit__disabled-banner
   [icon "error"]
   "This company is currently disabled."])

(defn page [edit? admin?]
  (let [page-selection (<sub [::subs/page-selection])
        error          (<sub [::subs/error])
        payment?       (<sub [::subs/payment])
        loading?       (<sub [::subs/loading?])
        disabled?      (<sub [::subs/disabled?])
        settings-title (cond (and admin? edit?) (str (<sub [::subs/name]) " Settings")
                             edit? "Settings"
                             :else "Create company")]
    [:div.main-container
     [:div.main
      (cond
        loading?
        [:div.company-edit.company-edit--loading
         [:h1 settings-title]]
        error
        [:div.company-edit.wh-formx-page-container.company-edit--error
         [:h1 "An error occurred"]
         [:h3 (error->message error)]]
        :else
        [:div.company-edit.wh-formx-page-container
         {:class (str "company-edit--" (if edit? "edit" "create"))}
         (if (and admin? edit?)
           [:div.company-edit__banner
            [link [:h1 settings-title] :company-dashboard :id (<sub [::subs/id])]
            (when disabled?
              (disabled-banner))]
           [:h1 settings-title])
         (when (and edit? payment?)
           [selector
            page-selection
            (<sub [::subs/page-selections])
            #(dispatch [::events/set-page-selection %])])
         [:div.columns.is-variable.is-2
          [:div.column.is-7
           (if (and (= page-selection :payment-details) payment?)
             [:div
              [payment-details edit? admin?]]
             [:div
              (when admin?
                [company-details edit? admin?])
              (when edit?
                [integrations])
              (when edit?
                [users admin?])])]

          [:div.column.is-4.is-offset-1.company-edit__side-pods
           [:div.company-edit__side-pods--container
            [manager-pod admin?]
            (let [pk (<sub [::subs/package-kw])
                  bp (<sub [::subs/billing-period])
                  coupon (<sub [::subs/coupon])
                  offer (<sub [::subs/offer])]
              (hiring-pod admin? (-> (get package-data pk)
                                     (dissoc :perks))
                          (get billing-data bp)
                          coupon
                          offer))

            (when (and edit? admin?)
              [admin-pod])
            (when (= page-selection :payment-details)
              [invoices-pod admin?])]]]])]

     (when (<sub [::subs/show-integration-popup?])
       (integration-popup))
     (when (<sub [::subs/showing-cancel-plan-dialog?])
       [cancel-plan-dialog])]))

(defn create-page []
  (page false true))

(defn edit-page []
  (page true (<sub [:user/admin?])))
