; Martin Forsgren 2013
; inspired by http://clj-me.cgrand.net/index.php?s=game+of+life

(ns cljs-test.client.main
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

;;;;;;;;;;;;;;;
(def grid-size 10)

(def board {[10 10] :p1, [10 11] :p1, [10 12] :p1, [10 13] :p1,
            [11 10] :p2, [11 11] :p1})

(def directions
  (for [dx [-1 0 1], dy [-1 0 1]
        :when (not= [dx dy] [0 0])]
    [dx dy]))

;; logic

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
         :let [row (take row-length (iterate #(pos+ dir %) pos))
               colors (map board row)]
         :when (apply = colors)] ; whole row occupied by same color
     (first colors))))

;; UI

(def cell-divs (repeatedly (* grid-size grid-size) #(node [:div.cell])))

(def div->cell
  (into {}
        (map vector
             cell-divs
             (for [x (range grid-size), y (range grid-size)] [x y]))))

(defn  add-grid! []
  (let [grid (reduce dommy/append! [:div#grid] cell-divs)]
    (dommy/append! (sel1 :body) grid)))


(dommy/listen!
 (sel1 :#grid) :click
 (fn [e] (dbg (div->cell (.-target e)))))

;(dommy/unlisten! (sel1 :#grid) :click dbg)

;(add-grid!)

;(node [:p "Hello"])

;(defn dbg [e] (.log js/console (pr-str e) e) e)
