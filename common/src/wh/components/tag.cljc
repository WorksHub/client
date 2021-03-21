(ns wh.components.tag
  (:require [clojure.string :as str]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.util :as util]))

(defn ->tag
  [m]
  (-> m
      (update :type keyword)
      (util/update* :subtype keyword)
      (cond-> (nil? (:subtype m)) (dissoc :subtype))))

(defn tag->form-tag
  [{:keys [id label type selected] :as tag}]
  (merge tag {:tag      label
              :key      id
              :class    (str "tag--type-" (name type))
              :selected (or selected false)}))

(def labels-with-icons #{"java" "docker" "graphql" "nodejs" "akka" "bitcoin"
                         "c++" "etherium" "git" "go"
                         "postgres" "python" "serverless" "typescript"
                         "haskell" "scala" "javascript" "clojure" "f#" "rust"
                         "c#" "elm" "aws" "erlang" "ocaml" "react" "elixir"})

(defn add-invert [on-click]
  (str on-click "invertTag(this);"))

(defn tag
  [element-type {:keys [label type subtype id icon on-click href
                        with-icon? inverted? interactive? server-side-invert-on-click?]
                 :or {with-icon? true}
                 :as t}]
  (let [label-lc (or (some-> label str/lower-case) "")
        standard-icon (when (labels-with-icons label-lc)
                        (str label-lc "-tag"))
        icon' (or icon standard-icon)]
    [element-type
     (merge {:key        id
             :data-label label
             :class      (util/merge-classes "tag"
                                             (when inverted? "tag--inverted")
                                             (when interactive? "tag--interactive")
                                             (when (= :button element-type) "tag--button")
                                             (str "tag--type-" (if t (name type) "skeleton"))
                                             (when-not t "tag--skeleton")
                                             (when subtype (str "tag--subtype-" (name subtype))))}
            (when href {:href href})
            (when on-click
              (interop/on-click-fn #?(:cljs on-click
                                      :clj (cond-> on-click
                                                   server-side-invert-on-click? add-invert)))))
     (when (and icon' with-icon?)
       [icons/icon icon'])
     [:span label]]))

(defn tag-list
  [element-type tags]
  (when (not-empty tags)
    (->> tags
         (sort-by #(get % :weight 0) >)
         (map (fn [t] [tag element-type t]))
         (into [:ul.tags.tags--inline.tags--profile
                {:data-test "tags-list"}]))))

(defn strs->tag-list [element-type strs {:keys [f]
                                         :or   {f identity}}]
  (tag-list
    element-type
    (map #(f (hash-map
               :label %
               :type "tech"))
         strs)))
