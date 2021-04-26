(ns wh.venia.core
  (:require
    [cljs.loader :as loader]
    [venia.core :as venia]
    [wh.common.fx.graphql :as graphql-fx]))

(reset! graphql-fx/compile-query-fn venia/graphql-query)

(loader/set-loaded! :venia)
