(ns wh.components.tag
  (:require
    [wh.components.icons :as icons]
    [wh.util :as util]
    [wh.interop :as interop]
    [clojure.string :as str]))

(defn ->tag
  [m]
  (-> m
      (update :type keyword)
      (util/update* :subtype keyword)
      (cond-> (nil? (:subtype m)) (dissoc :subtype))))

(defn tag->form-tag
  [{:keys [id label type] :as tag}]
  (merge tag {:tag      label
              :key      id
              :class    (str "tag--type-" (name type))
              :selected false}))

(def labels-with-icons #{"java" "docker" "graphql" "nodejs" "akka" "bitcoin"
                         "c++" "etherium" "git" "go"
                         "postgres" "python" "serverless" "typescript"
                         "haskell" "scala" "javascript" "clojure" "f#" "rust"
                         "c#" "elm" "aws" "erlang" "ocaml" "react" "elixir"})

(defn tag
  [element-type {:keys [label type subtype id icon on-click href] :as t}]
  (let [label-lc (str/lower-case label)
        standard-icon (when (labels-with-icons label-lc)
                        (str label-lc "-tag"))
        icon' (or icon standard-icon)]
    [element-type
     (merge {:key        id
             :data-label label
             :class      (util/merge-classes "tag"
                                             (str "tag--type-" (if t (name type) "skeleton"))
                                             (when subtype (str "tag--subtype-" (name subtype))))}
            (when href {:href href})
            (when on-click
              (interop/on-click-fn on-click)))
     (when icon'
       [icons/icon icon'])
     [:span label]]))

(defn tag-list
  [element-type tags]
  (when (not-empty tags)
    (into [:ul.tags.tags--inline.tags--profile]
          (map (fn [t] [tag element-type t]) tags))))

(defn strs->tag-list [element-type strs {:keys [f]
                                         :or   {f identity}}]
  (tag-list
    element-type
    (map #(f (hash-map
               :label %
               :type "tech"))
         strs)))
