(ns wh.common.numbers)

(defn parse-int
  "Parse a string as integer, returning nil if it wasn't possible."
  [s]
  #?(:clj (try
            (Integer/parseInt s)
            (catch Exception _ nil))
     :cljs (when (and (string? s) (re-find #"^[-+]?\d+$" s))
             (let [res (js/parseInt s)]
               (when-not (js/isNaN res)
                 res)))))

(defn coerce-int
  "Try to coerce value to int. Created to handle GraphQL variables
  that may come as strings but have to be coerced to int."
  [s]
  ;; In Clojure only numbers can be converted to integer with (int…)
  ;; Strings have to be parsed with parseInt
  #?(:clj (cond
            (number? s) (int s)
            :else (try
                    (Integer/parseInt s)
                    (catch Exception _ nil)))
     ;; JS parseInt is ok with strings and numbers passed as arguments
     :cljs (let [res (js/parseInt s)]
             (when-not (js/isNaN res)
               res))))
