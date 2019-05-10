(ns wh.pages.util
  (:require
    #?(:clj [net.cgrand.jsoup :as jsoup])
    [wh.common.text :as txt]))

#?(:clj
   (defn html->hiccup
     [html-str]
     (when (txt/not-blank html-str)
       (-> html-str
           (.getBytes "UTF-8")
           java.io.ByteArrayInputStream.
           jsoup/parser))))

(defn attach-on-scroll-event
  [f]
  #?(:cljs
     (when-let [el (aget (.getElementsByClassName js/document "page-container") 0)]
       (set! (.-onscroll el)
             (fn [_] (f el))))))
