(ns wh.interop.forms
  (:require
    [clojure.string :as str]
    [wh.interop :refer [->jsfn unparse-arg]]
    [wh.re-frame.events :refer [dispatch]]
    [wh.util :as util]))

(defn on-change-fn
  [jsf]
  #?(:clj {:onchange jsf}
     :cljs {:on-change jsf}))

(defn multiple-on-change
  [& fns]
  (let [fns (remove nil? fns)]
    #?(:clj
       (let [thisv (str (gensym))
             fstrs (str (str/replace (str/join ";" (map :onchange fns)) #"this" thisv) ";")]
         {:onchange (str "var " thisv "=this;"
                         "(function() {"fstrs"})();")})
       :cljs
       (let [ffns (remove nil? (map :on-change fns))]
         {:on-change (fn [e] (run! (fn [f] (f e)) ffns))}))))

(defn add-select-value-to-url
  [query-param-name options]
  #?(:clj (str "let v=" (str (unparse-arg (map (comp name :id) options)) "[this.value];")
               (->jsfn "setQueryParams" (hash-map (name query-param-name) 'v)))
     :cljs (fn [e]
             (let [v (nth (map (comp name :id) options) (js/parseInt (.-value (.-target e))))]
               (dispatch [:wh.events/nav--query-params (hash-map query-param-name v)])))))

(defn dispatch-events-on-change
  "Dispatches re-frame event. noop on clj"
  [events]
  #?(:cljs {:on-change (fn [_] (run! dispatch events))}))
