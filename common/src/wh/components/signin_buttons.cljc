(ns wh.components.signin-buttons
  (:require [wh.components.icons :refer [icon]]
            [wh.routes :as routes]
            [wh.styles.signin-buttons :as style]
            [wh.util :as util]))

(defn github
  ([]
   [github {:text "Github"}])
  ([{:keys [text type data-test]}]
   [:a {:class (util/mc
                 style/button
                 style/button--github
                 [(= type :auth) style/button--auth])
        :href  (routes/path :login :params {:step :github})
        :data-test data-test}
    [icon "github" :class style/icon]
    [:span text]]))

(defn stack-overflow
  ([]
   [stack-overflow {:text "Stack Overflow"}])
  ([{:keys [text type]}]
   [:a {:class (util/mc
                 style/button
                 style/button--stackoverflow
                 [(= type :auth) style/button--auth])
        :href  (routes/path :login :params {:step :stackoverflow})}
    [icon "stackoverflow" :class (util/mc style/icon style/icon--stackoverflow)]
    [:span text]]))

(defn twitter
  ([]
   [twitter {:text "Twitter"}])
  ([{:keys [text type]}]
   [:a {:class (util/mc
                 style/button
                 style/button--twitter
                 [(= type :auth) style/button--auth])
        :href  (routes/path :login :params {:step :twitter})}
    [icon "twitter" :class (util/mc style/icon style/icon--twitter)]
    [:span text]]))

(defn email
  ([]
   [email {:text "Email"}])
  ([{:keys [text]}]
   [:a {:class (util/mc style/button)
        :href  (routes/path :login :params {:step :email})}
    [icon "mail" :class (util/mc style/icon style/icon--email)]
    [:span text]]))
