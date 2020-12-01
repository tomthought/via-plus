(ns via-plus.ring
  (:require [ring.util.response :refer [resource-response content-type] :as resp]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.cors :refer [wrap-cors origin]]
            [io.pedestal.http.secure-headers :as secure-headers]
            [utilis.map :refer [map-vals]]
            [clojure.string :as st]))

;;; Declarations

(declare ->csp-string
         wrap-content-security-policy
         wrap-additional-secure-headers)

;;; Defaults

(def ring-site-defaults
  (-> ring-defaults/site-defaults
      (assoc-in [:security :frame-options] :deny)
      (assoc-in [:security :hsts] true)
      (assoc-in [:security :anti-forgery] true)))

(def default-content-security-policy
  {:default-src :self
   :script-src :self
   :style-src [:self :unsafe-inline]
   :img-src :self
   :connect-src :self
   :font-src :self
   :object-src :none
   :media-src :self
   :report-uri "/report-csp-violation"
   :child-src :none
   :form-action :self
   :frame-ancestors :none})

;;; API

(defn wrap-defaults
  "When using the default values, this ring handler will provide a reasonably
  secure Content Security Policy (CSP) header. Additional secure headers are
  controlled with the :secure-headers? flag.

  Before deploying this handler, make sure to review all the headers included
  on the generated responses to control the desired level of security on your
  application."
  [handler & {:keys [defaults
                     content-security-policy
                     secure-headers?
                     gzip?]
              :or {defaults ring-site-defaults
                   content-security-policy default-content-security-policy
                   secure-headers? true
                   gzip? true}}]
  (cond-> handler
    defaults (ring-defaults/wrap-defaults defaults)
    content-security-policy (wrap-content-security-policy content-security-policy)
    secure-headers? (wrap-additional-secure-headers)
    gzip? (wrap-gzip)))

(defn wrap-content-security-policy
  [handler csp]
  (let [csp-header (->> csp
                        (map-vals ->csp-string)
                        (secure-headers/content-security-policy-header))]
    (fn [request]
      (when-let [response (handler request)]
        (resp/header response "Content-Security-Policy" csp-header)))))

(defn wrap-additional-secure-headers
  [handler]
  (fn [request]
    (some-> (handler request)
            (resp/header "X-Download-Options" "noopen")
            (resp/header "X-Permitted-Cross-Domain-Policies" "none"))))

;;; Private

(defn- ->csp-string
  [value]
  (cond
    (keyword? value) (format "'%s'" (name value))
    (string? value) value
    (or (sequential? value) (set? value)) (->> value (map ->csp-string) (st/join " "))
    :else (throw (ex-info "Unable to create CSP string from value." {:value value}))))
