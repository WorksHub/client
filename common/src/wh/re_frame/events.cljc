(ns wh.re-frame.events
  (:require [re-frame.core :as re-frame]))

#?(:clj
   (defn dispatch
     "no-op"
     [_])
   :cljs
   (def dispatch re-frame/dispatch))

#?(:clj
   (defn dispatch-sync
     "no-op"
     [_])
   :cljs
   (def dispatch-sync re-frame/dispatch-sync))

#?(:clj
   (defn reg-event-db
     "no-op"
     [& _])
   :cljs
   (def reg-event-db re-frame/reg-event-db))

#?(:clj
   (defn reg-event-fx
     "no-op"
     [& _])
   :cljs
   (def reg-event-fx re-frame/reg-event-fx))

#?(:clj
   (defn reg-cofx
     "no-op"
     [& _])
   :cljs
   (def reg-cofx re-frame/reg-cofx))

#?(:clj
   (defn inject-cofx
     "no-op"
     [_])
   :cljs
   (def inject-cofx re-frame/inject-cofx))

#?(:clj
   (defn client-only-handler [_]
        (fn [_ _]))
   :cljs
   (defn client-only-handler [handler]
        handler))
