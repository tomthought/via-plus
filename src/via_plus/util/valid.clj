(ns via-plus.util.valid
  (:require [clojure.string :as st]))

;;; Declarations

(def ^:private email-pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(def ^:private phone-number-pattern #"^[+]*[(]{0,1}[0-9]{1,4}[)]{0,1}[-\s\./0-9]*$")

;;; API

(defn email?
  [email]
  (boolean
   (when (string? email)
     (->> email
          (st/lower-case)
          (st/trim)
          (re-matches email-pattern)))))

(defn phone-number?
  [phone-number]
  (boolean
   (when (string? phone-number)
     (->> phone-number
          (st/lower-case)
          (st/trim)
          (re-matches phone-number-pattern)))))
