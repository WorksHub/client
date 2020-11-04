(ns wh.components.button-auth
  (:require [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.util :as util]))

(defn add-onclick [params on-click]
  (cond-> params
          on-click (merge (interop/on-click-fn on-click))))

(defn add-id [params id]
  (cond-> params
          id (merge {:id id})))

(def type->icon
  {:github "github"
   :stackoverflow "stackoverflow-with-colors"
   :twitter "twitter"
   :email-signin "mail"
   :email-signup "mail"})

(def type->href
  {:github (routes/path :login :params {:step :github})
   :stackoverflow (routes/path :login :params {:step :stackoverflow})
   :twitter (routes/path :login :params {:step :twitter})
   :email-signin (routes/path :login :params {:step :email})
   :email-signup (routes/path :get-started)})

(def type->class
  {:github "button--github"
   :stackoverflow "button--stackoverflow"
   :twitter "button--twitter"
   :email-signin nil
   :email-signup nil})

(def type->class-icon
  {:twitter "button__icon--twitter"})

(def type->default-text
  {:github        "Start with GitHub"
   :stackoverflow "Start with Stack Overflow"
   :twitter       "Start with Twitter"
   :email-signin  "Login with Email"
   :email-signup  "Start with Email"})

(defn button
  ([type]
   (button type nil))
  ([type
    {:keys [class text on-click inverted? id]}]
   (let [text (or text (type->default-text type))
         href (type->href type)
         class (util/merge-classes
                 "button button--large button--auth"
                 (type->class type)
                 (when inverted? "button--inverted")
                 class)
         class-icon (util/merge-classes
                      "button__icon"
                      (type->class-icon type))
         icon-name (type->icon type)]
     [:a
      (-> {:href href
           :class class}
          (add-onclick on-click)
          (add-id id))
      [icon icon-name :class class-icon]
      text])))

(def type->connect-text
  {:github        "Connect GitHub"
   :stackoverflow "Connect Stack Overflow"
   :twitter       "Connect Twitter"})

(def type->icon-class
  {:github "button__icon"
   :stackoverflow "button__icon"
   :twitter "button__icon button__icon--twitter"})

(defn connect-button
  [type]
  [:a
   (-> {:href (type->href type)
        :class (util/merge-classes
                 "button button--connect"
                 (type->class type))})
   [icon (type->icon type) :class (type->icon-class type)]
   (type->connect-text type)])
