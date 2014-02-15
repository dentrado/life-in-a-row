(ns life-in-a-row.client.util)

(defn dbg [tag e]
  (.log js/console tag (pr-str e) e) e)
