(ns wh.interop
  (:require [clojure.string :as str]))

(defn unparse-arg
  [arg]
  (cond (string? arg)
        (str "\"" (str/replace arg "\"" "\\\"") "\"" )
        (sequential? arg)
        (str "[" (str/join "," (map unparse-arg arg)) "]")
        (map? arg)
        (str "{" (str/join "," (map (fn [[k v]] (str (name k) ":" (unparse-arg v))) arg)) "}")
        :else
        arg))

(defn ->jsfn
  "Takes a fn name and n arguments, and returns a string which is a JS fn call format.
  e.g. myFunction(1, 2, \"foo\", \"bar\")"
  [fn-name & args]
  (let [params (->> args
                    (map unparse-arg)
                    (str/join ",")
                    (apply str))]
    (str fn-name "(" params ")")))

(defn do
  [& fns]
  #?(:clj
     (let [fstrs (str (str/join ";" fns) ";")]
       (str "(function() {"fstrs"})();"))
     :cljs
     (fn [_] (run! (fn [f] (f)) fns))))

(defn set-class-on-scroll
  [id cls scroll-amount]
  (str "attachOnScrollEvent(function(e){" (->jsfn "setClassAtScrollPosition" 'e id cls scroll-amount) "})"))

(defn set-is-open
  [id on?]
  #?(:clj (->jsfn "setClass" id "is-open" on?)
     :cljs (fn [_] (js/setClass id "is-open" on?))))

(defn set-is-open-class
  [class on?]
  #?(:clj (->jsfn "setClassOnClass" class "is-open" on?)
     :cljs (fn [_] (js/setClassOnClass class "is-open" on?))))

(defn analytics-track
  [evt prps]
  #?(:clj (->jsfn "submitAnalyticsTrack" evt prps)
     :cljs (fn [_] (js/submitAnalyticsTrack evt prps))))

(defn show-auth-popup
  [context & [redirect]]
  (let [c (when context (name context))
        r (when redirect (pr-str redirect))]
    #?(:clj (->jsfn "showAuthPopUp" c r)
       :cljs (fn [_] (js/showAuthPopUp c r)))))

(defn hide-auth-popup
  []
  #?(:clj (->jsfn "hideAuthPopUp")
     :cljs (fn [_] (js/hideAuthPopUp))))

(defn open-video-player
  [youtube-id]
  #?(:clj (->jsfn "openVideoPlayer" youtube-id)
     :cljs (fn [_] (js/openVideoPlayer youtube-id))))

(defn open-photo-gallery
  [index images]
  #?(:clj (->jsfn "openPhotoGallery" index images)
     :cljs (fn [_] (js/openPhotoGallery index (clj->js (mapv clj->js images))))))

;;

(defn on-click-fn
  [jsf]
  #?(:clj {:onClick jsf}
     :cljs {:on-click jsf}))

(defn toggle-is-open-on-click
  [id]
  #?(:clj {:onClick (->jsfn "toggleClass" id "is-open")}
     :cljs {:on-click (fn [_] (js/toggleClass id "is-open"))}))

(defn set-is-open-on-click
  [id on?]
  (on-click-fn (set-is-open id on?)))

(defn set-is-open-class-on-click
  [class on?]
  (on-click-fn (set-is-open-class class on?)))

(defn disable-no-scroll-on-click
  []
  #?(:clj {:onClick (->jsfn "disableNoScroll")}
     :cljs {:on-click (fn [_] (js/disableNoScroll))}))

(defn toggle-no-scroll-on-click
  [id]
  #?(:clj {:onClick (->jsfn "toggleNoScroll" id)}
     :cljs {:on-click (fn [_] (js/toggleNoScroll id))}))

(defn set-no-scroll-on-click
  [id on?]
  #?(:clj {:onClick (->jsfn "setNoScroll" id on?)}
     :cljs {:on-click (fn [_] (js/setNoScroll id on?))}))

(defn agree-to-tracking-on-click
  []
  #?(:clj {:onClick (->jsfn "agreeToTracking")}
     :cljs {:on-click (fn [_] (js/agreeToTracking))}))

(defn multiple-on-click
  [& fns]
  #?(:clj
     (let [fstrs (str (str/join ";" (map :onClick fns)) ";")]
       {:onClick (str "(function() {"fstrs"})();")})
     :cljs
     (let [ffns (map :on-click fns)]
       {:on-click (fn [_] (run! (fn [f] (f)) ffns))})))
