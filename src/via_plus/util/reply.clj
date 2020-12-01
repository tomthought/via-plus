(ns via-plus.util.reply)

(defn error
  ([message status]
   (error message status nil))
  ([message status data]
   (merge {:via/status status}
          (when (or message data)
            {:via/reply (merge
                         (when message {:message message})
                         (when data {:data data}))}))))

(defn short-circuit
  ([message] (short-circuit message 500))
  ([message status] (short-circuit message status nil))
  ([message status data]
   {:effects (error message status data)
    :queue []}))
