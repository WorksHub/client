(ns wh.routes-test
  (:require
    [bidi.bidi :as bidi]
    [wh.common.url :as url]
    [wh.routes :as r]
    [wh.test-common :as tc]))

(tc/def-spec-test r/path
                  r/prepare-path-params
                  url/serialize-query-params
                  bidi/path-for)
(tc/def-spec-test r/prepare-path-params)
