(ns wh.pages.util
  (:require
    #?(:clj [net.cgrand.jsoup :as jsoup])
    [wh.common.text :as txt]))

(defn rand-width
  []
  (let [pc (str (+ 80 (rand-int 20)) "%")]
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

(defn html [html-content]
  (if html-content
    #?(:clj (html->hiccup html-content)
       :cljs [:div.html-content {:dangerouslySetInnerHTML {:__html html-content}}])
    [:div.html-content.html-content--skeleton
     (reduce (fn [a _] (conj a [:div {:style (rand-width)}])) [:div] (range (+ 4 (rand-int 6))))]))

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
                              (f el))))
       ;; mobile
       (.addEventListener js/window "scroll"
                          (fn [_]
                            (f js/window))))))
