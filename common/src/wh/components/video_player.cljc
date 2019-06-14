(ns wh.components.video-player
  (:require
    [clojure.string :as str]
    [wh.interop :as interop]))

(defn open-on-click
  [youtube-id]
  (interop/on-click-fn
   (interop/open-video-player youtube-id)))
