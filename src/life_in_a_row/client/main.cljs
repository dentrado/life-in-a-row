; Martin Forsgren 2013
; inspired by http://clj-me.cgrand.net/index.php?s=game+of+life

(ns life-in-a-row.client.main
  (:refer-clojure :exclude [read-string])
  (:require [cljs.reader :refer [read-string]]
            [clojure.set :refer [map-invert]]
            [clojure.browser.event :as event]
            [clojure.browser.net :as net]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [cljs.core.async.macros :only [go]]
               [dommy.macros :only [node sel sel1]]))

(defn dbg [tag e]
  (.log js/console tag (pr-str e) e) e)

(def grid-size 10)
(def nr-to-win 4)

(def board {[5 5] :p1, [5 6] :p1, [5 7] :p1, [5 8] :p1,
            [6 5] :p2, [6 6] :p1})

(def directions
  (for [dx [-1 0 1], dy [-1 0 1]
        :when (not= [dx dy] [0 0])]
    [dx dy]))

;; Logic

(def get-opponent {:p1 :p2, :p2 :p1})

(defn pos+
  "adds two positions/directions"
  [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn neighbours [[pos player]]
  (for [dir directions]
    [(pos+ pos dir) player]))

(defn step
  "A living cell keeps the color from the previous generation.
   A new living cell gets the most common color among its neighbours."
  [board]
  (into {}
        (for [[pos pos-player-pairs] (group-by first (mapcat neighbours board))
              :let [n (count pos-player-pairs)]
              :when (or (= n 3) (and (= n 2) (board pos)))]
          (let [player (or (board pos)                  ; same as prev gen
                           (->> (vals pos-player-pairs) ; most common neighbour
                                frequencies
                                (sort-by (comp - val))
                                ffirst))]
            [pos player]))))

(defn get-winner
  "returns the winner or nil if no one has won"
  [board]
  (first
   (for [[pos player] board
         dir directions
         :let [row (take nr-to-win (iterate #(pos+ dir %) pos))
               colors (map board row)]
         :when (apply = colors)] ; whole row occupied by same color
     (first colors))))

;; UI

(def cell-divs (repeatedly (* grid-size grid-size) #(node [:div.cell])))

(def grid (reduce dommy/append! [:div#grid] cell-divs))

(def turn-indicator (node [:div#turn-indicator.cell]))

(def info (node [:div#info "Current player:" turn-indicator]))

(def div->cell
  (into {}
        (map vector
             cell-divs
             (for [x (range grid-size), y (range grid-size)] [x y]))))

(def cell->div (map-invert div->cell))

(defn event-chan
  [elem event-type f]
  (let [ch (chan)]
    (dommy/listen! elem event-type #(put! ch (f %)))
    ch))

(defn draw! [board cur-player]
  (dommy/remove-class! turn-indicator :p1 :p2)
  (dommy/add-class! turn-indicator cur-player)
  (doseq [div cell-divs]
    (dommy/remove-class! div :p1 :p2))
  (doseq [[cell player] board]
    (when-let [div (cell->div cell)] ;don't try to draw outside the grid
      (dommy/add-class! div player))))

; same computer -> p1chan = p2chan = clickchan
; online: p1chan = clickchan, p2chan = ajax
(defn game-loop [p1-move-chan p2-move-chan starting-player]
  (let [chans {:p1 p1-move-chan :p2 p2-move-chan}]
    (go (loop [player   starting-player
               opponent (get-opponent player)
               board    board]
          (let [move (<! (chans player))]
            (if (board move) ; ignore clicks on living cells
              (recur player opponent board)
              (let [board (assoc board move player)
                    next-board (step board)]
                (draw! board player)
                (<! (timeout 200))
                (draw! next-board opponent)
                (if-let [winner (get-winner next-board)]
                  (js/confirm (str winner " won!"))
                  (recur opponent
                         player
                         next-board)))))))))

(def cell-click-ch (event-chan grid :click #(dbg  "clk" (div->cell (.-target %)))))

(defn setup! []
  (dommy/append! (sel1 :body) grid info)
  (draw! board :p1)
  (game-loop cell-click-ch cell-click-ch :p1))

(defn new-game! []
  (let [url "http://localhost:4001/new-game"
        xhr (net/xhr-connection)]
    (event/listen xhr :error #(dbg "error" %))
    (event/listen xhr :success  #(dbg (-> % .-target .getResponseText read-string)))
    (net/transmit xhr url "POST" {:q "hesjan"})))

;(setup!)

;(node [:p "Hello"])

