(ns wh.promotions.create-promotion.components
  (:require [wh.routes :as routes]
            [wh.styles.promotions :as styles]
            [wh.util :as util]))

(defn promote-button [{:keys [id type class]}]
  [:a
   {:href      (routes/path :create-promotion :params {:type type :id id})
    :data-test (str "promote-" (name type))
    :class     (util/mc "button--promote" styles/button--promote class)}
   (str "Promote "
        (case type
          :article "Blog"
          :company "Company"
          :issue   "Issue"
          :job     "Job"
          ""))])
