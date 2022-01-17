(ns wh.pages.util
  (:require
    #?(:clj [net.cgrand.jsoup :as jsoup])
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

;; If you came here looking for 'on-scroll' functionality,
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
