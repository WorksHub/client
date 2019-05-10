;; This is our version of pushy
;; (https://github.com/kibu-australia/pushy), heavily modified to
;; support our use-case of remembering scroll positions on navigation
;; and scrolling back to proper positions when the back button is
;; pressed.

;; Here's an overview of how we achieve that and differences from
;; upstream pushy:

;; 1. Upstream pushy uses Google Closure Library's wrappers around the
;;    pushState API (goog.history); we forgo that and use pushState
;;    directly instead.  The reason is that goog.history doesn't
;;    support passing arbitrary state to history items.  See
;;    https://developer.mozilla.org/en-US/docs/Web/API/History_API for
;;    an overview.

;; 2. We modify pushy to take a function that generates the history
;;    state (state-fn).  This function is then called directly before
;;    advancing to next page, and the resulting state is associated
;;    with history items.  On back-button navigation, the history state
;;    is passed as a second argument to dispatch-fn.

;; 3. Our history state is a JS object of the form {"scroll-position":
;;    274}. Our dispatch fn (set-page) passes that to the set-page
;;    event, which in turn sets it in app-db under ::db/scroll. After
;;    React re-renders our UI after navigation, the current-page
;;    component's :component-did-update lifecycle method sets the
;;    scroll position (wrapped in a requestAnimationFrame and a zero
;;    setTimeout to ensure that a full re-render has happened by the
;;    time it's called).

;; 4. If user click the back button, browser triggers onpopstate event
;;    and the dispatch-fn gets a non-null history state. In this case,
;;    we don't fire on-page-load events, which makes for a smoother
;;    user experience.

(ns pushy.core
  (:require [clojure.string]
            [goog.events :as events])
  (:import goog.Uri))

(defn- on-click [funk]
  (events/listen js/document "click" funk))

(defprotocol IHistory
  (set-token! [this token title])
  (replace-token! [this token title])
  (get-token [this])
  (start! [this])
  (stop! [this]))

(defn- get-token-from-uri [uri]
  (let [path (.getPath uri)
        query (.getQuery uri)]
    ;; Include query string in token
    (if (empty? query) path (str path "?" query))))

(defn- processable-url? [uri]
  (and (not (clojure.string/blank? uri))                    ;; Blank URLs are not processable.
       (or (and (not (.hasScheme uri)) (not (.hasDomain uri))) ;; By default only process relative URLs + URLs matching window's origin
           (some? (re-matches (re-pattern (str "^" (.-origin js/location) ".*$"))
                              (str uri))))))

(defn pushy
  "Takes in three functions:
    * dispatch-fn: the function that dispatches when a match is found
    * match-fn: the function used to check if a particular route exists
    * identity-fn: (optional) extract the route from value returned by match-fn"
  [dispatch-fn match-fn &
   {:keys [processable-url? identity-fn state-fn prevent-default-when-no-match?]
    :or   {processable-url?               processable-url?
           identity-fn                    identity
           state-fn                       (constantly nil)
           prevent-default-when-no-match? (constantly false)}}]

  (let [event-keys (atom nil)]
    (reify
      IHistory
      (set-token! [this token title]
        (js/window.history.replaceState (state-fn) nil nil)
        (js/window.history.pushState nil title token)
        (when-let [match (-> (get-token this) match-fn identity-fn)]
          (dispatch-fn match nil)))

      (replace-token! [this token title]
        (js/window.history.replaceState (state-fn) title token)
        (when-let [match (-> (get-token this) match-fn identity-fn)]
          (dispatch-fn match nil)))

      (get-token [_]
        (get-token-from-uri (.parse Uri js/window.location.href)))

      (start! [this]
        (stop! this)
        ;; We want to call `dispatch-fn` on any change to the location
        (swap! event-keys conj
               (events/listen js/window "popstate"
                              (fn [e]
                                (when-let [match (-> (get-token this) match-fn identity-fn)]
                                  (dispatch-fn match (.-state e))))))

        ;; Dispatch on initialization
        (when-let [match (-> (get-token this) match-fn identity-fn)]
          (dispatch-fn match nil))

        (swap! event-keys conj
               (on-click
                (fn [e]
                  (when-let [el (some-> e .-target (.closest "a"))]
                    (let [uri (.parse Uri (.-href el))]
                      ;; Proceed if `identity-fn` returns a value and
                      ;; the user did not trigger the event via one of the
                      ;; keys we should bypass
                      (when (and (processable-url? uri)
                                 ;; Bypass dispatch if any of these keys
                                 (not (.-altKey e))
                                 (not (.-ctrlKey e))
                                 (not (.-metaKey e))
                                 (not (.-shiftKey e))
                                 ;; Bypass if target = _blank
                                 (not (get #{"_blank" "_self"} (.getAttribute el "target")))
                                 ;; Bypass if explicitly instructed to ignore this element
                                 (or (not (.hasAttribute el "data-pushy-ignore"))
                                     (= (.getAttribute el "data-pushy-ignore") "false"))
                                 ;; Only dispatch on left button click
                                 (= 0 (.-button e)))
                        (let [next-token (get-token-from-uri uri)]
                          (if (identity-fn (match-fn next-token))
                            ;; Dispatch!
                            (do
                              (set-token! this next-token (or (-> el .-title) ""))
                              (.preventDefault e))

                            (when (prevent-default-when-no-match? next-token)
                              (.preventDefault e))))))))))
        nil)

      (stop! [this]
        (doseq [key @event-keys]
          (events/unlistenByKey key))
        (reset! event-keys nil)))))

;; Backwards compatibility with pushy <= 0.2.2
(defn push-state!
  ([dispatch-fn match-fn]
   (push-state! dispatch-fn match-fn identity))
  ([dispatch-fn match-fn identity-fn]
   (let [h (pushy dispatch-fn match-fn :identity-fn identity-fn)]
     (start! h)
     h)))
