(ns wh.common.url-test
  (:require [wh.common.url :as url]
            [wh.test-common :as tc]))

(tc/def-spec-test url/parse-query-string)
(tc/def-spec-test url/uri->query-params)
(tc/def-spec-test url/serialize-query-params)
