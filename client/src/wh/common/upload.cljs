(ns wh.common.upload
  (:require
    [ajax.json :as ajax-json]
    [bidi.bidi :as bidi]
    [re-frame.core :refer [dispatch]]
    [re-frame.db]
    [wh.routes :refer [path]]))

(defn upload-file
  "Launches a file reader that will asynchronously read the contents of
  file and dispatch a launch upload event eventually triggering on-success
  or on-failure."
  [file {:keys [launch on-upload-start on-success on-failure custom-headers]}]
  (let [reader (js/FileReader.)]
    (doto reader
      (aset "onloadend"
            #(do
               (when on-upload-start
                 (dispatch on-upload-start))
               (dispatch (into launch
                               [file (-> % .-target .-result) on-success on-failure custom-headers]))))
      (.readAsArrayBuffer file))))

(defn handler
  [& {:as options}]
  "Returns a handler for onchange events suitable for attaching to
  <input type=file>s."
  (fn [^js/Event ev]
    (let [files (-> ev .-target .-files)]
      (when (> (alength files) 0)
        (let [file (aget files 0)]
          (upload-file file options))))))

(defn file-upload-fn [endpoint _ [file contents on-success on-failure custom-headers]]
  (let [filename (.-name file)]
    {:http-xhrio {:method          :post
                  :uri             (str (:wh.db/api-server @re-frame.db/app-db) endpoint)
                  :body            contents
                  :headers         (merge {"Content-Type" (.-type file)
                                           "X-Filename" (bidi/url-encode filename)}
                                          custom-headers)
                  :response-format (ajax-json/json-response-format {:keywords? true})
                  :with-credentials true
                  :timeout         20000
                  :on-success      (conj on-success filename)
                  :on-failure      on-failure}}))

(def image-upload-fn (partial file-upload-fn (path :image-upload)))
(def cv-upload-fn (partial file-upload-fn (path :cv-upload)))
(def cover-letter-upload-fn (partial file-upload-fn (path :cover-letter-upload)))
