(ns wh.interop
  (:require [clojure.string :as str]))

(defn ->jsfn
  "Takes a fn name and n arguments, and returns a string which is a JS fn call format.
  e.g. myFunction(1, 2, \"foo\", \"bar\")"
  [fn-name & args]
  (let [params (->> args
                    (map (fn [arg] (if (string? arg) (str "\"" arg "\"" ) arg)))
                    (str/join ",")
                    (apply str))]
    (str fn-name "(" params ")")))

(defn toggle-is-open-on-click
  [id]
  #?(:clj {:onClick (->jsfn "toggleClass" id "is-open")}
     :cljs {:on-click (fn [_] (js/toggleClass id "is-open"))}))

(defn set-is-open-on-click
  [id on?]
  #?(:clj {:onClick (->jsfn "setClass" id "is-open" on?)}
     :cljs {:on-click (fn [_] (js/setClass id "is-open" on?))}))

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
