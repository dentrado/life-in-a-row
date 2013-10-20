; Martin Forsgren 2013
; inspired by http://clj-me.cgrand.net/index.php?s=game+of+life

(ns cljs-test.client.main
  (:require [clojure.set :refer [map-invert]]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [cljs.core.async.macros :only [go]]
               [dommy.macros :only [node sel sel1]]))

(defn dbg [e] (.log js/console (pr-str e) e) e)

;;;;;;;;;;;;;;;
(def grid-size 10)

(def board {[5 5] :p1, [5 6] :p1, [5 7] :p1, [5 8] :p1,
            [6 5] :p2, [6 6] :p1})

(def directions
  (for [dx [-1 0 1], dy [-1 0 1]
        :when (not= [dx dy] [0 0])]
    [dx dy]))

;; logic
(def opponent {:p1 :p2, :p2 :p1})

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
         :let [row (take grid-size (iterate #(pos+ dir %) pos))
               colors (map board row)]
         :when (apply = colors)] ; whole row occupied by same color
     (first colors))))

;; UI

(def cell-divs (repeatedly (* grid-size grid-size) #(node [:div.cell])))

(def grid (reduce dommy/append! [:div#grid] cell-divs))

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

(defn draw! [board]
  (doseq [div cell-divs]
    (dommy/remove-class! div :p1 :p2))
  (doseq [[cell player] board]
    (dommy/add-class! (cell->div cell) player)))

(defn game-loop [cell-click-ch first-player]
  (go (loop [player first-player
             move (<! cell-click-ch)
             board board]
        (let [board (assoc board move player)
              next-board (step board)]
          (draw! board)
          (<! (timeout 200))
          (draw! next-board)
          (recur (opponent player)
                 (<! cell-click-ch)
                 next-board)))))

(def cell-click-ch (event-chan grid :click #(dbg (div->cell (.-target %)))))

(comment
  (dommy/append! (sel1 :body) grid)
  (draw! board)
  (game-loop cell-click-ch)
  (go (dbg (str "hej" (<! cell-click-ch))))
  )

;(defn setup! []
;  (dommy/append! (sel1 :body) grid)
;  (let [a 1]
;     (game-loop cell-click-ch)))
;

;(dommy/unlisten! (sel1 :#grid) :click dbg)

;(add-grid!)

;(node [:p "Hello"])

