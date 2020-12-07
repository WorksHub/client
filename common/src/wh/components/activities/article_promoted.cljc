(ns wh.components.activities.article-promoted
  (:require [wh.components.activities.article :as article]
            [wh.components.activities.components :as components]
            [wh.styles.activities :as styles]))

(defn card [blog actor description type opts]
  [components/card type
   [components/promoter actor]

   [components/quoted-description {:class styles/quoted-description--article} description]

   [article/base-card blog (:author-info blog) type opts]])
