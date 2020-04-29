;; This ns defines a number of components for use in forms.
;; Each of them has the following signature:
;;
;;   (defn component [value options]
;;
;; where value is the value currently displayed by the component
;; (typically sourced from a subscription), and options is a map
;; describing the behaviour of the component.
;;
;; Some options are common to most components. These include:
;;
;;   :label      label displayed before the component
;;   :help       help message displayed under the component
;;   :on-change  re-frame event to be dispatched when the component's
;;               value changes; will be conj'ed with the new value

(ns wh.components.forms.views
  "Form building blocks."
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as reagent]
    [wh.common.emoji :as emoji]
    [wh.common.re-frame-helpers :refer [merge-classes]]
    [wh.common.specs.primitives :as p]
    [wh.common.user :as common-user]
    [wh.components.icons :refer [icon]]
    [wh.subs :refer [<sub]]
    [wh.util :as util])
  (:require-macros
    [clojure.core.strint :refer [<<]]))

(defn target-value [^js/Event ev _]
  (-> ev .-target .-value))

(defn target-checked [^js/Event ev]
  (-> ev .-target .-checked))

(defn target-numeric-value [^js/Event ev {:keys [minv maxv]}]
  (let [value (-> ev .-target .-valueAsNumber)]
    (when-not (js/isNaN value)
      (cond-> value
              minv (max minv)
              minv (min maxv)))))

;; This one's signature is an exception to what's described above.
(defn field-container
  "A generic container for a field. Wraps one or more controls in a
  container widget. Provides support for labels and help messages."
  [{:keys [label class error help id inline?]} & controls]
  (vec
   (concat
    [:div
     {:id    id
      :class (merge-classes
              "field"
              (when (> (count controls) 1) "grouped")
              (when error "field--errored")
              class)}
     (when label
       [(if (string? error)
          :label.label.field--invalid
          :label.label)
        {:class (when inline? "is-pulled-left")} label])]
    (when help
      [[:div.help help]])
    (conj (mapv (fn [control] [:div.control control]) controls)
          (if (string? error)
            [:div.field__error.field--invalid error]
            [:div.field__error.field--invalid.is-invisible "errors go here"])))))

(defn text-input
  "A bare text input. Typically not used standalone, but wrapped as
  text-field. See that function for parameters description.

  If supplied, the `dirty` option should be an atom which holds
  a boolean value; it will be reset to true upon first change of
  the input. This is used by text-field."
  [value {:keys [type on-change placeholder on-scroll rows dirty]
          :or {type :text}
          :as options}]
  [(if (= type :textarea)
     :textarea.textarea
     :input.input)
   (merge (when-not (= type :textarea) {:type type})
          {:value value :placeholder placeholder}
          (when on-change
            {:on-change #(let [new-value ((if (= type :number) target-numeric-value target-value) % options)]
                           (when dirty
                             (reset! dirty true))
                           (if (fn? on-change)
                             (on-change new-value)
                             (dispatch-sync (conj on-change new-value))))})
          (when on-scroll
            {:on-scroll #(dispatch on-scroll)})
          (select-keys options [:on-focus :on-blur :auto-complete :disabled :read-only :on-key-press
                                :step])
          (when (and rows (= type :textarea))
            {:rows rows}))])

(defn suggestions-list
  "A list of suggestions for a text-field."
  [items {:keys [on-select-suggestion]}]
  (into [:ul.input__suggestions]
        (map (fn [{:keys [id label]}]
               ;; use 'onmousedown' as well as 'onclick' as in some sitations 'onclick' wont
               ;; fire because the suggestions list is removed too quickly due to !focused parent
               [:li {:on-click #(when on-select-suggestion
                                  (dispatch (conj on-select-suggestion id)))
                     :on-mouse-down #(when on-select-suggestion
                                       (dispatch (conj on-select-suggestion id)))}
                label])
             items)))

(defmulti error-message identity)

(defmethod error-message ::p/non-empty-string [_]
  "This field should not be empty.")

(defmethod error-message ::p/email [_]
  "This is not a valid email address.")

(defmethod error-message :default [_]
  "This field has an incorrect value.")

(defn- text-field-input-options [dirty focused & [{:keys [on-blur on-focus]}]]
  {:dirty dirty
   :on-focus #(do (reset! dirty true)
                  (reset! focused true)
                  (when on-focus (on-focus %)))
   :on-blur #(do (reset! focused false)
                 (when on-blur (on-blur %)))})
  

(defn text-field-error [value {:keys [validate error]} dirty focused]
  (when (and @dirty (not @focused))
    (or error
        (when validate
          (let [valid? (s/valid? validate value)]
            (when-not valid?
              (or error (error-message validate))))))))

(defn text-field
  "A textual form field. Options include:
  :type - the type of this form field as a keyword: :text, :textarea,
          :number, :tel, etc.
  :error - error message to render if this field's value is incorrect;
           if not specified, (error-message validate) will be used.
  :validate - a spec to validate this field's value against.
  :suggestions - a list of suggestions displayed beneath the field.
  :on-select-suggestion - the event dispatched when a suggestion is selected."
  [value options]
  (let [dirty (reagent/atom false)
        focused (reagent/atom false)]
    (fn [value {:keys [suggestions dirty? error force-error? read-only on-select-suggestion on-remove hide-icon?] :as options}]
      (when (and (not (nil? dirty?))
                 (boolean? dirty?))
        (reset! dirty dirty?))
      (let [value (or value (:value options))
            suggestable? (and (or (seq suggestions) on-select-suggestion) (not hide-icon?))
            show-suggestion? (and (seq suggestions) @focused (not read-only))
            removable? (and on-remove value (not @focused))]
        (field-container (merge options
                                {:error (if (and (string? error) force-error?)
                                          error
                                          (text-field-error value options dirty focused))})
                         [:div.text-field-control
                          {:class (str (when show-suggestion? "text-field-control--showing-suggestions")
                                       (when suggestable? " text-field-control--suggestable")
                                       (when removable? " text-field-control--removable"))}
                          (text-input value (merge options (text-field-input-options dirty focused options)))
                          (when suggestable? [icon "search-new" :class "search-icon"])
                          (when show-suggestion?
                            [suggestions-list suggestions options])
                          (when removable?
                            [icon "close" :class "remove-text-field-btn" :on-click #(dispatch-sync on-remove)])])))))

(defn checkbox
  "A bare checkbox. Value should be a boolean."
  [value {:keys [on-change] :as options}]
  [:input {:type      "checkbox"
           :checked   (or value (:value options))
           :class     (:class options)
           :on-change #(dispatch-sync (conj on-change (target-checked %)))}])

(defn checkbox-field
  "A checkbox with a label."
  [value options]
  (field-container options
                   (checkbox value options)))

(defn labelled-checkbox
  "Like checkbox-field, but with a different markup."
  [value {:keys [label disabled indeterminate? on-change class label-class] :as options}]
  (let [id (or (:id options) (name (gensym)))]
    [:div.checkbox {:class (util/merge-classes
                             class
                             (when disabled "checkbox--disabled")
                             (when indeterminate? "checkbox--indeterminate"))}
     [:input
      (merge
        {:type "checkbox"
         :id id
         :disabled disabled

         :checked (if (nil? value) (:value options) value)}
        (when on-change
          {:on-change #(when (not disabled)
                         (dispatch-sync (conj on-change (target-checked %))))}))]
     [:label {:for id
              :class label-class}
       [:div {:class "checkbox__box"}]
       label]]))

(defn- sanitize-select-options
  [vals]
  (mapv #(if (string? %) {:id %, :label %} %) vals))

(defn select-input
  "A bare select input. Typically not used standalone, but wrapped as
  select-field. See that function for parameters description."
  [value {:keys [options on-change disabled]}]
  (let [options (sanitize-select-options options)]
    [:div.select
     (into
       [:select
        (merge {:value (str (util/index-of (mapv :id options) value))}
               (when on-change
                 {:on-change #(let [id (:id (nth options (js/parseInt (target-value % nil))))]
                                (if (fn? on-change)
                                  (on-change id)
                                  (dispatch-sync (conj on-change id))))})
               (when disabled {:disabled true}))]
       (map-indexed (fn [i {:keys [label]}]
                      [:option {:value (str i)} label])
                    options))]))

(defn select-field
  "A dropdown form field.
  :options - possible options for the field. They can either be strings,
             or maps with :id and :label (\"foo\" is shorthand for
             {:id \"foo\", :label \"foo\"}).
  :value - should be one of the :id's."
  [value {:keys [dirty? force-error?] :as options}]
  (field-container (if (or dirty? force-error?)
                     options
                     (dissoc options :error))
                   (select-input (or value (:value options)) options)))

(defn radio-buttons
  "Renders a set of radio buttons wrapped together in a div."
  [value {options :options, name- :name, on-change :on-change}]
  (let [options (sanitize-select-options options)
        name- (or name- (name (gensym)))]
    (into
      [:div.radios]
      (for [[i {:keys [id label]}] (map-indexed vector options)
            :let [el-id (str name- i)]]
        [:span.radio-item
         [:input {:id el-id
                  :type :radio
                  :checked (= value id)
                  :on-change #(when on-change
                                (if (fn? on-change)
                                  (on-change id)
                                  (dispatch (conj on-change id))))}]
         [:label {:for el-id}]
         label]))))

(defn radio-field
  "A selection of radio controls + label"
  [value args]
  (field-container args (radio-buttons value args)))

(defn multiple-buttons
  "An input widget allowing to pick zero or more of the given options.
  value should be a set (a sub-set of :options)."
  [value {:keys [options on-change]}]
  (into [:div.multiple-buttons]
        (for [{:keys [id label]} (sanitize-select-options options)]
          [:button.button
           (merge
             (when-not (contains? value id)
               {:class "button--light"})
             (when on-change
               {:on-click #(do (.preventDefault %)
                               (dispatch (conj on-change id)))}))
           label])))

(defn multiple-checkboxes
  "Like multiple-buttons, but renders a set of labelled checkboxes."
  [multi-value {:keys [options on-change]}]
  (into [:div.multiple-checkboxes]
        (map
         (fn [{:keys [value label disabled] :as option}]
           [labelled-checkbox (contains? multi-value value)
            {:label label
             :disabled disabled
             :on-change (conj on-change value)}])
         options)))

(defn multi-edit
  "A multiplexer of text fields (or other fields). Renders as many
  fields as there are items, plus one (allowing to input a new item).
  Passes options to each rendered field, conj'ing field number to both
  :i entry in options and to the on-change vector."
  [items
   {:keys [label on-change component]
    :or {component text-field}
    :as options}]
  (into [:div]
        (for [[i item] (map-indexed vector (conj (vec items) nil))]
          [component item
           (merge (dissoc options :label)
                  {:i i
                   :on-change (when on-change (conj on-change i))}
                  (when (zero? i) {:label label}))])))

(defn required-star []
  [:span.has-text-danger " *"])

(defn tag
  [{:keys [tag count selected class] :as full-tag} read-only on-tag-click]
  [:li {:class (merge-classes "tag"
                              (when selected "tag--selected")
                              class)
        :on-click #(when-not read-only (on-tag-click full-tag))}
   (if count (<< "~{tag} (~{count})") tag)
   (when selected
     [icon "close"])])

(defn tags-field
  [text opts]
  (let [dirty (reagent/atom false)
        focused (reagent/atom false)
        -collapsed? (reagent/atom true)]
    (fn [text {:keys [collapsed? error label placeholder tags dirty? read-only empty-label id
                      on-change on-tag-click on-toggle-collapse on-blur on-focus on-add-tag] :as opts}]
      (when (and (not (nil? dirty?))
                 (boolean? dirty?))
        (reset! dirty dirty?))
      (let [has-tags? (pos? (count tags))
            error (and @dirty (not @focused) error)
            collapsed? (cond
                         (not (str/blank? text)) false ;; can't be collapsed if we have text
                         (not (contains? opts :collapsed?)) @-collapsed? ;; use internal state if we're controlling it
                         :otherwise collapsed?)
                         
            on-toggle-collapse (or on-toggle-collapse #(swap! -collapsed? not))]
        [:div
         {:class (merge-classes
                   "tags-container"
                   (when collapsed? "tags-container--collapsed")
                   (when (string? error) "tags-container--errored"))}
         [text-field
          text
          (merge
            {:id id
             :label label
             :class "tags-text-input"
             :placeholder placeholder
             :on-focus #(do (reset! focused true)
                            (reset! dirty true)
                            (when on-focus (on-focus %)))
             :on-blur #(do (reset! focused false)
                           (when on-blur (on-blur %)))
             :on-change on-change
             :on-key-press (when (and on-add-tag (not (str/blank? text)))
                             #(if (= (.-key %) "Enter")
                                (on-add-tag text)))
             :read-only read-only}
            (when (string? error)
              {:force-error? true
               :error error}))]
         (when (and on-add-tag (not (str/blank? text)))
           [:div.tag-add
            {:on-click #(on-add-tag text)
             :class (when label "tag-add--with-label")}
            [icon "plus"]])
         [:div.tags-selection
          (when has-tags?
            [icon "roll-down"
             :class "tags-roll"
             :on-click on-toggle-collapse])
          (if has-tags?
            (let [grouped-tags (group-by :selected tags)
                  no-selected? (zero? (count (get grouped-tags true)))]
              (if no-selected?
                [:div
                 (into [:ul.tags.tags--no-selected]
                       (map #(tag % read-only on-tag-click) tags))]
                [:div
                 (into [:ul.tags.tags--selected]
                       (map #(tag % read-only on-tag-click) (get grouped-tags true)))
                 (into [:ul.tags.tags--unselected]
                       (map #(tag % read-only on-tag-click) (concat (get grouped-tags nil)
                                                                    (get grouped-tags false))))]))
            (if (str/blank? text)
              [:div.tags-loading "Loading..."]
              [:div.tags-empty (or empty-label "No results matched the search term.")]))]
         (if (string? error)
           [:div.field__error.field--invalid error]
           [:div.field__error.field--invalid.is-invisible "errors go here"])]))))

(defn error-component
  [message opts]
  [:div.is-danger.form-error
   opts
   [:span.form-error__emoji (emoji/int->char 10071)]
   [:span.form-error__content message]])

(defn logo-field
  [_options]
  (let [dirty (reagent/atom false)]
    (fn [{:keys [value on-select-file loading? error id text dirty?] :as options
          :or {text "logo"}}]
      (when (and (not (nil? dirty?))
                 (boolean? dirty?))
        (reset! dirty dirty?))
      (let [error (and @dirty error)]
        [:div.logo
         {:id id
          :class (when error "logo--errored")}
         (if loading?
           [:div.logo__is-loading]
           (field-container
             {}
             [:label
              [:input.file-input {:type      "file"
                                  :name      [:span "logo"]
                                  :accept    "image/*"
                                  :on-change #(do
                                                (reset! dirty true)
                                                (on-select-file %))}]
              (if-not (clojure.string/blank? value)
                [:img.logo__add-new {:src (:value options)}]
                [:div.logo__add-new.logo__add-new--empty
                 {:class (when error "logo__add-new--errored")}
                 [:div "+"]
                 [:div text]])]))]))))

(defn status-button
  [{:keys [id on-click class text enabled? waiting? status]}]
  [:div.status-button
   [:div.status
    {:class (when (:status status) (str "status--" (name (:status status))))}
    (when (:status status)
      [:div.status__label
       (case (:status status)
         :good [icon "tick"]
         :bad  [icon "error"])
       [:span.is-hidden-mobile (:message status)]])]
   [:button.button
    {:id id
     :class (str class (when waiting? " button--inverted button--loading"))
     :disabled (false? enabled?)
     :on-click #(when on-click (on-click id))}
    text]])

(defn toggle
  [{:keys [value on-change]}]
  [:div
   {:class (util/merge-classes "toggle"
                               (when value "toggle--enabled"))}
   [:div.toggle__track]
   [:div.toggle__thumb-wrapper
    [:div.toggle__thumb
     {:on-click #(when on-change
                   (on-change (not value)))}]]])

(defn predefined-avatar-picker [{:keys [selected on-change]}]
  (into
    [:div.avatars]
    (for [i (range 1 6)]
      [:img.avatar {:class (when (= i selected) "selected")
                    :src (common-user/avatar-url i)
                    :on-mouse-down #(dispatch [on-change i])
                    :alt (str "Pre-set avatar image " i)}])))

(defn custom-avatar-picker [{:keys [uploading? set-avatar avatar-url]}]
  (if uploading?
    [:p "Uploading your avatar, please wait..."]
    [:div.file.avatar-picker
     [:label.file-label
      [:input.file-input {:type "file"
                          :name "avatar"
                          :accept    "image/*"
                          :on-change set-avatar}]
      [:span.file-cta.button
       [:span.file-label "Choose an avatar"]]]
     (when avatar-url
       [:img.avatar {:src avatar-url
                     :alt "Uploaded avatar"}])]))

(defn avatar-field [{:keys [custom-avatar-mode
                            set-custom-avatar-mode
                            predefined-avatar
                            set-predefined-avatar
                            uploading-avatar?
                            avatar-url
                            set-avatar]}]
  (let [custom? custom-avatar-mode]
    [:div.field.avatar-field
     [:label.label "Your avatar"]
     [:div.control
      [:label.radio
       [:input {:type "radio"
                :name "avatar-type"
                :checked (not custom?)
                :on-change #(dispatch [set-custom-avatar-mode false])}]
       " Use a predefined avatar"
       (when-not custom?
         [predefined-avatar-picker
          {:selected predefined-avatar
           :on-change set-predefined-avatar}])]
      [:label.radio
       [:input {:type "radio"
                :name "avatar-type"
                :checked custom?
                :on-change #(dispatch [set-custom-avatar-mode true])}]
       " Upload your own image"
       (when custom?
         [custom-avatar-picker {:uploading? uploading-avatar?
                                :set-avatar set-avatar
                                :avatar-url avatar-url}])]]]))
