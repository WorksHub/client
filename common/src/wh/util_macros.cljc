(ns wh.util-macros
  #?(:cljs (:require-macros [wh.util-macros :refer [when-not-prod]])))

#?(:clj
   (defmacro when-not-prod
     [& body]
     (when (not= (System/getenv "ENVIRONMENT") "prod")
       `(do
          ~@body))))
