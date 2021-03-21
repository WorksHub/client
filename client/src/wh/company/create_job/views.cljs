(ns wh.company.create-job.views
  (:require ["rc-slider" :as slider]
            [clojure.set :as set]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [wh.common.upload :as upload]
            [wh.company.create-job.db :as create-job]
            [wh.company.create-job.events :as events]
            [wh.company.create-job.subs :as subs]
            [wh.components.common :refer [link]]
            [wh.components.forms :as forms]
            [wh.components.forms.views :as f
             :refer [labelled-checkbox field-container
                     select-field text-field tags-field logo-field]]
            [wh.components.icons :refer [icon]]
            [wh.components.rich-text-field.loadable :refer [rich-text-field]]
            [wh.components.verticals :as vertical-views]
            [wh.db :as db]
            [wh.subs :refer [<sub error-sub-key]]
            [wh.util :as util]
            [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def create-slider-with-tooltip (aget slider "createSliderWithTooltip"))
(def slider-range (r/adapt-react-class (create-slider-with-tooltip (aget slider "Range"))))

(defn field
  [k & {:as opts}]
  (let [{:keys [disabled? label error data-test]} opts
        {:keys [message show-error?]}             (when-not (false? error) (<sub [(error-sub-key k)]))]
    (merge {:value     (<sub [(keyword "wh.company.create-job.subs" (name k))])
            :id        (db/key->id k)
            :label     (when label [:span label])
            :error     message
            :read-only disabled?
            :validate  (get-in create-job/fields [k :validate])
            :dirty?    (when show-error? true)
            :on-change [(keyword "wh.company.create-job.events" (str "edit-" (name k)))]
            :data-test data-test}
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
  (let [currency    (<sub [::subs/remuneration__currency])
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
                  :options (<sub [::subs/currencies])
                  :data-test "select-currency")]]
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
          [forms/radio-buttons (<sub [::subs/remuneration__time-period])
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
   [:h2 "Technologies & frameworks"]
   (let [tags (<sub [::subs/matching-tags])]
     ;; TODO: use new tags-filter-field component here
     [tags-field
      (<sub [::subs/tag-search])
      {:label              "* Enter the technologies and frameworks that will be used in this role"
       :id                 "_wh_company_create-job_db_tags"
       :collapsed?         (<sub [::subs/tags-collapsed?])
       :placeholder        "e.g. Clojure, Haskell, Scala"
       :tags               tags
       :on-change          [::events/set-tag-search]
       :on-toggle-collapse #(dispatch [::events/toggle-tags-collapsed])
       :on-tag-click       #(dispatch [::events/toggle-tag %])
       :tag-data-test      "job-skill-tag"}])])

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
                             :placeholder "Type post/zip code or street address to search"
                             :auto-complete "off"
                             :suggestions (<sub [::subs/location-suggestions])
                             :on-select-suggestion [::events/select-location-suggestion]
                             :data-test "job-location")]
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
                                  :label "State"
                                  :help "USA only"
                                  :hide-icon? true
                                  :suggestions (<sub [::subs/state-suggestions])
                                  :on-select-suggestion [::events/select-state-suggestion])]
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
   [rich-text-field (field ::create-job/description-html
                           :class "job-edit__description"
                           :label (if admin? "* Description"
                                      "* Responsibilities, duties and detailed tech requirements")
                           :data-test "job-description")]])

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
  [_admin?]
  [:fieldset.job-edit__details
   [text-field nil (field ::create-job/title :label "* Role title" :data-test "role-title")]
   (let [tl         (<sub [::subs/tagline])
         chars-left (- create-job/tagline-max-length (count tl))]
     [text-field nil (field ::create-job/tagline
                            :label (<< "* Tagline (summarise the role in one sentence, ~{chars-left} characters remain)")
                            :data-test "role-tagline")])
   [:div.columns.is-mobile.is-variable.is-8.job-edit__details__role-type-sponsor-offer
    [:div.column
     [select-field (<sub [::subs/role-type])
      {:options   create-job/role-types
       :label     "* Role type"
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
    [description admin?]]])

(defn- benefit-tags []
  (r/with-let [tags-collapsed? (r/atom true)]
    (let [selected-tag-ids              (<sub [::subs/selected-benefit-tag-ids])
          matching-tags                 (<sub [::subs/matching-benefit-tags
                                               {:include-ids selected-tag-ids :size 20}])
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
        :on-tag-click       #(dispatch [::events/toggle-selected-benefit-tag-id (:id %)])
        :tag-data-test      "company-benefit-tag"}])))

(defn- company-logo []
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

(defn- company-description []
  (let [pending-description           (<sub [::subs/pending-company-description])
        {:keys [message show-error?]} (<sub [(error-sub-key :wh.company.profile/description-html)])]
    [rich-text-field {:value       pending-description
                      :id          (db/key->id :wh.company.profile/description-html)
                      :error       message
                      :dirty?      (when show-error? true)
                      :label       "* Company description"
                      :placeholder "eg WorksHub enables companies to gain access to the right talent in a crowded market. Our smart personalised candidate experience gives users the ability to make better data-driven applications in real-time reducing the time to hire from weeks to days. We are striving to build something amazing! "
                      :on-change   [::events/set-pending-company-description]
                      :data-test   "company-description"}]))

(defn company-form [admin?]
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

        (when-not (<sub [::subs/validate-existing-company-field :logo])
          [company-logo])

        (when-not (<sub [::subs/validate-existing-company-field :description-html])
          [company-description])

        (when-not (<sub [::subs/validate-existing-company-field :tags])
          [benefit-tags])])]))

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
     [labelled-checkbox nil (field ::create-job/approved
                                   :label "Approved")]]]])

(defn ats-pod
  []
  (let [heading (str (<sub [::subs/ats-name]) " Integration")]
    [:div
     [:div
      [:h2.is-hidden-desktop heading]]
     [:div.pod.job-edit__settings-pod.pod__ats
      [:h1.is-hidden-mobile heading]
      [:form.wh-formx.wh-formx__layout
       {:on-submit #(.preventDefault %)}
       (if (<sub [::subs/need-to-select-account?])
         [:div
          [select-field (<sub [::subs/workable-subdomain])
           {:label     [:span "Workable Account"]
            :on-change [::events/edit-workable-subdomain]
            :options   (<sub [::subs/workable-accounts])}]
          [:button.button.button--medium.is-pulled-right
           {:id "save-workable-account"
            :on-click #(dispatch [::events/save-workable-account])
            :class    (when (<sub [::subs/saving-workable-account?])
                        "button--inverted button--loading")}
           (when-not (<sub [::subs/saving-workable-account?])
             "Save")]]
         [text-field nil (field ::create-job/ats-job-id
                                :label (str (<sub [::subs/ats-name]) " Job ID")
                                :suggestions (<sub [::subs/ats-jobs])
                                :on-select-suggestion [::events/edit-ats-job-id])])]]]))

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
  (let [admin?     (<sub [:user/admin?])
        published? (<sub [::subs/published])]
    [:div.main.job-edit
     [:h1 (<sub [::subs/page-title])]
     [:div.split-content
      [:div.split-content__main
       [company-form admin?]

       [main-form admin?]

       [vertical-views/verticals-pod
        {:on-verticals  (<sub [::subs/verticals])
         :off-verticals (set/difference (set verticals/future-job-verticals) (<sub [::subs/verticals]))
         :toggle-event  [::events/toggle-vertical]
         :class-prefix  "job-edit"}]]
      [:div.job-edit__side-pods.split-content__side
       (when (<sub [::subs/show-integrations?])
         [ats-pod])
       (if admin?
         [settings-pod]
         [company-profile-pod])]]
     [:div.split-content
      [:div.split-content__main
       [:div.is-flex.job-edit__footer

        (let [edit?   (<sub [::subs/edit?])
              saving? (<sub [::subs/saving?])]
          [:button
           {:id        "job-edit__footer__save"
            :on-click  #(do (.preventDefault %)
                            (dispatch [::events/create-job]))
            :class     (cond-> (util/mc "button" "button--medium" "job-edit__save-button")
                               (not published?) (util/mc "button--inverted")
                               saving?          (util/mc "button--inverted" "button--loading"))
            :data-test "create-role"}
           (when-not saving?
             (cond published? "Save"
                   edit?      "Save & Preview"
                   :else      "Create"))])

        (when-not published?
          [:button
           {:id        "job-edit__footer__save_and_publish"
            :on-click  #(do (.preventDefault %)
                            (dispatch [::events/create-job true]))
            :class     (cond-> (util/merge-classes "button" "button--medium" "job-edit__save-button")
                               (<sub [::subs/saving?]) (util/merge-classes "button--inverted" "button--loading"))
            :data-test "save-and-publish"}
           (when-not (<sub [::subs/saving?])
             (if (<sub [::subs/edit?])
               "Save & Publish"
               "Create & Publish"))])

        (when published?
          [:button.button.button--medium.button--inverted.is-pulled-right
           {:id       "job-edit__footer__unpublished"
            :on-click #(do (.preventDefault %)
                           (dispatch [::events/unpublish-job]))
            :class    (when (<sub [::subs/saving?])
                        "button--loading")}
           (when-not (<sub [::subs/saving?]) "Unpublish")])

        (when-let [error (<sub [::subs/error])]
          (f/error-component-outdated error {:id "company-edit-error-desktop"}))]]]]))
