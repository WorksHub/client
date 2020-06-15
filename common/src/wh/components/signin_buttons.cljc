(ns wh.components.signin-buttons
  (:require [wh.components.icons :refer [icon]]
            [wh.routes :as routes]
            [wh.styles.signin-buttons :as style]
            [wh.util :as util]))

(defn github []
  [:a {:class (util/mc style/button)
       :href  (routes/path :login :params {:step :github})}
   [icon "github" :class style/icon]
   [:span "GitHub"]])

(defn stack-overflow []
  [:a {:class (util/mc style/button)
       :href  (routes/path :login :params {:step :stackoverflow})}
   [icon "stackoverflow-with-colors" :class style/icon]
   [:span "Stack Overflow"]])

(defn twitter []
  [:a {:class (util/mc style/button)
       :href  (routes/path :login :params {:step :twitter})}
   [icon "twitter" :class (util/mc style/icon style/icon--twitter)]
   [:span "Twitter"]])

(defn email []
  [:a {:class (util/mc style/button)
       :href  (routes/path :login :params {:step :email})}
   [icon "mail" :class (util/mc style/icon style/icon--email)]
   [:span "Email"]])
