(ns wh.components.forms
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [wh.components.icons :refer [icon]]
            [wh.components.tag :as tag]
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
  [{:keys [label class error help id inline? solo? hide-error?]} & controls]
  (vec
    (concat
      [:div
       {:id    id
        :class (util/merge-classes
                 "field"
                 (when (> (count controls) 1) "grouped")
                 (when solo? "wh-formx")
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
  [value {:keys [type on-change on-change-events on-input placeholder on-scroll rows dirty]
          :or {type :text}
          :as options}]
  [(if (= type :textarea)
     :textarea.textarea
     :input.input)
   (merge (when-not (= type :textarea) {:type (name type)})
          {:value value :placeholder placeholder}
          (if (and on-change on-change-events)
            (interop-forms/multiple-on-change
              (interop-forms/on-change-fn on-change)
              (interop-forms/dispatch-events-on-change on-change-events))
            (merge (when on-change
                     (interop-forms/on-change-fn on-change))
                   (when on-change-events
                     (interop-forms/dispatch-events-on-change on-change-events))))
          (when on-input
            (interop-forms/on-input-fn on-input))
          (when on-scroll
            {:on-scroll #(events/dispatch on-scroll)})
          (select-keys options [:on-focus :on-blur :auto-complete :disabled :read-only :on-key-press
                                :step])
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
      (let [value            value
            suggestable?     (and (or (seq suggestions) on-select-suggestion) (not hide-icon?))
            show-suggestion? (and (seq suggestions) @focused (not read-only))
            removable?       (and on-remove value (not @focused))]
        (field-container (merge options
                                {:error (when (and (string? error) force-error?)
                                          error)})
                         [:div.text-field-control
                          {:class (str (when show-suggestion? "text-field-control--showing-suggestions")
                                       (when suggestable? " text-field-control--suggestable")
                                       (when removable? " text-field-control--removable"))}
                          (text-input value options)
                          (when suggestable? [icon "search-new" :class "search-icon"])
                          #_(when show-suggestion?
                              [suggestions-list suggestions options]) ;; TODO Fix suggestable on SSRR text fields
                          (when removable?
                            [icon "close" :class "remove-text-field-btn" :on-click #(events/dispatch-sync on-remove)])])))))

(defn tags-field
  [{:keys [on-change] :as opts}]
  (let [dirty   (r/atom false)
        focused (r/atom false)
        text    (r/atom "")
        id      (or (:id opts) (str (gensym "tags-field")))
        render  (fn [{:keys [collapsed? error label placeholder dirty? read-only empty-label solo? tags
                             on-change on-tag-click on-toggle-collapse on-add-tag init-from-js-tags? hide-error?]}]
                  (when (and (not (nil? dirty?))
                             (boolean? dirty?))
                    (reset! dirty dirty?))
                  (let [error (and @dirty (not @focused) error)]
                    [:div
                     (merge {:id    id
                             :class (util/merge-classes
                                      "tags-container tags-container--collapsed tags-container--alternative"
                                      (when init-from-js-tags? "tags-container--wants-js-tags")
                                      (when (string? error) "tags-container--errored"))}
                            (when on-change
                              (interop-forms/on-change-fn on-change)))
                     [text-field
                      (merge
                        {:value        @text
                         :label        label
                         :solo?        solo?
                         :class        "tags-text-input"
                         :placeholder  placeholder
                         :hide-error?  true
                         :on-key-press (when (and on-add-tag (not (str/blank? @text)))
                                         #(if (= (.-key %) "Enter")
                                            (on-add-tag @text)))
                         :read-only    read-only
                         :on-input     (interop-forms/filter-tags id text)}
                        (when (string? error)
                          {:force-error? true
                           :error        error}))]
                     (when (and on-add-tag (not (str/blank? @text)))
                       [:div.tag-add
                        {:on-click #(on-add-tag @text)
                         :class    (when label "tag-add--with-label")}
                        [icon "plus"]])
                     [:div.tags-selection
                      [:div
                       (interop/toggle-class-on-click id "tags-container--collapsed")
                       [icon "roll-down"
                        :class "tags-roll"]]
                      [:div.tags-selection--tags-container
                       [:ul.tags.tags--top-level.tags--selected]
                       [:ul.tags.tags--top-level.tags--unselected
                        [:div.tags-loading "Loading..."]]]]
                     (when-not hide-error?
                       (if (string? error)
                         [:div.field__error.field--invalid error]
                         [:div.field__error.field--invalid.is-invisible "errors go here"]))]))]
    #?(:cljs (r/create-class
               {:component-did-mount (fn [this]
                                       (js/initTags (js/document.getElementById id))
                                       (when on-change
                                         (set! (.-onchange (r/dom-node this)) on-change)))
                :reagent-render      render})
       :clj (render opts))))

(defn- sanitize-select-options
  [vals]
  (mapv #(if (string? %) {:id % :label %} %) vals))

(defn select-input
  [value {:keys [options on-change on-change-events disabled]}]
  (let [options (sanitize-select-options options)]
    [:div.select
     (into
       [:select
        (merge {:value (str (util/index-of (mapv :id options) value))}
               (if (and on-change on-change-events)
                 (interop-forms/multiple-on-change
                   (interop-forms/on-change-fn on-change)
                   (interop-forms/dispatch-events-on-change on-change-events))
                 (merge (when on-change
                          (interop-forms/on-change-fn on-change))
                        (when on-change-events
                          (interop-forms/dispatch-events-on-change on-change-events))))
               (when disabled {:disabled true}))]
       (map-indexed (fn [i {:keys [label id]}]
                      [:option (merge {:value (str i)}
                                      #?(:clj (when (= id value)
                                                {:selected true}))) label])
                    options))]))

(defn select-field
  [{:keys [dirty? force-error? value] :as options}]
  (field-container (if (or dirty? force-error?)
                     options
                     (dissoc options :error))
                   (select-input value options)))

(defn labelled-checkbox
  "Like checkbox-field, but with a different markup."
  [{:keys [value label disabled? indeterminate? on-change class label-class] :as options}]
  (let [id (or (:id options) (name (gensym)))]
    [:div.checkbox {:class (util/merge-classes
                             class
                             (when disabled? "checkbox--disabled")
                             (when indeterminate? "checkbox--indeterminate"))}
     [:input
      (merge
        {:type "checkbox"
         :id id}
        (when disabled?
          {:disabled true})
        (when #?(:clj value :cljs true)
          {:checked value})
        (when on-change
          (interop-forms/on-change-fn on-change)))]
     [:label {:for id
              :class label-class}
      [:div {:class "checkbox__box"}]
      label]]))
