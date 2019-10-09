(ns wh.re-frame.subs
  (:require
    #?(:clj [taoensso.timbre :refer [error]])
    [re-frame.core :as re-frame]))

(defn <sub
  [c]
  (try
    (-> (re-frame/subscribe c)
        (deref))
    (catch #?(:clj Exception
              :cljs js/Error) e
      #?(:clj (do
                (error e "An error occurred whilst resolving the following subscription: " c)
                (throw e))
         :cljs (throw e)))))

;; The next three forms are needed for reg-sub-raw to work in clj.

#?(:clj (create-ns 'reagent.ratom))

#?(:clj
   (intern 'reagent.ratom 'make-reaction
           (fn make-reaction [f & [_args]]
             (reify clojure.lang.IDeref
               (deref [_] (f))))))

#?(:clj
   (defmacro reaction [& body]
     `(reagent.ratom/make-reaction
       (fn [] ~@body))))
