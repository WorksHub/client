(ns wh.re-frame
  (:require
    #?(:cljs [reagent.core :as r])
    [re-frame.db :as r-db])
  (:refer-clojure :exclude [atom]))

(defn set-app-db!
  [app-db]
  (reset! r-db/app-db app-db))

(def atom
  #?(:cljs r/atom
     :clj clojure.core/atom))
