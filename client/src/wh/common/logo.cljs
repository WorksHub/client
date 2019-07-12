(ns wh.common.logo
  (:require
    [ajax.formats :as ajax-formats]
    [ajax.json :as ajax-json]
    [re-frame.core :refer [reg-event-fx]]
    [wh.common.upload :as upload]
    [wh.db :as db]))

(reg-event-fx
  ::logo-upload
  db/default-interceptors
  upload/image-upload-fn)

(reg-event-fx
  ::fetch-clearbit-logo
  db/default-interceptors
  (fn [{db :db} [logo-uri upload-success-event upload-failure-event]]
    {:http-xhrio {:method          :get
                  :uri             logo-uri
                  :response-format (assoc (ajax-formats/raw-response-format) :type :arraybuffer)
                  :timeout         10000
                  :on-success      [::fetch-clearbit-logo-success upload-success-event upload-failure-event]
                  :on-failure      [::logo-upload-failure upload-failure-event]}}))

(reg-event-fx
  ::fetch-clearbit-logo-success
  db/default-interceptors
  (fn [{db :db} [upload-success-event upload-failure-event logo]]
    {:http-xhrio {:method          :post
                  :uri             "/api/image"
                  :body            logo
                  :headers         {"Content-Type" "image/png"}
                  :response-format (ajax-json/json-response-format {:keywords? true})
                  :timeout         10000
                  :on-success      [upload-success-event nil]
                  :on-failure      [::logo-upload-failure upload-failure-event]}}))

(reg-event-fx
  ::logo-upload-failure
  db/default-interceptors
  (fn [_ [upload-failure-event resp]]
    (js/console.error "There was an error uploading the company logo." upload-failure-event resp)
    {:dispatch-n [[:error/set-global "There was an error uploading the company logo."]
                  [upload-failure-event]]}))
