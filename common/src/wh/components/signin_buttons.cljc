(ns wh.components.signin-buttons
  (:require [wh.components.icons :refer [icon]]
            [wh.routes :as routes]
            [wh.styles.signin-buttons :as style]
            [wh.util :as util]))

(def type->href
  {:github        (routes/path :login :params {:step :github})
   :stackoverflow (routes/path :login :params {:step :stackoverflow})
   :twitter       (routes/path :login :params {:step :twitter})
   :email-signin  (routes/path :login :params {:step :email})
   :email-signup  (routes/path :register)})

(defn github
  ([]
   [github {:text      "GitHub"
            :data-test "signin-github"}])
  ([{:keys [text type data-test]}]
   [:a {:class     (util/mc
                     style/button
                     style/button--github
                     [(= type :auth) style/button--auth])
        :href      (routes/path :login :params {:step :github})
        :data-test data-test}
    [icon "github" :class style/icon]
    [:span text]]))

(defn stack-overflow
  ([]
   [stack-overflow {:text      "Stack Overflow"
                    :data-test "signin-stackoverflow"}])
  ([{:keys [text type data-test]}]
   [:a {:class     (util/mc
                     style/button
                     style/button--stackoverflow
                     [(= type :auth) style/button--auth])
        :href      (routes/path :login :params {:step :stackoverflow})
        :data-test data-test}
    [icon "stackoverflow" :class (util/mc style/icon style/icon--stackoverflow)]
    [:span text]]))

(defn twitter
  ([]
   [twitter {:text      "Twitter"
             :data-test "signin-twitter"}])
  ([{:keys [text type data-test]}]
   [:a {:class     (util/mc
                     style/button
                     style/button--twitter
                     [(= type :auth) style/button--auth])
        :href      (routes/path :login :params {:step :twitter})
        :data-test data-test}
    [icon "twitter" :class (util/mc style/icon style/icon--twitter)]
    [:span text]]))

(defn email
  ([]
   [email {:text      "Email"
           :data-test "signin-email"}])
  ([{:keys [text data-test]}]
   [:a {:class     (util/mc style/button)
        :href      (routes/path :login :params {:step :email})
        :data-test data-test}
    [icon "mail" :class (util/mc style/icon style/icon--email)]
    [:span text]]))

(defn employers []
  [:a {:class     (util/mc style/button style/button--auth)
       :href      (routes/path :register-company)
       :data-test "for-employers"}
   [icon "company-building" :class (util/mc style/icon)]
   [:span "For employers"]])
