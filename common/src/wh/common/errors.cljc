(ns wh.common.errors)

(def error-map
  {:invalid-arguments "Please enter correct email."
   :duplicate-user "Account with the email already exists."
   :invalid-email "Provided email does not seem to be real or able to receive emails."
   :missing-consent "Please provide consent with processing of your data."
   :invalid-name "Please enter your name and surname (at least two words)."
   :could-not-update-job "There was an error creating/updating job, please try again later."})

(defn upsert-user-error-message [error-key]
  (when error-key
    (get error-map error-key
         "There was an error creating/updating user, please try again later.")))
