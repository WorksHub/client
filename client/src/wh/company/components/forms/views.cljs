(ns wh.company.components.forms.views
  (:require [prop-types]
            ["react-quill" :as ReactQuill]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [reagent.core :as reagent]
            [wh.components.forms.views :refer [field-container text-field-error]]))

(def quill (reagent/adapt-react-class ReactQuill))

(defn rich-text-field
  "Like text-field, but renders a React Quill text area."
  [{:keys [label on-change on-blur on-focus placeholder]}]
  (let [dirty (reagent/atom false)
        focused (reagent/atom false)
        qid (name (gensym))]
    (fn [{:keys [dirty? value error validate force-error?] :as options}]
      (when (and (not (nil? dirty?))
                 (boolean? dirty?))
        (reset! dirty dirty?)
        ;; HACK we need this because quill doesn't fire proper blur
        ;; But it means we don't remove error on focus. Alternative is
        ;; no error shown when you immediately focus another element after quill
        (reset! focused false))
      (field-container
        (merge options
               {:error (if (and (string? error) force-error?)
                         error
                         (text-field-error value options dirty focused))
                :label label})
        [quill {:id qid
                :theme "snow"
                :bounds ".main"
                :modules (clj->js {:toolbar [[{:header  [2, 3, false]}]
                                             ["bold"] ["italic"] ["underline"] ["link"]
                                             [{:list "ordered"}] [{:list "bullet"}]
                                             ["image"] ["video"] ["code"]
                                             ["clean"]]})
                :value value
                :placeholder placeholder
                :on-change-selection #(if (nil? %) ;; nil == on-blur
                                        (do (reset! focused false)
                                            (when on-blur (on-blur %)))
                                        (do (reset! dirty true)
                                            (reset! focused true)
                                            (when on-focus (on-focus %))))
                :on-change #(do
                              (when dirty
                                (reset! dirty true))
                              (cond (vector? on-change)
                                    (dispatch-sync (conj on-change %))
                                    (fn? on-change)
                                    (on-change %)))}]))))
