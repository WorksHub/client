(ns wh.interop
  (:require [clojure.string :as str]))

(defn clj-toggle-class-str
  [id cls]
  (str "toggleClass(\"" id "\", \"" cls "\")"))

(defn clj-set-class-str
  [id cls on?]
  (str "setClass(\"" id "\", \"" cls "\", " on? ")"))

(defn toggle-is-open-on-click
  [id]
  #?(:clj {:onClick (clj-toggle-class-str id "is-open")}
     :cljs {:on-click (fn [_] (js/toggleClass id "is-open"))}))

(defn set-is-open-on-click
  [id on?]
  #?(:clj {:onClick (clj-set-class-str id "is-open" on?)}
     :cljs {:on-click (fn [_] (js/setClass id "is-open" on?))}))

(defn disable-no-scroll-on-click
  []
  #?(:clj {:onClick "disableNoScroll()"}
     :cljs {:on-click (fn [_] (js/disableNoScroll))}))

(defn toggle-no-scroll-on-click
  [id]
  #?(:clj {:onClick (str "toggleNoScroll("\" id \"")")}
     :cljs {:on-click (fn [_] (js/toggleNoScroll id))}))

(defn set-no-scroll-on-click
  [id on?]
  #?(:clj {:onClick (str "setNoScroll("\" id \"", " on? ")")}
     :cljs {:on-click (fn [_] (js/setNoScroll id on?))}))

(defn multiple-on-click
  [& fns]
  #?(:clj
     (let [fstrs (str (str/join ";" (map :onClick fns)) ";")]
       {:onClick (str "(function() {"fstrs"})();")})
     :cljs
     (let [ffns (map :on-click fns)]
       {:on-click (fn [_] (run! (fn [f] (f)) ffns))})))
