(ns wh.common.tag)

(defn tech? [tag]
  (boolean (some-> tag
                   :type
                   keyword
                   (= :tech))))
