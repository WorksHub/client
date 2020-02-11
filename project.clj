(defproject works-hub "2.0.0-SNAPSHOT"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :url "https://github.com/WorksHub/client"
  :source-paths ["server/src"
                 "common/src"
                 "common-pages/src"]
  :resource-paths ["target/resources"
                   "client/resources"
                   "client/src-js"]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-sass "0.4.0"]
            [lein-shell "0.5.0"]]
  :prep-tasks ["compile"
               ["cljsbuild" "once"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [cljsjs/react "16.3.2-0"]
                 [cljsjs/react-dom "16.3.2-0"]
                 [cljsjs/prop-types "15.7.2-0"] ;; need for react-quill
                 [alandipert/storage-atom "2.0.1" :exclusions [com.cognitect/transit-cljs]]
                 [bidi "2.1.3"]
                 [camel-snake-kebab "0.4.0"]
                 [circleci/analytics-clj "0.8.0"]
                 [clj-http "3.9.1"]
                 [cljs-ajax "0.7.3" :exclusions [com.cognitect/transit-clj]]
                 [cljsjs/body-scroll-lock "2.6.1-0"]
                 [cljsjs/google-maps "3.18-1"]
                 [cljsjs/moment "2.24.0-0"]
                 [cljsjs/react-draggable "3.0.3-0" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/react-quill "1.1.0-0" :exclusions [cljsjs/react]]
                 [cljsjs/react-stripe-elements "1.6.0-1"]
                 [cljsjs/react-tooltip "3.3.0-0"]
                 [cljsjs/smoothscroll-polyfill "0.4.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.smxemail/re-frame-cookie-fx "0.0.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [enlive "1.1.6"]
                 [expound "0.7.1"]
                 [markdown-clj "1.0.2"]
                 [metosin/spec-tools "0.8.0"]
                 [org.clojars.nathell/venia "0.2.6-SNAPSHOT"] ; with https://github.com/Vincit/venia/pull/37
                 [org.clojure/core.incubator "0.1.4"]
                 [org.clojure/test.check "0.9.0"]
                 [re-frame "0.10.7"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [thi.ng/geom "1.0.0-RC3"]]

  :cljsbuild {:builds {:client {:compiler {:foreign-libs  [{:file        "client/vendor/js/physicsjs.js"
                                                            :module-type :es6
                                                            :provides    ["physicsjs"]}
                                                           {:file     "client/vendor/js/rc-slider.min.js"
                                                            :requires ["cljsjs.react"
                                                                       "cljsjs.react.dom"]
                                                            :provides ["rcslider"]}]
                                           :modules       ~(read-string (slurp "client/resources/modules.edn"))
                                           :verbose       true
                                           :output-to     "target/resources/public/js/wh.js"
                                           :output-dir    "target/resources/public/js/"
                                           :asset-path    "/js"}}}}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.9"]
                                  [com.taoensso/tufte "2.0.1"]
                                  [nrepl "0.6.0"]
                                  [cider/piggieback "0.4.0"]
                                  [figwheel-sidecar "0.5.18"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [org.clojure/test.check "0.9.0"]
                                  [day8.re-frame/re-frame-10x "0.3.7"]
                                  [ring-mock "0.1.5"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :cljsbuild    {:builds {:client {:source-paths ["client/env/dev" "client/src" "common/src" "common-pages/src"]
                                                    :figwheel     {:build-id       "client"
                                                                   :websocket-host :js-client-host}
                                                    :compiler     {:main                 wh.dev
                                                                   :optimizations        :none
                                                                   :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                                                                   :preloads             [devtools.preload day8.re-frame-10x.preload]
                                                                   :source-map           true
                                                                   :source-map-timestamp true}}}}
                   :plugins      [[lein-ancient "0.6.15" :exclusions [org.clojure/clojure commons-codec org.clojure/data.xml]]]
                   :figwheel     {:ring-handler   wh.server/handler
                                  :server-logfile false
                                  :css-dirs       ["client/resources/public/wh.css"]}}
             :prod {:cljsbuild {:builds {:client {:source-paths ["client/env/prod" "client/src" "common/src" "common-pages/src"]
                                                  :jar          true
                                                  :compiler     {:main            wh.core
                                                                 :externs         ["client/externs/physics.js"
                                                                                   "client/externs/analytics.js"]
                                                                 :source-map      "target/resources/public/js/works-hub.js.map"
                                                                 :optimizations   :advanced
                                                                 :closure-defines {goog.DEBUG false}
                                                                 :pseudo-names    false
                                                                 :pretty-print    false}}}}}}

  :sass {:src              "client/styles/"
         :output-directory "client/resources/public"}

  :min-lein-version "2.5.0")
