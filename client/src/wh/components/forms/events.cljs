(ns wh.components.forms.events
  (:require [clojure.string :as str]))

(defn multi-edit-fn
  "Helper for multi-edit events."
  [k & path]
  (fn [db [i value]]
    (let [old-values (k db)
          new-values (assoc-in old-values (into [i] path) value)
          new-values (vec (remove #(str/blank? (get-in % path)) new-values))]
      (assoc db k new-values))))
