(ns wh.common.repo)

(defn github-url [{:keys [owner name] :as _repo}]
  (str "https://github.com/" owner "/" name))

(defn pull-requests-github-url [repo]
  (str (github-url repo) "/pulls"))