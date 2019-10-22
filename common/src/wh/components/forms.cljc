(ns wh.components.forms
  (:require [wh.interop :as interop]
            [wh.interop.forms :as interop-forms]
            [wh.util :as util]))

;; NOTE we will start moving CLJS form components into here
;; although they will probably operate quite differently

(defn field-container
  "A generic container for a field. Wraps one or more controls in a
  container widget. Provides support for labels and help messages."
  [{:keys [label class error help id inline? solo?]} & controls]
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
            (if (string? error)
              [:div.field__error.field--invalid error]
              [:div.field__error.field--invalid.is-invisible "errors go here"])))))


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
