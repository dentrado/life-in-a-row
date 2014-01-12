(ns life-in-a-row.core
  (:require [clojure.edn :as edn]
            [org.httpkit.server :as hk]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]))

(def clients (atom {})) ;player(uuid) -> channel

(defn new-uuid []
  (java.util.UUID/randomUUID))

(defn move-handler [req]
  (hk/with-channel req chan
    (swap! clients assoc :en-uuid chan)
    (println chan " connected.")
    (hk/on-close chan (fn [status]
                        (swap! clients dissoc :en-uuid)
                        (println chan " disconnected. status: " status)))
    (println (:body req))))

(defroutes life
  (GET "/" [] (resource-response "index.html"))
  (POST "/new-game" [] (str (new-uuid)))
  (POST "/move" [] move-handler))

(defn -main [& args]
  (hk/run-server
   (-> #'life
       handler/site
       (wrap-resource ".")
       wrap-file-info)
   {:port 4001}))
