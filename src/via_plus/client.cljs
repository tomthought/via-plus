(ns via-plus.client
  (:require [re-frame-utils.core :as rfu]
            [via.endpoint :as via]
            [via.subs :refer [subscribe]]
            [via.fx]
            [re-frame.core :refer [dispatch reg-event-fx reg-fx reg-sub]]
            [integrant.core :as ig]))

;;; Declarations

(def debug? js/goog.DEBUG)
(defonce system-atom (atom nil))

(declare stop-system! start-system!)

;;; Configuration

(def default-config
  {:via/endpoint {}
   :via/events {:endpoint (ig/ref :via/endpoint)}
   :via/subs {:endpoint (ig/ref :via/endpoint)}
   :via/fx {:endpoint (ig/ref :via/endpoint)}
   :via-plus/client {:endpoint (ig/ref :via/endpoint)}})

;;; API

(defn start
  [{:keys [config via-endpoint on-connect on-disconnect]
    :or {config default-config}}]
  (dispatch
   [:via-plus.client/initialize
    {:config (cond-> config
               via-endpoint (assoc-in [:via/endpoint :url] via-endpoint)
               on-connect (assoc-in [:via-plus/client :on-connect] on-connect)
               on-disconnect (assoc-in [:via-plus/client :on-disconnect] on-disconnect))}]))

;;; Event Handlers

(reg-event-fx
 :via-plus.client/initialize
 (fn [_ [_ {:keys [config]}]]
   {:via-plus.client.system/start {:config config}}))

(reg-event-fx
 :via.dispatch/on-success
 (fn [_ [_ handler result]]
   (cond
     (vector? handler) {:dispatch handler}
     (fn? handler) (handler result)
     :else nil)))

(reg-event-fx
 :via.dispatch/on-failure
 (fn [_ [_ handler result]]
   (cond
     (vector? handler) {:dispatch handler}
     (fn? handler) (handler result)
     :else nil)))

(reg-event-fx
 :via.dispatch/on-timeout
 (fn [_ [_ handler]]
   (cond
     (vector? handler) {:dispatch handler}
     (fn? handler) (handler)
     :else nil)))

(reg-event-fx
 :via/dispatch
 (fn [_ [_ {:keys [event on-success on-failure on-timeout]}]]
   {:via/dispatch {:event event
                   :on-success [:via.dispatch/on-success on-success]
                   :on-failure [:via.dispatch/on-failure on-failure]
                   :on-timeout [:via.dispatch/on-timeout on-timeout]}}))

(reg-event-fx
 :via-plus.client/start
 (fn [_ [_ args]]
   {:via-plus.client/start args}))

;;; Effect Handlers

(reg-fx
 :via-plus.client.system/stop
 (fn []
   (stop-system!)))

(reg-fx
 :via-plus.client.system/start
 (fn [{:keys [config]}]
   (start-system! config)))

(reg-fx
 :via-plus.client/start
 (fn [args]
   (start args)))

;;; Integrant Client

(defmethod ig/init-key :via-plus/client
  [_ {:keys [endpoint on-connect on-disconnect]}]
  {:endpoint endpoint
   :sub-key (when-let [subs (not-empty
                             (merge (when on-connect {:open #(dispatch (conj on-connect %))})
                                    (when on-disconnect {:close #(dispatch (conj on-disconnect %))})))]
              (via/subscribe endpoint subs))})

(defmethod ig/halt-key! :via-plus/client
  [_ {:keys [endpoint sub-key]}]
  (when sub-key (via/dispose endpoint sub-key)))

;;; Implementation

(defn- stop-system!
  []
  (when-let [system @system-atom]
    (ig/halt! system)))

(defn- start-system!
  [config]
  (stop-system!)
  (when-let [system (ig/init config)]
    (reset! system-atom system)))
