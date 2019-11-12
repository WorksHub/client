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

(defn on-input-fn
  [jsf]
  #?(:clj {:oninput jsf}
     :cljs {:on-input jsf}))

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

(defn remove-page-add-interaction-qps
  [url]
  (->jsfn "setQueryParam"
          (symbol (->jsfn "deleteQueryParam" url "page"))
          "interaction" 1))

(defn add-select-value-to-url
  [query-param-name options]
  #?(:clj (str "let v=" (str (unparse-arg (map (comp name :id) options)) "[this.value];")
               "window.location = " (remove-page-add-interaction-qps
                                      (symbol (->jsfn "setQueryParam" false (name query-param-name) 'v))) ".href")
     :cljs (fn [e]
             (let [v (nth (map (comp name :id) options) (js/parseInt (.-value (.-target e))))]
               (dispatch [:wh.events/nav--set-query-param (name query-param-name) v])))))

(defn add-tag-value-to-url
  [query-param-name on-change-event]
  #?(:clj (str "let url = handleTagChange(this, \"" (name query-param-name) "\"); if(url){window.location = url.href;} ")
     :cljs (fn [focused-tag-query-id focused-tag]
             (this-as this
               (dispatch [on-change-event
                          (name query-param-name)
                          (or focused-tag-query-id (.-focusedTagQueryId this))
                          (or focused-tag (.-focusedTag this))])))))

(defn add-check-value-to-url
  [query-param-name]
  #?(:clj (str "if(this.checked)"
               "{window.location = " (remove-page-add-interaction-qps
                                       (symbol (->jsfn "setQueryParam" false (name query-param-name) 1))) ".href;}"
               "else"
               "{window.location = " (->jsfn "deleteQueryParam" false (name query-param-name)) ".href;}")
     :cljs (fn [e]
             (let [v (when (.-checked (.-target e)) 1)]
               (dispatch [:wh.events/nav--set-query-param (name query-param-name) v])))))

(defn filter-tags
  [tag-field-id text-atom]
  #?(:clj  (->jsfn "filterTags" tag-field-id 'this)
     :cljs (fn [e]
             (reset! text-atom (.. e -target -value))
             (js/filterTags tag-field-id (.-target e)))))

(defn dispatch-events-on-change
  "Dispatches re-frame event. noop on clj"
  [events]
  #?(:cljs {:on-change (fn [_] (run! dispatch events))}))
