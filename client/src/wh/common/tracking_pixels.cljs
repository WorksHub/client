(ns wh.common.tracking-pixels
  (:require [wh.common.local-storage :as local-storage]))

(def last-visit-key "wh_last_visit")

(defn add-tracking-pixels? []
  (let [first-visit? (-> (local-storage/get-item last-visit-key)
                         boolean
                         not)
        timestamp    (.getTime (js/Date.))]
    (when first-visit?
      (local-storage/set-item! last-visit-key timestamp))
    first-visit?))

(defn add-to-page! [elms]
  (let [wrapper (.createElement js/document "div")]
    (set! (.-id wrapper) "tracking-pixels")
    (doseq [elm elms]
      (.appendChild wrapper elm))
    (.appendChild js/document.body wrapper)))

(defmulti tracking-pixel identity)

(def base "https://neuvoo.ca/pixel.gif?action=conversion+apply&source=functionalworks")
(def url-talent-registration (str base "&apply_step=registration"))
(def url-talent-application (str base "&apply_step=application"))

(defmethod tracking-pixel :talent-registration []
  (let [wrapper (.createElement js/document "div")]
    (set! (.-id wrapper) "talent-registration")
    (set! (.-innerHTML wrapper)
          (str "<img src='" url-talent-registration "' />"))
    wrapper))

(defmethod tracking-pixel :talent-application []
  (let [wrapper (.createElement js/document "div")]
    (set! (.-id wrapper) "talent-application")
    (set! (.-innerHTML wrapper)
          (str "<img src='" url-talent-application "' />"))
    wrapper))

(defn add-registration-tracking-pixels []
  (when (add-tracking-pixels?)
    (add-to-page! [(tracking-pixel :talent-registration)])))

(defn add-application-tracking-pixels []
  (add-to-page! [(tracking-pixel :talent-application)]))
