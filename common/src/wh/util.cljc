(ns wh.util
  (:require #?(:cljs [goog.i18n.NumberFormat :as nf])
            [camel-snake-kebab.core :as c]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]])
  (:refer-clojure :exclude [random-uuid]))

#?(:clj (defn random-uuid []
          (str (java.util.UUID/randomUUID)))
   :cljs (def random-uuid cljs.core/random-uuid))

(defn dissoc-selected-keys-if-blank
  "If a key in the provided set is nil or blank, remove it entirely."
  [m ks]
  (into {} (remove (fn [[k v]] (and (contains? ks k)
                                    (or (nil? v) (and (string? v) (str/blank? v))))))
        m))

(defn remove-where
  "Removes the map entry if the value satisfies predicate.
  Also walks into nested maps"
  [m pred?]
  (postwalk
   (fn [x] (if (map? x)
             (into {} (remove (fn [[k v]]
                                (pred? v))) x)
             x)) m))

(defn remove-nils
  "Removes the map entry if the value for the key is nil. Also walks into nested maps"
  [m]
  (remove-where m nil?))

(defn remove-nil-or-blank
  "Removes the map entry if the value is nil, blank string.
  Also walks into nested maps"
  [m]
  (remove-where
   m #(or (nil? %)
          (and (string? %) (str/blank? %)))))

(defn remove-nil-blank-or-empty
  "Removes the map entry if the value is nil, blank string or empty collection.
  Also walks into nested maps"
  [m]
  (remove-where
   m
   #(or (nil? %)
        (and (string? %) (str/blank? %))
        (and (coll? %) (empty? %)))))

(s/fdef remove-nils
  :args (s/cat :m map?)
  :ret map?
  :fn (fn [{ret :ret}]
        (letfn [(all-values [m]
                  (reduce-kv (fn [a k v] (if (map? v) (concat a (all-values v)) (conj a v))) [] m))]
          (not (some nil? (all-values ret))))))

(defn index-of
  "Returns the zero-based position of x in coll, or nil
  if coll doesn't contain x."
  [coll x]
  (loop [idx 0 items coll]
    (cond
      (empty? items) nil
      (= x (first items)) idx
      :else (recur (inc idx) (rest items)))))

(defn toggle
  "If set contains option, removes it, else adds it.
  Creates a new set if none supplied"
  [coll option]
  (if coll
    ((if (contains? (set coll) option) disj conj) (set coll) option)
    #{option}))

(defn toggle-by-id
  "If set contains option with matching :id key, removes it, else adds it.
  Creates a new set if none supplied"
  [coll option]
  (if coll
    (let [candidate (some #(when (= (:id %) (:id option)) %) coll)]
      ((if candidate disj conj) (set coll) (or candidate option)))
    #{option}))

(defn toggle-unless-empty
  "Like toggle, but returns set unchanged if the result
  would otherwise be empty."
  [coll option]
  (if (= coll #{option})
    coll
    (toggle coll option)))

(defn gql-errors->error-key
  [error-resp]
  (or (some-> error-resp
              :errors
              first
              :extensions
              :key
              keyword)
      :unknown-error))

(defn unflatten-map
  "Takes a flat map and inflate any keys that include nested indicator.
   e.g. `{:a 1 :b__c 2 :b__d 3}` => `{:a 1 :b {:c 2 :d 3}}`"
  ([m]
   (unflatten-map m "__"))
  ([m nest-indicator]
   (reduce
    (fn [a [k v]]
      (let [parts (str/split (name k) (re-pattern nest-indicator))]
        (if (= 1 (count parts))
          (assoc a k v)
          (update a (keyword (namespace k) (first parts)) assoc (keyword (last parts)) v)))) {} m)))

(defn flatten-map
  "Takes a map and deflates any nested maps keys using nested indicator.
   e.g. `{:a 1 :b {:c 2 :d 3}}` => `{:a 1 :b__c 2 :b__d 3}` => "
  ([m]
   (flatten-map m "__"))
  ([m nest-indicator]
   (reduce
    (fn [a [k v]]
      (if (map? v)
        (apply assoc
               (dissoc a k)
               (mapcat (fn [[k' v']]
                         [(keyword (str (name k) nest-indicator (name k'))) v']) v))
        a)) m m)))

(defn ->vec
  "Takes a value and either converts it to a vector or puts it into a vector
   (->vec 1)    ;; [1]
   (->vec [2])  ;; [2]
   (->vec '(3)) ;; [3]"
  [x]
  (cond (vector? x) x
        (coll? x) (into [] x)
        :else [x]))

(s/fdef ->vec
  :args (s/cat :value any?)
  :ret vector?
  :fn (fn [{ret :ret {value :value} :args}]
        (if (coll? value)
          (= (vec value) ret)
          (= [value] ret))))

(defn fix-order
  "Takes an ordered list of values, a key (k) and a list of maps (ms).
   Reassembles the list of maps using the ordered list of values, where `(get m k)` appears in the ordered list.
    (fix-order [4 3 2 1] :id [{:id 1} {:id 2} {:id 3} {:id 4}]) => [{:id 4} {:id 3} {:id 2} {:id 1}]"
  [ordered-ids k ms]
  (loop [output []
         ms     (set ms)
         ids    ordered-ids]
    (if-let [id (first ids)]
      (let [m (some #(when (= id (get % k)) %) ms)]
        (recur (if m (conj output m) output) (disj ms m) (rest ids)))
      output)))

(s/fdef fix-order
  :args (s/cat :ordered-ids (s/coll-of any?)
               :k any?
               :ms (s/coll-of map? :distinct true))
  :ret (s/coll-of map? :distinct true))

(defn list->sentence
  "Takes a collection and lists as a sentence. e.g. [:a :b :c] => \":a, :b and :c\""
  [coll]
  (let [x (first coll)]
    (cond
      (nil? (next coll))
      (str x)

      (= 1 (count (next coll)))
      (str x " and " (list->sentence (next coll)))

      (< 1 (count (next coll)))
      (str x ", " (list->sentence (next coll))))))


;; https://stackoverflow.com/a/26059795
(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn update-in*
  "Only calls `update-in` if key exists"
  [m ks f & args]
  (if (contains-in? m ks)
    (apply (partial update-in m ks f) args)
    m))

(defn update*
  "Only calls `update` if key exists"
  [m k f & args]
  (apply update-in* m [k] f args))

(defn update-vals
  "Update many values under many keys in map"
  [m ks & args]
  (reduce #(apply update % %2 args) m ks))

(defn merge-classes
  [& classes]
  (->> classes
       (map #(if (vector? %)
               (when (first %)
                 (second %))
               %))
       (remove nil?)
       (str/join " ")))

;; helper alias
(def mc merge-classes)

(defn smc [& classes]
  {:class (apply merge-classes classes)})

(defn ctx->ip [{:keys [remote-addr headers]}]
  (or (get headers "cf-connecting-ip")                      ;This header is added by Cloudflare and has original IP in prod
      (get headers "x-forwarded-for")                       ;This is filled by both Heroku and Cloudflare in prod (has 2 IPs in prod), in staging it will have original IP
      remote-addr))                                         ;This will have original IP when run out of Heroku and Cloudflare

(defn dissoc-if-empty
  "Checks (empty? (get m k)); if true, k is dissoc'd from m"
  [m k]
  (if (empty? (get m k))
    (dissoc m k)
    m))

(defn drop-ith
  [i coll]
  (vec (concat (subvec coll 0 i) (subvec coll (inc i)))))

(defn insert-at
  [coll el idx]
  (apply concat (interpose [el] (split-at idx coll))))

(defn trunc [n s]
  (when (pos-int? n)
    (->> (count s)
         (min n)
         (subs s 0))))

(s/fdef trunc
  :args (s/cat :n nat-int?
               :s string?)
  :ret (s/or :success string?
             :nil nil?))

(defn maps-with-id [n]
  (map (partial hash-map :id) (range n)))

(defn string->boolean [x]
  (= "true" x))

(defn fmap
  "applies f to each value inside a hashmap"
  [m f]
  (into {} (for [[k v] m] [k (f v)])))
