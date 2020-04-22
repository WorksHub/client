(ns wh.components.button-auth
  (:require [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.util :as util]
            [wh.routes :as routes]))

(defn add-onclick [params on-click]
  (cond-> params
          on-click (merge (interop/on-click-fn on-click))))

(defn add-id [params id]
  (cond-> params
          id (merge {:id id})))

(def type->icon
  {:github "github"
   :stackoverflow "stackoverflow-with-colors"
   :email-signin "mail"
   :email-signup "mail"})

(def type->href
  {:github (routes/path :login :params {:step :github})
   :stackoverflow (routes/path :login :params {:step :stackoverflow})
   :email-signin (routes/path :login :params {:step :email})
   :email-signup (routes/path :get-started)})

(def type->class
  {:github "button--github"
   :stackoverflow "button--stackoverflow"
   :email-signin nil
   :email-signup nil})

(def type->default-text
  {:github "Start with Github"
   :stackoverflow "Start with Stack Overflow"
   :email-signin "Login with Email"
   :email-signup "Start with Email"})

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
         icon-name (type->icon type)]
     [:a
      (-> {:href href
           :class class}
          (add-onclick on-click)
          (add-id id))
      [icon icon-name :class "button__icon"]
      text])))