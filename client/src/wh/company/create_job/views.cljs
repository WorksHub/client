(ns wh.company.create-job.views
  (:require
    [clojure.set :as set]
    [rcslider :as slider]
    [re-frame.core :refer [dispatch]]
    [reagent.core :as r]
    [wh.common.data :as data]
    [wh.common.text :as txt]
    [wh.common.upload :as upload]
    [wh.company.components.forms.views :refer [rich-text-field]]
    [wh.company.create-job.db :as create-job]
    [wh.company.create-job.events :as events]
    [wh.company.create-job.subs :as subs]
    [wh.company.edit.subs :as edit-subs]
    [wh.components.common :refer [link]]
    [wh.components.forms.views :as f
     :refer [labelled-checkbox field-container
             select-field text-field radio-buttons tags-field logo-field]]
    [wh.components.icons :refer [icon]]
    [wh.components.navbar :as nav-common]
    [wh.components.verticals :as vertical-views]
    [wh.db :as db]
    [wh.subs :refer [<sub error-sub-key]]
    [wh.util :as util]
    [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def slider-obj (aget js/window "rc-slider"))
(def slider (r/adapt-react-class slider-obj))
(def create-slider-with-tooltip (aget slider-obj "createSliderWithTooltip"))
(def slider-range (r/adapt-react-class (create-slider-with-tooltip (aget slider-obj "Range"))))

(defn field
  [k & {:as opts}]
  (let [{:keys [disabled? label error]} opts
        {:keys [message show-error?]} (when-not (false? error) (<sub [(error-sub-key k)]))]
    (merge {:value     (<sub [(keyword "wh.company.create-job.subs" (name k))])
            :id        (db/key->id k)
            :label     (when label [:span label])
            :error     message
            :read-only disabled?
            :validate  (get-in create-job/fields [k :validate])
            :dirty?    (when show-error? true)
            :on-change [(keyword "wh.company.create-job.events" (str "edit-" (name k)))]}
           (dissoc opts :label))))

(defn verify-lat-long-and-help-links []
  (let [long (<sub [::subs/location__longitude])
        lat  (<sub [::subs/location__latitude])]
    (when (and long lat)
      [:div
       [:div.job-edit__location__latlon__help__text
        "These are important to give candidates accurate recommendations"]
       [:div.job-edit__location__latlon__help__link
        [:a.a--underlined
         {:href   (<< "https://www.google.com/maps/search/?api=1&query=~{lat},~{long}")
          :target "_blank"
          :rel    "noopener"}
         "Click here to verify latitude and longitude on Google Maps"]]])))

(defn company-links []
  [:span "or "
   (if-let [company-id (<sub [::subs/company-id])]
     [link (str "Edit " (<sub [::subs/company__name])) :admin-edit-company :id company-id :class "a--underlined"]
     [link "Create a new company" :create-company :class "a--underlined"])])

(defn remuneration
  []
  (let [currency (<sub [::subs/remuneration__currency])
        competitive (<sub [::subs/remuneration__competitive])]
    [:fieldset.job-edit__remuneration
     [:h2 "Remuneration"]
     [:p "Fill in the salary details below, or select ‘Competitive’"]
     (when (<sub [::subs/show-salary-details?])
       [:div.job-edit__remuneration__salary
        [:div.columns.is-variable.is-4
         [:div.column.is-3
          [select-field (<sub [::subs/remuneration__currency])
           (field ::create-job/remuneration__currency
                  :label "* Currency"
                  :options   (<sub [::subs/currencies]))]]
         (if currency
           [:div.column.job-edit__remuneration__salary__slider
            (field-container
              {:label "* Salary range"}
              [slider-range
               {:value     (<sub [::subs/salary-range-js])
                :min       (<sub [::subs/salary-min])
                :max       (<sub [::subs/salary-max])
                :step      (case (<sub [::subs/remuneration__time-period])
                             "Yearly" 1000
                             50)
                :on-change #(dispatch [::events/set-salary-range (js->clj %)])}])]
           [:div.column.job-edit__remuneration__currency__prompt
            "Select a currency in order to define the salary range"])
         (when currency
           [:div.column.is-narrow.job-edit__remuneration__salary__label
            [:span (<sub [::subs/salary-label])]])]
        (field-container
          {:class   "job-edit__remuneration__salary__time-period"
           :label   "* Salary rate is:"
           :inline? true}
          [radio-buttons (<sub [::subs/remuneration__time-period])
           {:options   (<sub [::subs/time-periods])
            :on-change [::events/edit-remuneration__time-period]}])])
     [:div.job-edit__remuneration__additional-options
      (when-not competitive
        [labelled-checkbox nil (field ::create-job/remuneration__equity
                                      :label "Equity offered")])
      [labelled-checkbox nil (field ::create-job/remuneration__competitive
                                    :label "Competitive")]]]))

(defn skills
  [admin?]
  [:fieldset.job-edit__skills
   [:h2 "Skills"]
   [tags-field
    (<sub [::subs/tag-search])
    (field ::create-job/tags
           :label              "* Enter any of the key skills required for this role"
           :collapsed?         (<sub [::subs/tags-collapsed?])
           :placeholder        "e.g. Clojure, Haskell, Scala"
           :tags               (<sub [::subs/matching-tags])
           :on-change          [::events/set-tag-search]
           :on-toggle-collapse #(dispatch [::events/toggle-tags-collapsed])
           :on-tag-click       #(dispatch [::events/toggle-tag %])
           :on-add-tag         (when admin? #(dispatch [::events/toggle-tag %])))]])

(defn location
  []
  (let [form-open? (<sub [::subs/search-location-form-open?])]
    [:fieldset.job-edit__location
     [:h2 "Location"]
     [labelled-checkbox nil (field ::create-job/remote :label "Remote")]
     [:div
      [text-field nil (field ::create-job/search-address
                             :error false
                             :label "* Address finder"
                             :placeholder "Type street address or postcode to search"
                             :auto-complete "off"
                             :suggestions (<sub [::subs/location-suggestions])
                             :on-select-suggestion [::events/select-location-suggestion])]
      (when-not form-open?
        [:span "or " [:a.pseudo-link {:on-click #(dispatch [::events/set-editing-address true])}
                      "Enter the address manually"]])
      (when form-open?
        (if (<sub [::subs/editing-address?])
          [:div
           [:hr]
           [text-field nil (field ::create-job/location__street
                                  :label "Street")]
           [text-field nil (field ::create-job/location__city
                                  :label (str (when-not (<sub [::subs/remote]) "* ")  "City")
                                  :hide-icon? true
                                  :suggestions (<sub [::subs/city-suggestions])
                                  :on-select-suggestion [::events/select-city-suggestion])]
           [text-field nil (field ::create-job/location__post-code
                                  :label "Postcode / Zipcode")]
           [text-field nil (field ::create-job/location__state
                                  :label "State")]
           [text-field nil (field ::create-job/location__country
                                  :label "* Country"
                                  :hide-icon? true
                                  :suggestions (<sub [::subs/country-suggestions])
                                  :on-select-suggestion [::events/select-country-suggestion])]
           [:div.columns.job-edit__location__latlon
            [:div.column
             [text-field nil (field ::create-job/location__latitude :label "Latitude"
                                    :type :number
                                    :error false)]]
            [:div.column
             [text-field nil (field ::create-job/location__longitude :label "Longitude"
                                    :type :number
                                    :error false)]]]
           [:div.job-edit__location__latlon__help
            (verify-lat-long-and-help-links)]]

          (field-container
            {:label "Address"}
            [:div.job-edit__location__condensed-address
             [:div
              (for [[idx part] (map-indexed vector (<sub [::subs/condensed-address]))]
                ^{:key (str idx part)}
                [:span part])]
             [:div.job-edit__location__condensed-address__edit
              {:on-click #(dispatch [::events/set-editing-address true])}
              [icon "edit"]]])))]]))

(defn description
  [admin?]
  [:fieldset
   [:h2 "Description"]
   [rich-text-field (field ::create-job/private-description-html
                           :class "job-edit__private-description"
                           :label (if admin? "* Private description"
                                      "* Responsibilities, duties and detailed tech requirements"))]
   (when admin?
     [rich-text-field (field ::create-job/public-description-html
                             :class "job-edit__public-description"
                             :label "* Public description (for clients on 'Essential' package, this field will be overridden by the private description whenever they edit it)")])])

(defn integrations
  []
  [:fieldset.job-edit__integrations
   [:h2 "Integrations"]
   (when (<sub [::subs/greenhouse-integration?])
     [text-field nil (field ::create-job/ats-job-id
                            :label "Greenhouse Job ID"
                            :suggestions (<sub [::subs/greenhouse-jobs])
                            :hide-icon? true
                            :on-select-suggestion [::events/edit-ats-job-id])])])

(defn select-company
  []
  ;; we have to do some work to make this validate as 'company-id' but display as 'company__name'
  (let [{:keys [message show-error?]} (<sub [(error-sub-key ::create-job/company-id)])]
    [:div
     [text-field nil (field ::create-job/company__name
                            :id    (db/key->id ::create-job/company-id)
                            :class "is-marginless"
                            :placeholder "Type and select from list"
                            :label "* Company"
                            :error  message
                            :dirty? (when show-error? true)
                            :validate (get-in create-job/fields [::create-job/company-id :validate])
                            :suggestions (<sub [::subs/company-suggestions])
                            :on-select-suggestion [::events/select-company-suggestion])]
     [company-links]]))

(defn role-details
  [admin?]
  [:fieldset.job-edit__details
   [text-field nil (field ::create-job/title :label "* Role title")]
   (let [tl (<sub [::subs/tagline])
         chars-left (- create-job/tagline-max-length (count tl))]
     [text-field nil (field ::create-job/tagline
                            :label (<< "* Tagline (summarise the role in one sentence, ~{chars-left} characters remain)"))])
   [:div.columns.is-mobile.is-variable.is-8.job-edit__details__role-type-sponsor-offer
    [:div.column
     [select-field (<sub [::subs/role-type])
      {:options create-job/role-types
       :label "* Role type"
       :on-change [::events/edit-role-type]}]]
    [:div.column
     [:div.job-edit__details__sponsorship-offered
      [labelled-checkbox nil (field ::create-job/sponsorship-offered :label "Sponsorship offered")]]]]])

(defn main-form
  [admin?]
  [:section.split-content-section
   [:h2.title "Role details"]
   [:form.wh-formx.wh-formx__layout
    {:on-submit #(.preventDefault %)}
    [role-details admin?]
    [remuneration]
    [skills admin?]
    [location]
    [description admin?]
    (when (<sub [::subs/show-integrations?])
      [integrations])]])

(defn company-form
  [_admin?]
  (let [tags-collapsed? (r/atom true)]
    (fn [admin?]
      (when (or (not (<sub [::subs/validate-existing-company-fields])) admin?)
        [:section.split-content-section
         [:h2.title "Company details"]
         (when admin?
           [:form.wh-formx.wh-formx__layout
            {:on-submit #(.preventDefault %)}
            [select-company]])
         (cond
           (<sub [::subs/company-loading?])
           [:div.create-job__inline-loader
            [:div.is-loading-spinner]]
           (<sub [::subs/company-id])
           [:form.wh-formx.wh-formx__layout.create-job__company-form
            {:on-submit #(.preventDefault %)}
            (when (not (<sub [::subs/validate-existing-company-field :logo]))
              (let [pending-logo                  (<sub [::subs/pending-logo])
                    {:keys [message show-error?]} (<sub [(error-sub-key :wh.company.profile/logo)])]
                [:div.create-job__company-form__logo-field
                 {:id (db/key->id :wh.company.profile/logo)}
                 [:label {:class (util/merge-classes "label"
                                                     (when show-error? "field--invalid"))}
                  "* Your company logo"]
                 [logo-field
                  {:error          message
                   :dirty?         (when show-error? true)
                   :value          pending-logo
                   :loading?       (<sub [::subs/logo-uploading?])
                   :on-select-file (upload/handler
                                     :launch [:wh.common.logo/logo-upload]
                                     :on-upload-start [::events/logo-upload-start]
                                     :on-success [::events/logo-upload-success]
                                     :on-failure [::events/logo-upload-failure])}]]))
            (when (not (<sub [::subs/validate-existing-company-field :description-html]))
              (let [pending-description           (<sub [::subs/pending-company-description])
                    {:keys [message show-error?]} (<sub [(error-sub-key :wh.company.profile/description-html)])]
                [rich-text-field {:value        pending-description
                                  :id           (db/key->id :wh.company.profile/description-html)
                                  :error        message
                                  :dirty?       (when show-error? true)
                                  :label        "* Company description"
                                  :placeholder  "eg WorksHub enables companies to gain access to the right talent in a crowded market. Our smart personalised candidate experience gives users the ability to make better data-driven applications in real-time reducing the time to hire from weeks to days. We are striving to build something amazing! "
                                  :on-change    #(dispatch [::events/set-pending-company-description %])}]))

            (when (not (<sub [::subs/validate-existing-company-field :tags]))
              (let [selected-tag-ids              (<sub [::subs/selected-benefit-tag-ids])
                    matching-tags                 (<sub [::subs/matching-benefit-tags {:include-ids selected-tag-ids :size 20}])
                    {:keys [message show-error?]} (<sub [(error-sub-key :wh.company.profile/benefit-tags)])]
                [tags-field
                 (<sub [::subs/benefits-search])
                 {:tags               (map #(if (contains? selected-tag-ids (:key %))
                                              (assoc % :selected true)
                                              %) matching-tags)
                  :id                 (db/key->id :wh.company.profile/benefit-tags)
                  :collapsed?         @tags-collapsed?
                  :on-change          [::events/set-benefits-search]
                  :label              "* Benefits"
                  :error              message
                  :dirty?             (when show-error? true)
                  :placeholder        "e.g. flexible working, health insurance, child care, training etc"
                  :on-toggle-collapse #(swap! tags-collapsed? not)
                  :on-tag-click
                  #(when-let [id (some (fn [tx] (when (= (:tag tx) %) (:key tx))) matching-tags)]
                     (dispatch [::events/toggle-selected-benefit-tag-id id]))}]))])]))))

(defn settings-pod
  []
  [:div
   [:div.wh-formx [:h2.is-hidden-desktop "Settings"]]
   [:div.pod.job-edit__settings-pod
    [:h1.is-hidden-mobile "Settings"]
    [:form.wh-formx.wh-formx__layout
     {:on-submit #(.preventDefault %)}
     [text-field nil (field ::create-job/manager
                            :label "* Manager"
                            :placeholder "Type to search Managers"
                            :suggestions (<sub [::subs/manager-suggestions])
                            :on-select-suggestion [::events/select-manager])]]
    [:div
     [:span "This role has been:"]
     [labelled-checkbox nil (field ::create-job/promoted
                                   :label "Promoted")]
     [labelled-checkbox nil (field ::create-job/approved
                                   :label "Approved")]]]])

(defn company-profile-pod
  []
  (let [slug (<sub [::subs/company-slug])]
    [:section.split-content-section.job-edit__company-profile-pod
     [:h2.title "Company Profile"]
     [:div.job-edit__company-profile-pod__content
      [:div.job-edit__company-profile-pod__content__img
       [:img {:src "/images/hiw/company/hiw/hiw4.svg"
              :alt ""}]]
      [:div.job-edit__company-profile-pod__content__info
       [:p "We use information from your company profile to make your job roles stand out to potential candidates."]
       [link
        [:button.button.button--inverted.is-hidden-desktop
         {:class (when-not slug "button--loading")}
         "Update company profile"]
        :company :slug slug]]]
     [link
      [:button.button.is-hidden-mobile
       {:class (when-not slug "button--loading button--inverted")}
       "Update company profile"]
      :company :slug slug]]))

(defn page []
  (let [admin? (<sub [:user/admin?])
        package (<sub [::subs/package])
        published? (<sub [::subs/published])]
    [:div.main.job-edit
     [:h1 (<sub [::subs/page-title])]
     [:div.split-content
      [:div.split-content__main
       [company-form admin?]
       [main-form admin?]]
      [:div.job-edit__side-pods.split-content__side
       [vertical-views/verticals-pod
        {:toggleable? true
         :on-verticals (<sub [::subs/verticals])
         :off-verticals (set/difference (set verticals/future-job-verticals) (<sub [::subs/verticals]))
         :toggle-event [::events/toggle-vertical]
         :class-prefix "job-edit"}]
       (if admin?
         [settings-pod]
         [company-profile-pod])]]
     [:div.split-content
      [:div.split-content__main
       [:div.is-flex.job-edit__footer
        [:button.button.button--medium.is-pulled-right
         {:id "job-edit__footer__save"
          :on-click #(do (.preventDefault %)
                         (dispatch [::events/create-job]))
          :class    (when (<sub [::subs/saving?])
                      "button--inverted button--loading")}
         (when-not (<sub [::subs/saving?])
           (cond published? "Save"
                 (<sub [::subs/edit?]) "Save & Preview"
                 :else "Create role"))]
        (when published?
          [:button.button.button--medium.button--inverted.is-pulled-right
           {:id "job-edit__footer__unpublished"
            :on-click #(do (.preventDefault %)
                           (dispatch [::events/unpublish-job]))
            :class    (when (<sub [::subs/saving?])
                        "button--loading")}
           (when-not (<sub [::subs/saving?]) "Unpublish")])
        (when-let [error (<sub [::subs/error])]
          (f/error-component error { :id "company-edit-error-desktop"}))]]]]))
