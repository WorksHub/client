(ns wh.routes-test
  (:require
    [bidi.bidi :as bidi]
    [wh.routes :as r]
    [wh.test-common :as tc]))

(tc/def-spec-test r/path r/serialize-query-params bidi/path-for)
(tc/def-spec-test r/serialize-query-params)
