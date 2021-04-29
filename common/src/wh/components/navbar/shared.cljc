(ns wh.components.navbar.shared
  (:require [wh.routes :as routes]
            [wh.styles.navbar :as styles]))

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