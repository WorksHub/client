(ns wh.common.fx.tracking-pixels
  (:require [re-frame.core :refer [reg-fx]]
            [wh.common.tracking-pixels :as tracking-pixels]))

(reg-fx :tracking-pixels/init-application-pixels tracking-pixels/add-application-tracking-pixels)
(reg-fx :tracking-pixels/init-job-pixels tracking-pixels/add-job-tracking-pixels)
