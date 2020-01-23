(ns wh.db)

;; This file is here to facilitate server-side resolution of the `wh.db` ns
;; and provide some no-ops

(def default-interceptors nil)

(defn logged-in? [db]
  (get-in db [:wh.user.db/sub-db :wh.user.db/id]))
