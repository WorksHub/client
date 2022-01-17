(ns wh.interop.forms
  (:require
    [clojure.string :as str]
    [wh.interop :refer [->jsfn unparse-arg]]
    [wh.re-frame.events :refer [dispatch]]))

(defn on-change-fn
  [jsf]
  #?(:clj {:onchange jsf}
     :cljs {:on-change jsf}))

(defn on-input-fn
  [jsf]
  #?(:clj {:oninput jsf}
     :cljs {:on-input jsf}))

(defn on-press-enter-fn
  [jsf]
  #?(:clj {:onkeypress (str "if (event.key === 'Enter') { " jsf "}")}
     :cljs {:on-key-press
            (fn [e]
              (when (= (aget e "key") "Enter")
                (jsf)))}))

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

(defn add-input-value-to-url
  [query-param-name event]
  #?(:clj (str "window.location = " (remove-page-add-interaction-qps
                                      (symbol (->jsfn "setQueryParam" false (name query-param-name) 'this.value))))
     :cljs (fn [_e]
             (dispatch [event]))))

(defn dispatch-tag-select-event [on-change-event query-param-name]
  (fn [focused-tag-query-id focused-tag value attr]
    #?(:cljs (this-as this
               (dispatch [on-change-event
                          {:query-param  (some-> query-param-name name)
                           :tag-query-id (or focused-tag-query-id (.-focusedTagQueryId this))
                           :tag          (or focused-tag (.-focusedTag this))
                           :value        value
                           :attr         attr}])))))

(defn add-tag-value-to-url
  ([on-change-event]
   #?(:clj (str "let url = handleTagChange(this);
                if (url){window.location = url.href;} ")
      :cljs (dispatch-tag-select-event on-change-event nil)))
  ([query-param-name on-change-event]
   #?(:clj (str "let url = handleTagChange(this, \"" (name query-param-name) "\");
                if (url){window.location = url.href;} ")
      :cljs (dispatch-tag-select-event on-change-event query-param-name))))

(defn add-check-value-to-url
  ([query-param-name]
   (add-check-value-to-url query-param-name 1))
  ([query-param-name value]
   #?(:clj (str "if(this.checked)"
                "{window.location = " (remove-page-add-interaction-qps
                                        (symbol (->jsfn "setQueryParam" false (name query-param-name) value))) ".href;}"
                "else"
                "{window.location = " (->jsfn "deleteQueryParam" false (name query-param-name)) ".href;}")
      :cljs (fn [e]
              (let [v (when (.-checked (.-target e)) value)]
                (dispatch [:wh.events/nav--set-query-param (name query-param-name) v]))))))

(defn filter-tags
  [tag-field-id text-atom collapsable?]
  #?(:clj  (->jsfn "filterTags" tag-field-id 'this collapsable?)
     :cljs (fn [e]
             (reset! text-atom (.. e -target -value))
             (js/filterTags tag-field-id (.-target e) collapsable?))))

(defn dispatch-events-on-change
  "Dispatches re-frame event. noop on clj"
  [events]
  #?(:cljs {:on-change (fn [_] (run! dispatch events))}))
