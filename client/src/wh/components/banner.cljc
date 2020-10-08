(ns wh.components.banner
  (:require [wh.components.common :refer [link]]
            [wh.styles.banner :as styles]
            [wh.util :as util]))

(def pages-with-banner #{:pricing :how-it-works :issues :homepage :homepage-not-logged-in})

(defn banner [{:keys [page logged-in?]}]
  (when (and (contains? pages-with-banner page)
             (not logged-in?))
    ;; is-open class is used in our js files
    [:div
     {:class (util/mc styles/banner "is-open")
      :id "promo-banner"}
     [link "Hiring? Sign up and post your first job for FREE!" :register-company :class styles/link]]))
