(ns wh.user.core
  (:require [cljs.loader :as loader]
            [wh.user.events]
            [wh.user.subs]
            [wh.user.views]))

(loader/set-loaded! :user)
