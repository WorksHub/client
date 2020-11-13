(ns wh.admin.create-candidate.views
  (:require
    [re-frame.core :refer [dispatch]]
    [wh.admin.create-candidate.db :as create-candidate]
    [wh.admin.create-candidate.events :as events]
    [wh.admin.create-candidate.subs :as subs]
    [wh.common.upload :as upload]
    [wh.components.forms.views
     :refer [error-component-outdated field-container labelled-checkbox
             tags-field text-field]]
    [wh.components.icons :refer [icon]]
    [wh.db :as db]
    [wh.subs :refer [<sub error-sub-key]]))

(defn field
  [k & {:as opts}]
  (let [{:keys [disabled? label error index]} opts
        {:keys [message show-error?]} (when-not (false? error) (<sub [(error-sub-key k)]))
        value-sub (cond-> [(keyword "wh.admin.create-candidate.subs" (name k))]
                    index (conj index))
        edit-event (cond-> [(keyword "wh.admin.create-candidate.events" (str "edit-" (name k)))]
                     index (conj index))]
    (merge {:value     (<sub value-sub)
            :id        (db/key->id k)
            :label     (when label [:span label])
            :error     message
            :read-only disabled?
            :validate  (get-in create-candidate/fields [k :validate])
            :dirty?    (when show-error? true)
            :on-change edit-event}
           (dissoc opts :label))))

(defn resume []
  (cond
    (<sub [::subs/cv-uploading?])
    [:span "Uploading..."]
    (<sub [::subs/cv-filename])
    [:span.create-candidate__uploaded-file
     [:a.a--underlined {:href (<sub [::subs/cv-url])}
      [icon "upload"]
      (<sub [::subs/cv-filename])]]
    :else
    [:label.file-label.create-candidate__upload-cv
     [:span.create-candidate__upload-cta [icon "upload"] "Upload candidate’s resume"]
     [:input.file-input {:type "file"
                         :name "cv"
                         :on-change (upload/handler
                                     :launch [::events/cv-upload]
                                     :on-upload-start [::events/cv-upload-start]
                                     :on-success [::events/cv-upload-success]
                                     :on-failure [::events/cv-upload-failure])}]]))

(defn resume-and-links []
  [:fieldset.create-candidate__resume-and-links
   [:h2 "Resume & links"]
   [resume]
   [text-field nil (field ::create-candidate/github-url :label "Candidate's GitHub profile")]
   (into [:div]
         (for [i (range (<sub [::subs/num-other-links]))]
           [text-field nil
            (field ::create-candidate/other-links
                   :label (<sub [::subs/other-links-title i])
                   :index i)]))
   [:a.create-candidate__add-link
    {:on-click #(do (.preventDefault %)
                    (dispatch [::events/add-link]))}
    [icon "add-new-2"] "Add another link"]])

(defn main-form []
  [:form.wh-formx.wh-formx__layout
   [:fieldset.create-candidate__details
    [:h2 "Candidate details"]
    [text-field nil (field ::create-candidate/name :label "* Name")]
    [text-field nil (field ::create-candidate/email :label "* Email")]
    [text-field nil (field ::create-candidate/phone :label "Phone Number")]
    [labelled-checkbox nil (field ::create-candidate/notify :label "Notify the candidate that their account has been created")]
    [text-field nil (field ::create-candidate/location-search
                           :label "* Location"
                           :placeholder "Type street address or postcode to search"
                           :auto-complete "off"
                           :suggestions (<sub [::subs/location-suggestions])
                           :on-select-suggestion [::events/select-location-suggestion])]
    [text-field nil (field ::create-candidate/current-company-search
                           :label "Current company"
                           :auto-complete "off"
                           :suggestions (<sub [::subs/company-suggestions])
                           :on-select-suggestion [::events/select-company-suggestion])]]

   [resume-and-links]
   [:fieldset.create-candidate__preferences
    [:h2 "Candidate preferences"]
    ;; TODO: 5222, put tag selector back if needed
    [tags-field
     (<sub [::subs/company-tag-search])
     (field ::create-candidate/company-tags
            :label       "Company"
            :placeholder "e.g. flexible working, gym membership"
            :on-change    [::events/edit-company-tag-search]
            :on-tag-click #(dispatch [::events/toggle-company-tag (:tag %)])
            :on-add-tag   #(dispatch [::events/toggle-company-tag %])
            :tags         (<sub [::subs/matching-company-tags]))]]
   (when-let [error (<sub [::subs/error])]
     (error-component-outdated error {:id "create-candidate-error-desktop"}))
   [:div.buttons-container
    [:button.button.button--medium {:on-click #(do (.preventDefault %)
                                                   (dispatch [::events/save]))} "Create candidate"]]])

(defn page []
  [:div.main-container
   [:div.main
    [:div.create-candidate.wh-formx-page-container
     [:h1 "Create candidate"]
     [:div.columns.is-variable.is-2
      [:div.column.is-7
       [main-form]]]]]])
