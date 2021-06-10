(ns wh.components.forms
  (:require [clojure.string :as str]
            [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.interop.forms :as interop-forms]
            [wh.re-frame :as r]
            [wh.re-frame.events :as events]
            [wh.util :as util]))

;; NOTE we will start moving CLJS form components into here
;; although they will probably operate quite differently

(defn field-container
  "A generic container for a field. Wraps one or more controls in a
  container widget. Provides support for labels and help messages."
  [{:keys [label class error help id inline? solo? hide-error? data-test]} & controls]
  (vec
    (concat
      [:div
       {:id        id
        :class     (util/mc
                     "field"
                     (when (> (count controls) 1) "grouped")
                     (when solo? "wh-formx")
                     (when error "field--errored")
                     class)
        :data-test data-test}
       (when label
         [(if error
            :label.label.field--invalid
            :label.label)
          {:class (when inline? "is-pulled-left")} label])]
      (when help
        [[:div.help help]])
      (conj (mapv (fn [control] [:div.control control]) controls)
            (when-not hide-error?
              (if (string? error)
                [:div.field__error.field--invalid error]
                [:div.field__error.field--invalid.is-invisible "errors go here"]))))))

(defn text-input
  "A bare text input. Typically not used standalone, but wrapped as
  text-field. See that function for parameters description.

  If supplied, the `dirty` option should be an atom which holds
  a boolean value; it will be reset to true upon first change of
  the input. This is used by text-field."
  [value {:keys [type on-change on-change-events on-input placeholder on-scroll
                 rows dirty class-input on-enter]
          :or   {type :text}
          :as   options}]
  [(if (= type :textarea)
     :textarea
     :input)
   (merge (when-not (= type :textarea) {:type (name type)})
          {:value value :placeholder placeholder
           :class (util/mc class-input
                           (if (= type :textarea)
                             "textarea" "input"))}
          (if (and on-change on-change-events)
            (interop-forms/multiple-on-change
              (interop-forms/on-change-fn on-change)
              (interop-forms/dispatch-events-on-change on-change-events))
            (merge (when on-change
                     (interop-forms/on-change-fn on-change))
                   (when on-change-events
                     (interop-forms/dispatch-events-on-change on-change-events))
                   (when (and (not on-change) (not on-change-events))
                     ;; in cljs we must provide on-change
                     #?(:cljs
                        {:on-change identity}))))
          (when on-input
            (interop-forms/on-input-fn on-input))
          (when on-enter
            (interop-forms/on-press-enter-fn on-enter))
          (when on-scroll
            {:on-scroll #(events/dispatch on-scroll)})
          (select-keys options [:on-focus :on-blur :auto-complete :disabled :read-only :on-key-press
                                :step :name])
          (when (and rows (= type :textarea))
            {:rows rows}))])

(defn text-field
  "A textual form field. Options include:
  :type - the type of this form field as a keyword: :text, :textarea,
          :number, :tel, etc.
  :error - error message to render if this field's value is incorrect;
           if not specified, (error-message validate) will be used.
  :validate - a spec to validate this field's value against.
  :suggestions - a list of suggestions displayed beneath the field.
  :on-select-suggestion - the event dispatched when a suggestion is selected."
  [options]
  (let [dirty   (r/atom false)
        focused (r/atom false)]
    (fn [{:keys [value suggestions dirty? error force-error? read-only on-select-suggestion on-remove hide-icon?] :as options}]
      (when (and (not (nil? dirty?))
                 (boolean? dirty?))
        (reset! dirty dirty?))
      (let [value            (or value "") ;; nil value not allowed
            suggestable?     (and (or (seq suggestions) on-select-suggestion) (not hide-icon?))

            removable?       (and on-remove value (not @focused))]
        (field-container (merge options
                                {:error (when (or (string? error) force-error?)
                                          (or error true))})
                         [:div.text-field-control
                          {:class (str #_(when show-suggestion? "text-field-control--showing-suggestions")
                                       (when suggestable? " text-field-control--suggestable")
                                       (when removable? " text-field-control--removable"))}
                          (text-input value options)
                          (when suggestable? [icon "search-new" :class "search-icon"])
                          #_(when show-suggestion?
                              [suggestions-list suggestions options]) ;; TODO Fix suggestable on SSRR text fields
                          (when removable?
                            [icon "close" :class "remove-text-field-btn" :on-click #(events/dispatch-sync on-remove)])])))))


(defn init-tags! [{:keys [id tags force-render selected collapsable? show-icons tags-url]}]
  #?(:cljs
     (js/initTags
       (js/document.getElementById id)
       ;; If tags are CLJS seq make sure to transform to JS, before sending to JS initTags fn.
       ;; Trying to keep component as transparent as possible for CLJS callers.
       (if (coll? tags) (clj->js tags) tags)
       force-render
       (if (coll? selected) (clj->js selected) selected)
       collapsable?
       show-icons
       tags-url)))

(defn tags-filter-field
  [{:keys [on-tag-select show-icons tags-url] :as opts :or {show-icons true}}]
  (let [dirty   (r/atom false)
        focused (r/atom false)
        text    (r/atom "")
        id      (or (:id opts) (str (gensym "tags-field")))
        render  (fn [{:keys [error label placeholder dirty? read-only solo? _tags
                            on-tag-select on-add-tag init-from-js-tags?
                            collapsable? new-design?]
                     :or   {collapsable? true new-design? false}}]
                  (when (and (not (nil? dirty?))
                             (boolean? dirty?))
                    (reset! dirty dirty?))

                  (let [error (and @dirty (not @focused) error)]
                    [:div
                     (merge {:id    id
                             :class (util/merge-classes
                                      "tags-container tags-container--alternative"
                                      [collapsable? "tags-container--collapsed"]
                                      [new-design? "tags-container--new-design"]
                                      [init-from-js-tags? "tags-container--wants-js-tags"]
                                      [(string? error) "tags-container--errored"])}
                            (when tags-url
                              {:data-tags-url tags-url})
                            #?(:clj (when on-tag-select
                                      {:onsubmit on-tag-select})))
                     [text-field
                      (merge
                        {:value        @text
                         :label        label
                         :solo?        solo?
                         :class        "tags-text-input"
                         :placeholder  placeholder
                         :on-key-press (when (and on-add-tag (not (str/blank? @text)))
                                         #(when (= (.-key %) "Enter")
                                            (on-add-tag @text)))
                         :read-only    read-only
                         :on-input     (interop-forms/filter-tags id text collapsable?)}
                        (when (string? error)
                          {:force-error? true
                           :error        error}))]
                     (when (and on-add-tag (not (str/blank? @text)))
                       [:div.tag-add
                        {:on-click #(on-add-tag @text)
                         :class    (when label "tag-add--with-label")}
                        [icon "plus"]])
                     [:div.tags-selection
                      (when collapsable?
                        [:div
                         (interop/toggle-class-on-click id "tags-container--collapsed")
                         [icon "roll-down"
                          :class "tags-roll"]])
                      [:div.tags-selection--tags-container
                       [:ul.tags.tags--top-level.tags--selected]
                       [:ul.tags.tags--top-level.tags--unselected
                        [:div.tags-loading "Loading…"]]]]
                     (if (string? error)
                       [:div.field__error.field--invalid error]
                       [:div.field__error.field--invalid.is-invisible "errors go here"])]))]

    #?(:cljs (r/create-class
               {:component-did-mount (fn [this]
                                       ;; *1
                                       ;; we're calling init-tags! whenever tags are present.
                                       ;; JS implementation makes decision whether it should really
                                       ;; initialise component at this point in time, basing on
                                       ;; component DOM state
                                       (let [{tags     :tags selected :selected collapsable? :collapsable?
                                              tags-url :tags-url}
                                             (r/props this)]
                                         (when (or tags tags-url)
                                           (init-tags!
                                             {:id           id
                                              :tags         tags
                                              :selected     selected
                                              :collapsable? collapsable?
                                              :tags-url     tags-url
                                              :show-icons   show-icons})))

                                       (when on-tag-select
                                         (aset (r/dom-node this) "onsubmit" on-tag-select)))
                :component-did-update (fn [this [_ prev-props]]
                                        ;; *1
                                        ;; as above + if props are CLJS we check for equality, and
                                        ;; pass additional argument to force tags rerender if required
                                        (let [{new-tags     :tags
                                               selected     :selected
                                               collapsable? :collapsable?} (r/props this)
                                              prev-tags                    (:tags prev-props)
                                              prev-selected                (:selected prev-props)
                                              tags-changed?                (and (seq? new-tags) (seq? prev-tags)
                                                                                (not= new-tags prev-tags))
                                              selected-emptied?            (and (empty? selected)
                                                                                (seq prev-selected))
                                              force-render                 (or tags-changed? selected-emptied?)]
                                          (when new-tags (init-tags!
                                                           {:id           id
                                                            :tags         new-tags
                                                            :force-render force-render
                                                            :selected     selected
                                                            :collapsable? collapsable?
                                                            :tags-url     tags-url
                                                            :show-icons   show-icons}))))
                :reagent-render       render})
       :clj (render opts))))

(defn- sanitize-select-options
  [vals]
  (mapv #(if (string? %) {:id % :label %} %) vals))

(defn select-input
  [value {:keys [options on-change disabled on-change-events class class-wrapper data-test]
          :or   {class-wrapper "select"}}]
  (let [options (sanitize-select-options options)]
    [:div {:class class-wrapper}
     (into
       [:select
        (merge {:value     (str (util/index-of (mapv :id options) value))
                :class     class
                :data-test data-test}
               (cond
                 (and on-change on-change-events)
                 (interop-forms/multiple-on-change
                   (interop-forms/on-change-fn on-change)
                   (interop-forms/dispatch-events-on-change on-change-events))

                 on-change
                 (interop-forms/on-change-fn on-change)

                 on-change-events
                 (interop-forms/dispatch-events-on-change on-change-events))
               (when disabled {:disabled true}))]
       (map-indexed (fn [i {:keys [label id]}]
                      [:option (merge {:value (str i)}
                                      #?(:clj (when (= id value)
                                                {:selected true}))) label])
                    options))]))

(defn select-field
  [{:keys [dirty? force-error? value] :as options}]
  (let [field-options (cond-> options
                              (not (or dirty? force-error?))
                              (dissoc :error :data-test)
                              :always
                              (assoc :class (:class-container options)))]
    (field-container field-options
                     (select-input value options))))

#?(:cljs
   (defn target-checked [^js/Event ev]
     (-> ev .-target .-checked)))

(defn labelled-checkbox
  "Like checkbox-field, but with a different markup."
  [{:keys [value label disabled? indeterminate? on-change class label-class] :as options}]
  (let [id (or (:id options) (name (gensym)))]
    [:div (util/smc
            "checkbox"
            class
            (when disabled? "checkbox--disabled")
            (when indeterminate? "checkbox--indeterminate"))
     [:input
      (merge
        {:type "checkbox"
         :id   id}
        (when disabled?
          {:disabled true})
        (when #?(:clj value :cljs true)
          {:checked value})
        (when on-change
          (interop-forms/on-change-fn
            #?(:clj on-change
               :cljs #(when (not disabled?)
                        (if (fn? on-change)
                          (on-change %)
                          (events/dispatch-sync (conj on-change (target-checked %)))))))))]
     [:label {:for   id
              :class label-class}
      [:div {:class "checkbox__box"}]
      label]]))

(defn multiple-checkboxes
  "Like multiple-buttons, but renders a set of labelled checkboxes."
  [multi-value {:keys [options on-change class-wrapper item-class label-class selected-class]
                :or   {class-wrapper "multiple-checkboxes"}}]
  (into [:div {:class class-wrapper}]
        (map
          (fn [{:keys [value label disabled] :as option}]
            (let [selected? (contains? multi-value value)]
              [labelled-checkbox
               (merge {:value       selected?
                       :label       label
                       :class       (util/mc item-class
                                             [selected? selected-class])
                       :label-class label-class
                       :disabled    disabled}
                      ;; for CLJS we expect function handler, or vector with event name
                      ;; for CLJ we expect string handler, or function returning string handler
                      (interop-forms/on-change-fn
                        #?(:cljs (if (vector? on-change)
                                   (conj on-change value)
                                   on-change)
                           :clj (if (fn? on-change)
                                  (on-change value)
                                  on-change))))]))
          options)))

(defn fake-radio-buttons
  [value data-list options]
  [:div.radios {:data-test (:data-test options)}
   (for [{:keys [id label href]} data-list]
     ^{:key id}
     [:a
      {:class
             (util/mc "radio"
                      (when (= id value)
                        "radio--checked"))
       :href href
       :id   id}
      (when (= id value)
        [:div.radio__checked])
      [:div.radio__label label]])])

(defn radio-buttons
  "Renders a set of radio buttons wrapped together in a div."
  [value {options :options name- :name on-change :on-change class :class}]
  (let [options (sanitize-select-options options)
        name-   (or name- (name (gensym)))]
    (into
      [:div (util/smc "radios" class)]
      (for [[i {:keys [id label]}] (map-indexed vector options)
            :let                   [el-id (str name- i)]]
        [:label.radio-item
         {:for el-id}
         [:input (merge {:id   el-id
                         :type "radio"
                         :name name-}
                        (when (= value id)
                          {:checked true})
                        (when on-change
                          #?(:cljs {:on-change (if (fn? on-change)
                                                 #(on-change id)
                                                 #(events/dispatch (conj on-change id)))}
                             :clj {:onchange (if (fn? on-change)
                                               (on-change id)
                                               on-change)})))]
         [:div.circle]
         label]))))

(defn radio-field
  "A selection of radio controls + label"
  [value args]
  (field-container args (radio-buttons value args)))
