(ns via-plus.schema
  (:require [via-plus.util.context :as ctx]
            [via-plus.util.reply :as reply]
            [via-plus.util.schema :as s]
            [aave.core :as a]
            [aave.code :as code]
            [aave.syntax.ghostwheel :as syntax.gw]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [utilis.fn :refer [fsafe]]
            [signum.interceptors :refer [->interceptor]]
            [clojure.walk :refer [postwalk]]))

;;; Declarations

(declare tx-ret-schema remap-underscores)

;;; API

(defmacro >fn
  "Args are the same as >defn from aave (https://github.com/teknql/aave), but
  without a function name (as nothing will be defined at the namespace level)."
  {:arglists '([doc-string? attr-map? [params*] [schemas*]? body])}
  [& args]
  (let [cfg (-> (symbol (str "validate-" (gensym)))
                (cons args)
                (syntax.gw/parse)
                (merge {:private false})
                (update :meta-map (partial merge {::decode true ::scrub true})))
        {::keys [decode scrub]} (:meta-map cfg)
        ret? (boolean (:ret-schema cfg))
        {:keys [ret-schema]
         :as cfg} (cond-> cfg
                    ret? (update :ret-schema remap-underscores)
                    (:param-schema cfg) (update :param-schema remap-underscores))
        cfg (cond-> cfg
              ret? (-> (assoc :orig-ret-schema ret-schema)
                       (update :ret-schema tx-ret-schema)
                       (update :params+body (fn [[params body]]
                                              [params `(cond->> ~body
                                                         ~decode (s/decode-body ~ret-schema)
                                                         ~scrub (s/scrub-body ~ret-schema))]))))]
    (if ret?
      `(s/generate ~cfg)
      `(code/generate ~cfg))))

;;; Implementation

(def underscore (symbol "_"))
(def underscore? (partial = underscore))

(defn- remap-underscores
  [x]
  (postwalk #(if (underscore? %) 'any? %) x))

(defn tx-ret-schema
  [schema]
  [:multi {::outstrument true
           :dispatch '(fn [result]
                        (if (:via/status result)
                          (if (= 200 (:via/status result))
                            :via/reply-ok
                            :via/reply-error)
                          :via/sub))}
   [:via/reply-ok [:map [:via/reply schema]]]
   [:via/reply-error 'any?]
   [:via/sub schema]])
