(ns via-plus.util.schema
  (:require [via-plus.util.reply :as reply]
            [aave.core :as a]
            [aave.code :as code]
            [aave.syntax.ghostwheel :as syntax.gw]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [utilis.fn :refer [fsafe]]))

(defmacro generate
  [config]
  (let [ret-schema (:orig-ret-schema config)]
    `(let [f# (code/generate ~config)]
       (fn [& args#]
         (try (apply f# args#)
              (catch Exception e#
                (or (when-let [data# (ex-data e#)]
                      (when-let [schema# (:schema data#)]
                        (let [schema# (m/form schema#)]
                          (when (and (= :multi (first schema#))
                                     (:via-plus.schema/outstrument (second schema#)))
                            (let [error-data# {:explain data# :human (me/humanize data#)}]
                              (if (or (-> data# :value :via/status)
                                      (-> data# :value :via/reply))
                                (merge {:via-plus.schema/out-error error-data#}
                                       (reply/error "Output validation failed." 500))
                                (let [explain# (m/explain ~ret-schema (:value data#))]
                                  (throw (ex-info (.getLocalizedMessage e#) error-data#)))))))))
                    (throw e#))))))))

(defmacro tx-body
  [tx body]
  `(let [result# ~body]
     (if (and (:via/reply result#)
              (= 200 (:via/status result#)))
       (update result# :via/reply ~tx)
       (~tx result#))))

(defmacro decode-body
  [schema body]
  `(tx-body (fn [value#] (m/decode ~schema value# mt/string-transformer)) ~body))

(defmacro scrub-body
  [schema body]
  `(tx-body (fn [value#] (m/decode ~schema value# mt/strip-extra-keys-transformer)) ~body))
