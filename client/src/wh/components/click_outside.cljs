(ns wh.components.click-outside
  (:require [reagent.core :as r]))

(defn click-outside [{:keys [element on-click-outside] :as opts} & children]
  (r/with-let [ref     (r/atom nil)
               handler (fn [e] (when-not (.contains @ref (.-target e))
                                 (and on-click-outside (on-click-outside))))
               _       (.addEventListener js/document "click" handler)
               element (or element :div)]
    [element (-> opts
                 (dissoc :element :on-click-outside)
                 (assoc :ref #(reset! ref %)))
     children]

    (finally
      (.removeEventListener js/document "click" handler))))
