(ns life-in-a-row.client.logic)

(def directions
  (for [dx [-1 0 1], dy [-1 0 1]
        :when (not= [dx dy] [0 0])]
    [dx dy]))

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
  [board nr-to-win]
  (first
   (for [[pos player] board
         dir directions
         :let [row (take nr-to-win (iterate #(pos+ dir %) pos))
               colors (map board row)]
         :when (apply = colors)] ; whole row occupied by same color
     (first colors))))
