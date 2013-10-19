(defproject life-in-a-row "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [prismatic/dommy "0.1.1"]]
  :plugins [[lein-cljsbuild "0.3.3"]]
  :cljsbuild
  {:builds
   [{:id "simple"
     :source-paths ["src/life_in_a_row"]
     :compiler {:optimizations :simple ;:advanced
                :pretty-print false
                :output-dir "out"
                :output-to "main.js"
                :source-map "main.js.map"}}]})
