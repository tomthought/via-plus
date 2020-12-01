(ns via-plus.log
  (:require [via-plus.util.context :refer [context->query-v
                                           context->ip
                                           context->client-id
                                           update-query-v]]
            [via-plus.util.valid :refer [email? phone-number?]]
            [signum.interceptors :refer [->interceptor]]
            [clojure.walk :refer [postwalk]]
            [utilis.fn :refer [fsafe]]
            [utilis.map :refer [map-vals]]
            [clojure.tools.logging :as log]
            [clojure.string :as st]))

;;; Declarations

(declare key-vals->log-string mask-log-values)

;;; API

(def interceptor
  (->interceptor
   :id ::interceptor
   :before (fn [context]
             (assoc context
                    ::log {:level :info
                           :values {:cid {:label "cid"
                                          :value (context->client-id context)}
                                    :ip {:label "ip"
                                         :value (context->ip context)}
                                    :query-v {:value (context->query-v context)}}}))
   :after (fn [context]
            (let [{::keys [log]} context
                  string (key-vals->log-string (:values log))]
              (condp = (:level log)
                :debug (log/debug string)
                :info (log/info string)
                :warn (log/warn string)
                :error (log/error string)
                (throw (ex-info "Unknown log level." {:level (:level log)})))
              context))))

(defn mask
  "This interceptor can be added after via-plus.log/interceptor in order to mask
  values that match 'pred' before they are logged."
  [pred & {:keys [deep? mask-string]
           :or {deep? true
                mask-string "*****"}}]
  (throw (ex-info "Not Implemented" {}))
  (->interceptor
   :id ::mask
   :before (fn [context]
             (when (not (::log context))
               (throw (ex-info "Mask interceptor must run after the log interceptor." {})))
             (update-in context [::log :values] (partial mask-log-values pred deep? mask-string)))))

(defn sensitive?
  [value]
  (boolean
   (when (string? value)
     (or (email? value)
         (phone-number? value)))))

;;; Implementation

(def ^:private max-string-length 500)

(defn- truncate
  ([string] (truncate string max-string-length))
  ([string max-chars]
   (if (< (count string) max-chars)
     string
     (str (->> string
               (take (- max-chars 3))
               (apply str))
          "..."))))

(defn- value->string
  [value]
  (cond
    (keyword? value) (name value)
    (number? value) (str value)
    (boolean? value) (str value)
    (string? value) (truncate value)
    :else (truncate (pr-str value))))

(defn- key-vals->log-string
  [key-vals]
  (let [labels (->> (vals key-vals)
                    (filter :label)
                    (sort-by :label)
                    (map (fn [{:keys [label value]}]
                           (str (name label) "=" (value->string value)))))
        no-labels (->> key-vals
                       (remove (comp :label second))
                       (sort-by first)
                       (map (comp value->string :value second)))]
    (st/join " " (concat labels no-labels))))

(defn- mask-value
  [pred deep? mask-string value]
  (cond
    (and deep? (coll? value)) (postwalk #(if (pred %) mask-string %) value)
    (pred value) mask-string
    :else value))

(defn- mask-log-values
  [pred deep? mask-string log-values]
  (map-vals #(update % :value (partial mask-value pred deep? mask-string))
            log-values))
