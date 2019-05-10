(ns wh.re-frame
  (:require [re-frame.db :as r-db]))

(defn set-app-db!
  [app-db]
  (reset! r-db/app-db app-db))
