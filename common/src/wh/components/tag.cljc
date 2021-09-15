(ns wh.components.tag
  (:require [clojure.string :as str]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.util :as util]))

(defn ->tag [m]
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
  ([element-type t]
   [tag element-type t nil])
  ([element-type
    {:keys [label type subtype id icon on-click href
            with-icon? inverted? interactive? server-side-invert-on-click?]
     :or   {with-icon? true}
     :as   t}
    {:keys [class] :as _opts}]
   (let [label-lc      (or (some-> label str/lower-case) "")
         standard-icon (when (labels-with-icons label-lc)
                         (str label-lc "-tag"))
         icon'         (or icon standard-icon)]
     [element-type
      (merge {:key        id
              :data-label label
              :data-test  "tag"
              :class      (util/merge-classes "tag"
                                              (when class class)
                                              (when inverted? "tag--inverted")
                                              (when interactive? "tag--interactive")
                                              (when (= :button element-type) "tag--button")
                                              (str "tag--type-" (if t (name type) "skeleton"))
                                              (when-not t "tag--skeleton")
                                              (when subtype (str "tag--subtype-" (name subtype))))}
             (when href {:href href})
             (when on-click
               (interop/on-click-fn #?(:cljs on-click
                                       :clj  (cond-> on-click
                                               server-side-invert-on-click? add-invert)))))
      (when (and icon' with-icon?)
        [icons/icon icon'])
      [:span label]])))

(defn- skeleton-tags
  [{:keys [skeleton-tags-n]
    :or   {skeleton-tags-n 6}
    :as   _opts}]
  (map (fn [i]
         {:label   (apply str (repeat (+ 8 (rand-int 30)) " "))
          :key     i
          :type    :tech
          :subtype :software
          :slug    ""})
       (range skeleton-tags-n)))

(defn tag-list
  ([element-type tags]
   [tag-list element-type tags nil])
  ([element-type tags {:keys [skeleton? class-wrapper] :as opts}]
   (let [tags' (if skeleton? (skeleton-tags opts) tags)]
     (when (not-empty tags')
       (->> tags'
            (map (fn [t] [tag element-type t opts]))
            (into [:ul
                   {:class     (util/mc "tags" "tags--inline" "tags--profile" class-wrapper)
                    :data-test "tags-list"}]))))))

(defn strs->tag-list [element-type strs {:keys [f]
                                         :or   {f identity}}]
  (tag-list
    element-type
    (map #(f (hash-map
               :label %
               :type "tech"))
         strs)))

(def default-min-selected-tags 20)

(defn select-tags
  [tag-search tags {:keys [include-ids size type subtype]}]
  {:pre [(or (nil? include-ids) (set? include-ids))
         (or (nil? subtype) (and (some? type) (some? subtype)))]}
  (let [tag-search    (-> (if (nil? type)
                            tag-search
                            (get-in tag-search [type subtype]))
                          (or "")
                          (str/lower-case))
        type-matches? (fn [tag]
                        (or (nil? type)
                            (and (= type (:type tag))
                                 (or (nil? subtype)
                                     (= subtype (:subtype tag))))))
        id-included?  (fn [tag] (contains? include-ids (:id tag)))
        matching-but-not-selected
                      (filter (fn [tag]
                                (and (or (str/blank? tag-search)
                                         (str/includes? (str/lower-case (:label tag))
                                                        tag-search))
                                     (type-matches? tag)
                                     (not (id-included? tag))))
                              tags)
        selected-tags (->> tags
                           (filter id-included?)
                           (map #(assoc % :selected true)))
        tags-to-take  (+ (or size default-min-selected-tags) (count include-ids))]
    (take tags-to-take (concat selected-tags matching-but-not-selected))))
