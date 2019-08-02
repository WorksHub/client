(ns wh.components.stats.views
  #?(:cljs (:require
             [reagent.core :as r])))

(defn stats-item-dummy [_args]
  [:div.stats__item.stats__item--dummy])

#?(:cljs (def stats-item-impl (r/atom stats-item-dummy)))

#?(:cljs (defn stats-item [args]
           (@stats-item-impl args)))


