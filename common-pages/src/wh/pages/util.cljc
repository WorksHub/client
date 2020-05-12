(ns wh.pages.util
  (:require
   #?(:clj [net.cgrand.jsoup :as jsoup])
   #?(:cljs [goog.dom :as dom])
    [wh.common.text :as txt]))

(defn width
  []
  (let [pc (str 95 "%")]
    #?(:cljs {:width pc}
       :clj (str "width:" pc))))

#?(:clj
   (defn html->hiccup
     [html-str]
     (when (txt/not-blank html-str)
       (-> html-str
           (.getBytes "UTF-8")
           java.io.ByteArrayInputStream.
           jsoup/parser))))

(defn html
  ([html-content]
   (html html-content {:classname          "html-content"
                       :classname-skeleton "html-content html-content--skeleton"}))
  ([html-content {:keys [classname
                         classname-skeleton]}]
   (if html-content
     #?(:clj [:div {:class classname} (html->hiccup html-content)]
        :cljs [:div {:class classname
                     :dangerouslySetInnerHTML {:__html html-content}}])
     (when classname-skeleton
       [:div {:class classname-skeleton}
        (reduce (fn [a _] (conj a [:div {:style (width)}])) [:div] (range 10))]))))

;; If you came here looking for 'on-scroll' functionlity,
;; be aware that although we're hooking the `onscroll` event of `js/window`
;; it's `scrollTop` field of `documentElement` that has the correct scroll offset

(defn attach-on-scroll-event
  [f]
  #?(:cljs
     (do
       ;; desktop
       (when-let [el (.getElementById js/document "app")]
         (.addEventListener el "scroll"
                            (fn [_]
                              (f (.-scrollTop el)))))
       ;; mobile
       (.addEventListener js/window "scroll"
                          (fn [_]
                            (f (.-scrollY js/window)))))))

;; See https://stackoverflow.com/questions/11214404/how-to-detect-if-browser-supports-html5-local-storage
(defn local-storage-supported?
  []
  #?(:cljs
  (let [item "workshub-test-ls"]
    (try
      (.setItem js/localStorage item item)
      (.removeItem js/localStorage item)
      true
      (catch :default _
        false)))))

(defn get-main-style
  []
  #?(:cljs
     (if (and (local-storage-supported?) (= "true" (.getItem js/localStorage "darkmode-enabled?")))
       :dark
       :light)))

(defn set-main-style!
  [style]
  #?(:cljs
     (let [new-href (if (= style :dark) "/wh-dark.css" "/wh-light.css")
           old-link (dom/getElement "main-style")]
       (if-not (clojure.string/ends-with? (.-href old-link) new-href) 
         (let [head (aget (dom/getElementsByTagName "head") 0)
               new-link (dom/createElement "link")]
           (dom/setProperties old-link (clj->js {:id "main-style-old"}))
           (dom/setProperties new-link (clj->js {:href new-href
                                                 :id "main-style"
                                                 :rel "stylesheet"
                                                 :type "text/css"
                                                 :onload #(dom/removeNode old-link)}))
           (dom/appendChild head new-link)
           (if (local-storage-supported?) (.setItem js/localStorage "darkmode-enabled?" (= style :dark))))))))
