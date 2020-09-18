(ns wh.components.activities.issue-published
  (:require [clojure.string :as str]
            [wh.common.url :as url]
            [wh.components.activities.components :as components]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn issue->status [{:keys [status pr-count contributors] :as issue}]
  (if (= :open (keyword status))
    (cond
      (and pr-count (pos? pr-count))                 "submitted"
      (and contributors (pos? (count contributors))) "started"
      :else                                          "open")
    "closed"))

(defn details [{:keys [id title contributors-count level pr-count repo compensation] :as issue} entity-type]
  [components/inner-card
   [components/title-with-icon
    [components/title
     {:href (routes/path :issue :params {:id id})
      :type :medium}
     title]
    [components/entity-icon "git" entity-type]]
   [:div (util/smc styles/issue__meta-row-wrapper)
    [components/meta-row
     [components/text-with-icon {:icon "issue-state"}
                                (-> issue issue->status str/capitalize)]
     [components/text-with-icon {:icon "git"} pr-count]
     [components/text-with-icon {:icon "couple"} contributors-count]
     [components/text-with-icon {:icon (str "issue-level-" (name level))}
                                (-> level name str/capitalize)]
     [components/primary-language repo]
     [components/compensation-amount compensation]]]])

(defn card
  [{:keys [id body title] :as issue} actor type
   {:keys [base-uri vertical facebook-app-id]}]
  [components/card type
   [components/header
    [components/company-info actor]
    [components/entity-description :issue type]]
   [components/description {:type :cropped} body]
   [details issue type]
   [components/footer :default
    (let [url (str (url/strip-path base-uri)
                   (routes/path :issue :params {:id id}))]
      [components/actions
       {:share-opts {:url             url
                     :id              id
                     :content-title   title
                     :content         (str "this " (if (= type :publish) "new " "") "Open Source issue from " (get-in issue [:issue-company :name]))
                     :vertical        vertical
                     :facebook-app-id facebook-app-id}}])
    [components/footer-buttons
     [components/button
      {:href (routes/path :issues)
       :type :inverted}
      "All issues"]
     [components/button
      {:href (routes/path :issue :params {:id id})}
      "View Issue"]]]])
