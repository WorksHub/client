(ns wh.components.activities.issue-published
  (:require [clojure.string :as str]
            [wh.common.data :refer [currency-symbols]]
            [wh.components.activities.components :as components]
            [wh.components.tag :as tag]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn tag [tag]
  [tag/tag :div tag])

(defn issue->status [{:keys [status pr-count contributors] :as issue}]
  (if (= :open (keyword status))
    (cond
      (and pr-count (pos? pr-count)) "submitted"
      (and contributors (pos? (count contributors))) "started"
      :else "open")
    "closed"))

(defn currency-symbol [{:keys [compensation] :as issue}]
  (some-> compensation
          :currency
          name
          currency-symbols))

(defn details [{:keys [id title contributors-count level pr-count repo compensation] :as issue}]
  [components/inner-card
   [components/title
    {:href (routes/path :issue :params {:id id})
     :type :medium}
    title]
   [:div (util/smc styles/issue__meta-row-wrapper)
    [components/meta-row
     [components/text-with-icon {:icon "issue-state"} (-> issue
                                                          issue->status
                                                          str/capitalize)]
     [components/text-with-icon {:icon "git"} pr-count]
     [components/text-with-icon {:icon "couple"} contributors-count]
     [components/text-with-icon {:icon (str "issue-level-" (name level))} (-> level
                                                                              name
                                                                              str/capitalize)]
     [:div (util/smc styles/issue__tag-primary-language)
      [tag {:label (:primary-language repo)
            :type  "tech"}]]
     (let [amount (or (:amount compensation) 0)]
       (when-not (zero? amount)
         [tag {:label (str (currency-symbol compensation) amount)
               :type  "funding"}]))]]])

(defn card [{:keys [id body] :as issue} type]
  [components/card type
   [components/header
    [components/company-info (:issue-company issue)]
    [components/entity-icon "git" type]]
   [components/description {:type :cropped} body]
   [details issue]
   [components/footer :default
    [components/footer-buttons
     [components/button
      {:href (routes/path :issues)
       :type :inverted}
      "All issues"]
     [components/button
      {:href (routes/path :issue :params {:id id})}
      "View Issue"]]]])
