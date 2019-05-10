(ns wh.components.stats.views
  (:require
    [reagent.core :as r]))

(defn stats-item-dummy [_args]
  [:div.stats__item.stats__item--dummy])

(def stats-item-impl (r/atom stats-item-dummy))

(defn stats-item [args]
  (@stats-item-impl args))
