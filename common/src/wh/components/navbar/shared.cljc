(ns wh.components.navbar.shared
  (:require [wh.common.subs] ;; TODO: Isn't it necessary?
            [wh.components.icons :refer [icon]]
            [wh.components.navbar.styles :as styles]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.util :as util]))

(def articles-submenu-list
  [{:path       (routes/path :learn)
    :icon-name  "document"
    :icon-class styles/dropdown__link__icon-document
    :text       "All articles"}
   {:path       (routes/path :contribute)
    :icon-name  "plus-circle"
    :icon-class styles/dropdown__link__icon-plus
    :text       "Write an article"}
   {:path      (routes/path :liked-blogs)
    :icon-name "save"
    :text      "Saved articles"}])

(def articles-admin-submenu-list
  (conj articles-submenu-list
        {:path       (routes/path :admin-articles)
         :icon-name  "document-filled"
         :icon-class styles/dropdown__link__icon-document
         :text       "Unpublished articles"}))

(defn conversations-link [{:keys [class]}]
  (when (<sub [:wh/conversations-enabled?])
    [:a {:class (util/mc styles/nav-element styles/nav-element--conversations class)
         :href (routes/path :conversations)}
     [icon "message-circle"
      :class styles/nav-icon]]))
