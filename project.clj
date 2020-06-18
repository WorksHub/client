(defproject works-hub "2.0.0-SNAPSHOT"
  :license {:name "Eclipse Public License 2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :url "https://github.com/WorksHub/client"
  :source-paths ["server/src"
                 "common/src"
                 "common-pages/src"]
  :resource-paths ["target/resources"
                   "client/resources"
                   "client/src-js"]
  :plugins [[lein-sass "0.4.0"]
            [lein-shell "0.5.0"]]
  :prep-tasks ["compile"]
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

  :profiles {:dev  {:dependencies [[binaryage/devtools "0.9.9"]
                                   [com.taoensso/tufte "2.0.1"]
                                   [nrepl "0.6.0"]
                                   [cider/piggieback "0.4.0"]
                                   [javax.servlet/servlet-api "2.5"]
                                   [org.clojure/test.check "0.9.0"]
                                   [day8.re-frame/re-frame-10x "0.3.7"]
                                   [ring-mock "0.1.5"]]
                    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                    :plugins      [[lein-ancient "0.6.15" :exclusions [org.clojure/clojure commons-codec org.clojure/data.xml]]]}
             :prod {}}

  :sass {:src              "client/styles/"
         :output-directory "client/resources/public"}

  :min-lein-version "2.5.0")
