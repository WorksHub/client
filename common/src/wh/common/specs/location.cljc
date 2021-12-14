(ns wh.common.specs.location
  (:require
    [#?(:clj  clojure.spec.alpha
        :cljs cljs.spec.alpha) :as s]
    [#?(:clj  clojure.spec.gen.alpha
        :cljs cljs.spec.gen.alpha) :as gen]
    [spec-tools.core :as st]
    [wh.common.data :as data]
    [wh.common.specs.primitives :as p]))

(s/def :wh.location/street string?)
(s/def :wh.location/post-code string?)
(s/def :wh.location/city string?)
(s/def :wh.location/cities (s/coll-of :wh.location/city :distinct true))
(s/def :wh.location/state (s/with-gen
                            (s/and string? (set (map second data/us-states)))
                            #(gen/elements (map second data/us-states))))
(s/def :wh.location/administrative string?)
(s/def :wh.location/country ::p/non-empty-string)
(s/def :wh.location/country-code (st/spec (set data/country-codes) {:type 'String}))
(s/def :wh.location/non-us-country-code (st/spec (set data/non-us-country-codes) {:type 'String}))
(s/def :wh.location/country-codes (s/coll-of :wh.location/country-code :distinct true))
(s/def :wh.location/sub-region (s/nilable string?))
(s/def :wh.location/region (s/nilable string?))
(s/def :wh.location/regions (s/coll-of :wh.location/region :distinct true))


(s/def :wh.location/latitude (s/or :double (s/double-in :min -90.0 :max 90 :NaN false :infinite? false)
                                   :int (s/int-in -90 90)))
(s/def :wh.location/longitude (s/or :double (s/double-in :min -180.0 :max 180 :NaN false :infinite? false)
                                    :int (s/int-in -180 180)))
(s/def :wh.location/timezone string?)

(s/def :wh/location (s/keys :req-un [:wh.location/country
                                     :wh.location/country-code]
                            :opt-un [:wh.location/city
                                     :wh.location/sub-region
                                     :wh.location/street
                                     :wh.location/region
                                     :wh.location/latitude
                                     :wh.location/longitude
                                     :wh.location/administrative
                                     :wh.location/state
                                     :wh.location/timezone
                                     :wh.location/post-code]))
