(ns wh.components.activities.issue-started
  (:require [wh.common.text :as text]
            [wh.common.url :as url]
            [wh.components.activities.components :as components]
            [wh.components.common :refer [link wrap-img img base-img]]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]))

(defn image [{:keys [feature] :as blog}]
  (when feature
    [:div (util/smc styles/article__feature)
     (wrap-img img feature {:w 578 :h 154 :class styles/article__feature-img :crop "center"})]))

(defn issue-contributor-component [{:keys [name image-url] :as user}]
  [:div (util/smc styles/issue-contributor)
   (wrap-img img image-url {:w 48 :h 48 :crop "center" :class styles/issue-contributor__avatar})
   [:span (util/smc styles/issue-contributor__title)
    (if (text/not-blank name)
      [:span (util/smc styles/issue-contributor__title styles/issue-contributor__title--bold) name]
      [:span (util/smc styles/issue-contributor__title) "Someone "])
    [:span "has just started to work on an"]
    [:span (util/smc styles/issue-contributor__title styles/issue-contributor__title--bold) "open source issue!"]]
   [:img {:src   "/images/feed/issue.svg"
          :class styles/issue-contributor__img}]])

(defn issue-company-component [{:keys [logo name] :as company}]
  [:div (util/smc styles/issue-company)
   (wrap-img img logo {:w 25 :h 25 :crop "center" :class styles/issue-company__logo})
   [:span (util/smc styles/issue-company__name) name]])

(defn inner-card [{:keys [id title issue-company repo compensation] :as issue}]
  [:a
   {:class (util/mc styles/inner-card styles/inner-card--started-issue)
    :href (routes/path :issue :params {:id id})}
   [:div (util/smc styles/issue-tags__wrapper)
    [issue-company-component issue-company]
    [:div (util/smc styles/issue-tags)
     [components/compensation-amount compensation]
     [components/primary-language repo]]]
   [:div (util/smc styles/title)
    title]])

(defn card
  [{:keys [id issue-contributor title] :as issue} type
   {:keys [base-uri vertical facebook-app-id]}]
  [components/card type
   [issue-contributor-component issue-contributor]
   [inner-card issue]
   [components/footer :default
    (let [url (str (url/strip-path base-uri)
                   (routes/path :issue :params {:id id}))]
      [components/actions
       {:share-opts {:url             url
                     :id              id
                     :content-title   title
                     :content         "this Open Source issue getting some attention over"
                     :vertical        vertical
                     :facebook-app-id facebook-app-id}}])
    [components/footer-buttons
     [components/button
      {:href (routes/path :issues)
       :type :inverted}
      "All issues"]
     [components/button
      {:href (routes/path :issue :params {:id id})}
      "View issue"]]]])
