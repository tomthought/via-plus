(ns via-plus.util.context
  (:require [clojure.string :as st]))

(defn ring-request->ip
  [ring-request]
  (let [x-forwarded-for (-> ring-request
                            :headers
                            (get "x-forwarded-for"))]
    (or (not-empty (first (st/split (str x-forwarded-for) #",")))
        (:remote-addr ring-request))))

(defn context->ip
  [context]
  (-> context
      :request
      :ring-request
      ring-request->ip))

(defn context->client-id
  [context]
  (-> context :request :client-id))

(defn update-query-v
  [context query-v]
  (cond
    (:event context)
    (assoc context :event query-v)

    (->> context
         :request
         :payload
         second
         :query-v)
    (assoc-in context [:request :payload 1 :query-v] query-v)

    :else context))

(defn context->query-v
  [context]
  (or (:event context)
      (->> context
           :request
           :payload
           second
           :query-v)))
