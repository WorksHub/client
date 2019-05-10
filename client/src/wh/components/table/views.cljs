(ns wh.components.table.views
  (:require [re-frame.core :refer [dispatch]]
            [wh.subs :refer [<sub]]))

(defn default-header-renderer
  [{:keys [id label]} options]
  [:th label])

(defn- header
  [columns {:keys [header-renderer] :as options}]
  [:thead
   (into [:tr]
         (for [column columns]
           [header-renderer column options]))])

(defn default-cell-renderer
  [row id options]
  [:td (get row id)])

(defn default-row-renderer
  [columns row {:keys [cell-renderer] :as options}]
  (into [:tr]
        (for [{:keys [id] :as col} columns]
          ((or (:cell-renderer col) cell-renderer)
           row id options))))

(defn- body
  [columns data options]
  (let [default-options {:row-renderer default-row-renderer
                         :cell-renderer default-cell-renderer
                         :header-renderer default-header-renderer}
        options (merge default-options options)]
    (into [:tbody]
          (map (fn [row]
                 [(:row-renderer options) columns row options])
               data))))

(defn table
  [columns data {:keys [class] :as options}]
  [:table.table {:class class}
   [header columns options]
   [body columns data options]])

;; Sortable table

(defn- next-sort-dir [dir]
  (if (= dir :asc)
    :desc
    :asc))

(defn sortable-table-header-renderer
  [{:keys [id label]}
   {:keys [sorted on-sort] :as options}]
  (let [sort-dir (<sub (conj sorted id))]
    [:th
     {:on-click #(dispatch (conj on-sort id (next-sort-dir sort-dir)))}
     (str (case sort-dir
            :asc "▲ "
            :desc "▼ "
            "")
          label)]))

(defn sortable-table
  [columns data options]
  (table columns data
         (merge {:header-renderer sortable-table-header-renderer}
                options)))
