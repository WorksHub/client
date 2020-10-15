(ns wh.interop
  (:require [clojure.string :as str]
            #?(:clj [net.cgrand.macrovich :as macros]))
  #?(:cljs
     (:require-macros [net.cgrand.macrovich :as macros]
                      ;; must self refer macros
                      [wh.interop :refer [jsf]])))

(defn unparse-arg
  [arg]
  (cond (string? arg)
        (str "\"" (str/replace arg "\"" "\\\"") "\"")
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

(defmacro jsf [form]
  (let [[f args] ((juxt first rest) form)
        fname    (name f)]
    (macros/case
        :clj `(~->jsfn ~fname ~@args)
        :cljs (let [f (symbol "js" fname)]
                `(fn [_#] (~f ~@args))))))

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
  (jsf (setClass id "is-open" on?)))

(defn set-is-open-class
  [class on?]
  (jsf (setClassOnClass class "is-open" on?)))

(defn analytics-track
  [evt prps]
  (jsf (submitAnalyticsTrack evt prps)))

(defn show-auth-popup
  [context & [redirect]]
  (let [c (when context (name context))
        r (when redirect (pr-str redirect))]
    (jsf (showAuthPopUp c r))))

(defn save-redirect
  [redirect]
  (jsf (saveRedirect (pr-str redirect))))

(defn hide-auth-popup
  []
  (jsf (hideAuthPopUp)))

(defn open-video-player
  [youtube-id]
  (jsf (openVideoPlayer youtube-id)))

(defn open-photo-gallery
  [index images]
  #?(:clj  (->jsfn "openPhotoGallery" index images)
     :cljs (fn [_] (js/openPhotoGallery index (clj->js (mapv clj->js images))))))

;;

(defn on-click-fn
  [jsf]
  #?(:clj  {:onClick jsf}
     :cljs {:on-click jsf}))

(defn toggle-class-on-click
  [id cls]
  (on-click-fn (jsf (toggleClass id cls))))

(defn click-tag
  [tag type]
  (jsf (clickOnTag tag type)))

(defn toggle-is-open-on-click
  [id]
  (toggle-class-on-click id "is-open"))

(defn set-is-open-on-click
  [id on?]
  (on-click-fn (set-is-open id on?)))

(defn set-is-open-class-on-click
  [class on?]
  (on-click-fn (set-is-open-class class on?)))

(defn disable-no-scroll-on-click
  []
  (on-click-fn (jsf (disableNoScroll))))

(defn toggle-no-scroll-on-click
  [id]
  (on-click-fn (jsf (toggleNoScroll id))))

(defn set-no-scroll-on-click
  [id on?]
  (on-click-fn (jsf (setNoScroll id on?))))

(defn toggle-menu-display
  []
  (on-click-fn (jsf (toggleMenuDisplay))))

(defn agree-to-tracking-on-click
  []
  (on-click-fn (jsf (agreeToTracking))))

(defn copy-str-to-clipboard-on-click
  [s]
  (on-click-fn (jsf (copyStringToClipboard s))))

(defn multiple-on-click
  [& fns]
  (let [fns (remove nil? fns)]
    #?(:clj
       (let [fstrs (str (str/join ";" (map :onClick fns)) ";")]
         {:onClick (str "(function() {"fstrs"})();")})
       :cljs
       (let [ffns (remove nil? (map :on-click fns))]
         {:on-click (fn [_] (run! (fn [f] (f)) ffns))}))))

(defn listen-newsletter-form
  []
  (jsf (listenNewsletterForm)))

(defn highlight-code-snippets
  []
  (jsf (highlightCodeSnippets)))
