; Martin Forsgren 2013
; inspired by http://clj-me.cgrand.net/index.php?s=game+of+life

(ns life-in-a-row.client.main
  (:refer-clojure :exclude [read-string])
  (:require [cljs.reader :refer [read-string]]
            [clojure.browser.event :as event]
            [clojure.browser.net :as net]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]]
            [clojure.set :refer [map-invert]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [life-in-a-row.client.util :refer [dbg]]
            [life-in-a-row.client.logic :as l])
  (:use-macros [cljs.core.async.macros :only [go go-loop]]
               [dommy.macros :only [node sel sel1]]))

(def grid-size 10)
(def nr-to-win 4)

(def board {[5 5] :p1, [5 6] :p1, [5 7] :p1, [5 8] :p1,
            [6 5] :p2, [6 6] :p1})

;; UI
(def cell-divs (repeatedly (* grid-size grid-size) #(node [:div.cell])))

(def grid (reduce dommy/append! [:div#grid] cell-divs))

(def turn-indicator (node [:div#turn-indicator.cell]))

(def info (node [:div#info "Current player:" turn-indicator]))

(def div->cell
  (into {} (map vector
                cell-divs
                (for [x (range grid-size), y (range grid-size)] [x y]))))

(def cell->div (map-invert div->cell))

(defn event-chan
  [elem event-type f]
  (let [ch (async/chan)]
    (dommy/listen! elem event-type #(async/put! ch (f %)))
    ch))

(defn draw! [board cur-player]
  (dommy/remove-class! turn-indicator :p1 :p2)
  (dommy/add-class! turn-indicator cur-player)
  (doseq [div cell-divs]
    (dommy/remove-class! div :p1 :p2))
  (doseq [[cell player] board]
    (when-let [div (cell->div cell)] ;don't try to draw outside the grid
      (dommy/add-class! div player))))

;;loopilop

; same computer -> p1chan = p2chan = clickchan
; online: p1chan = clickchan, p2chan = ajax
(defn game-loop [p1-move-chan p2-move-chan starting-player]
  (let [chans {:p1 p1-move-chan :p2 p2-move-chan}]
    (go-loop [player   starting-player
               opponent (l/get-opponent player)
               board    board]
          (let [move (<! (chans player))]
            (if (board move) ; ignore clicks on living cells
              (recur player opponent board)
              (let [board (assoc board move player)
                    next-board (l/step board)]
                (draw! board player)
                (<! (timeout 200))
                (draw! next-board opponent)
                (if-let [winner (l/get-winner next-board nr-to-win)]
                  (js/confirm (str winner " won!"))
                  (recur opponent
                         player
                         next-board))))))))

(defn send-move [chan]
  (go-loop [value (<! chan)]
    (let [url "http://localhost:4001/move" ;; todo def url somewhere
          xhr (net/xhr-connection)]
      (event/listen xhr :error #(dbg "errår" %))
      (event/listen xhr :success #(dbg "sukses" (.getResponseText (.-target %))))
      (net/transmit xhr url "POST" value)
    (recur (<! chan)))))

;;;; Server communication
(defn new-game! []
  (let [ch (async/chan)
        url "http://localhost:4001/new-game"
        xhr (net/xhr-connection)]
    (event/listen xhr :error #(dbg "error" %))
    (event/listen xhr :success
                   #(->> % .-target .getResponseText
                         read-string
                         (dbg "success")
                         (put! ch)))
    (net/transmit xhr url "POST" {:q "hesjan"})
    ch))

(defn join-game
  "returns a channel with the opponents moves"
  [player-id]
  (let [ch (async/chan)
        again (fn again []
                   (let [url "http://localhost:4001/opponent-moves"
                         xhr (net/xhr-connection)]
                     (event/listen xhr :error (fn [e] (dbg "error" e) (again)))
                     (event/listen xhr :success
                                   (fn [e]
                                     (->> e .-target .getResponseText
                                          read-string
                                          (dbg "success")
                                          (put! ch))
                                     (again)))
                     (net/transmit xhr url "POST" {:player-id player-id})))]
    (again)
    ch))

;;;; Setup
(def cell-click-ch (event-chan grid :click #(dbg "clk" (div->cell (.-target %)))))

(defn setup! []
  (dommy/append! (sel1 :body) grid info)
  (draw! board :p1)
  (game-loop cell-click-ch cell-click-ch :p1))


(async/<!! (new-game!))

(let [url "http://localhost:4001/opponent-moves"
      xhr (net/xhr-connection)]
  (event/listen xhr :error  #(dbg "error" %))
  (event/listen xhr :success
                (fn [e]
                  (->> e .-target .getResponseText
                       read-string
                       (dbg "success")
                       )))
  (net/transmit xhr url "POST" {:player-id "asdf"})
  )

  ;(setup!)

  ;TODO
  ; game over screen
; multiplayer over server:
; https://coderwall.com/p/y7dima
; channel send och recv
(comment
  (let [url "http://localhost:4001/new-game"
        xhr (net/xhr-connection)]
    (event/listen xhr :error #(dbg "errår" %))
    (event/listen xhr :success #(dbg "sukses" (.getResponseText (.-target %))))
    (net/transmit xhr url "POST" {:q "hesjas"})
    ))

