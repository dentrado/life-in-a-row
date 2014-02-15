(ns life-in-a-row.core
  (:require [clojure.edn :as edn]
            [org.httpkit.server :as hk]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]))

;; State
(def opponents (atom {})) ; player-id -> player-id
(def channels (atom {}))  ; player-id -> channel

(defn get-opponent-chan [user-id]
  (@channels (@opponents user-id)))

;; Logic
(defn new-uuid []
  (java.util.UUID/randomUUID))

(defn new-game-handler [_]
  (let [p1 (new-uuid)
        p2 (new-uuid)]
    (swap! opponents assoc p1 p2 p2 p1)
    (pr-str [p1 p2])))

(defn join-handler
  [player-id request]
  (hk/with-channel request chan
                   (swap! channels assoc player-id chan) ; TODO add check if right num of players.
                   (println chan " connected.")
                   (hk/on-close chan (fn [status]
                                       (swap! channels dissoc player-id)
                                       (println chan " disconnected. status: " status))))
  (println (:session request)))

(defn move-handler [{:keys [player-id body]}]
  (if-let [chan (get-opponent-chan player-id)]
    (hk/send! chan body)
    (println "ERROR: opponent not there!")))

;; Routing
(defroutes life

  (GET "/" [] (resource-response "index.html"))

  (POST "/new-game" [] new-game-handler) ;; TODO: cleanup after a while if noone connects

  (POST "/join-game/:player-id" {{player-id :player-id} :params :as request}
        (join-handler player-id request))

  (POST "/move" [] move-handler)) ;render on a fn calls the fn



(def app
  (-> #'life
      handler/site ; includes wrap-session
      (wrap-resource ".")
      wrap-file-info))

(defn -main [& args]
  (hk/run-server #'app {:port 4001}))

;(-main)

;; Unused

(defn wrap-user-id [handler]
  (fn [req]
    (let [user-id (or (get-in req [:session :user-id]) (new-uuid))]
      (-> req
          (assoc-in [:session :user-id] user-id)
          (assoc :user-id user-id)
          handler))))

@channels
@opponents
(reset! opponents)
