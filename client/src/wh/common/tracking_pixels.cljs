(ns wh.common.tracking-pixels
  (:require [wh.common.local-storage :as local-storage]
            [clojure.string :as str]))

(def last-visit-key "wh_last_visit")

;;

(defn add-tracking-pixels? []
  (let [first-visit? (-> (local-storage/get-item last-visit-key)
                         boolean
                         not)
        timestamp    (.getTime (js/Date.))]
    (when first-visit?
      (local-storage/set-item! last-visit-key timestamp))
    first-visit?))

(defn add-to-page! [elms']
  (let [id          "tracking-pixels"
        _rm-wrapper (some-> js/document
                            (.getElementById id)
                            (.remove))
        wrapper     (.createElement js/document "div")
        elms        (remove nil? elms')]
    (set! (.-id wrapper) id)
    (doseq [elm elms]
      (.appendChild wrapper elm))
    (.appendChild js/document.body wrapper)))

;;

(def base "https://neuvoo.ca/pixel.gif?action=conversion+apply&source=functionalworks")
(def url-talent-registration (str base "&apply_step=registration"))
(def url-talent-application (str base "&apply_step=application"))
(def url-zip-recruiter "https://track.ziprecruiter.com/conversion?enc_account_id=06536b20")
(def url-adzuna-uk "https://www.adzuna.co.uk/app_complete/7616")
(def url-adzuna-usa "https://www.adzuna.com/app_complete/7615")
(def url-adzuna-germany "https://www.adzuna.de/app_complete/7618")
(def url-adzuna-canada "https://www.adzuna.ca/app_complete/8188")
(def url-adzuna-netherlands "https://www.adzuna.nl/app_complete/7617")
(def url-jooble "https://jooble.org/js/conversion.js")

;;

(defn job->adzuna-info [job]
  (when-let [country (get-in job [:location :country])]
    (cond
      (or (str/includes? country "UK")
          (str/includes? country "United Kingdom"))
      [:adzuna-uk url-adzuna-uk]
      ;;
      (or (str/includes? country "USA")
          (str/includes? country "United States of America")
          (str/includes? country "United States"))
      [:adzuna-usa url-adzuna-usa]
      ;;
      (str/includes? country "Netherlands")
      [:adzuna-netherlands url-adzuna-netherlands]
      ;;
      (str/includes? country "Germany")
      [:adzuna-germany url-adzuna-germany]
      ;;
      (str/includes? country "Canada")
      [:adzuna-canada url-adzuna-canada])))

;;

(defmulti tracking-pixel identity)

(defn tp [{:keys [content id]}]
  (let [wrapper (.createElement js/document "div")]
    (set! (.-id wrapper) id)
    (set! (.-innerHTML wrapper) content)
    wrapper))

(defmethod tracking-pixel :talent-registration []
  (tp {:id      "talent-registration"
       :content (str "<img src='" url-talent-registration "' />")}))

(defmethod tracking-pixel :talent-application []
  (tp {:id      "talent-application"
       :content (str "<img src='" url-talent-application "' />")}))

(defmethod tracking-pixel :zip-recruiter []
  (tp {:id      "zip-recruiter"
       :content (str "<img src='" url-zip-recruiter "' width='1' height='1' />")}))

(defmethod tracking-pixel :adzuna [_k job]
  (when-let [info (job->adzuna-info job)]
    (tp {:id      (-> info first name)
         :content (str "<iframe src='" (-> info second)
                       "' scrolling='no' frameborder='0' width='1' height='1' />")})))


(defmethod tracking-pixel :jooble []
  (let [wrapper (.createElement js/document "div")
        script  (let [script (.createElement js/document "script")
                      onload (fn []
                               (js/window.jbl_et)
                               (js/window.jbl_cc))]
                  (set! (.-async script) true)
                  (set! (.-type script) "text/javascript")
                  (set! (.-src script) url-jooble)
                  (set! (.-onload script) onload)
                  script)]
    (set! (.-id wrapper) "jooble")
    (.appendChild wrapper script)
    wrapper))

(defmethod tracking-pixel :default []
  nil)

(defn add-registration-tracking-pixels []
  (when (add-tracking-pixels?)
    (add-to-page! [(tracking-pixel :talent-registration)
                   (tracking-pixel :zip-recruiter)
                   (tracking-pixel :jooble)])))

(defn add-application-tracking-pixels [{:keys [env _job] :as args}]
  (when (= env :prod)
    (add-to-page! [(tracking-pixel :talent-application)])))

(defn add-job-tracking-pixels [{:keys [env job] :as args}]
  (when (= env :prod)
    (add-to-page! [(tracking-pixel :adzuna job)])))
