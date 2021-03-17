(ns wh.re-frame
  (:require #?(:cljs [reagent.core :as r])
            [re-frame.db :as r-db])
  (:refer-clojure :exclude [atom]))

(defn set-app-db!
  [app-db]
  (reset! r-db/app-db app-db))

(def atom
  #?(:cljs r/atom
     :clj clojure.core/atom))

(def props
  #?(:cljs r/props
     :clj identity))

(def dom-node
  #?(:cljs r/dom-node
     :clj identity))

#?(:clj
   (defn next-tick
     [f] (f))
   :cljs
   (def next-tick r/next-tick))

#?(:clj
   (defn create-class
     [m] (:reagent-render m))
   :cljs
   (def create-class r/create-class))
