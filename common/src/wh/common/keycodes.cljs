(ns wh.common.keycodes)

;; there are varying codes for the spacebar, so including both just in case
(def spacebar [0 32])

(def enter 13)
(def arrow-up 38)
(def arrow-down 40)
(def escape 27)

(def codes {:spacebar spacebar
            :enter enter
            :up arrow-up
            :down arrow-down
            :escape escape})
