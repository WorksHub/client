(ns wh.re-frame.subs
  (:require
    [re-frame.core :as re-frame]))

(def <sub (comp deref re-frame/subscribe))

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
