(ns chess.worker
    (:require [chess.gameplay :refer [get-next-move]]))

(defn load! []
    (js/self.addEventListener "message"
        (fn [^js evt]
            (let [start-time (.now js/Date)
                  data (js->clj (.. evt -data) :keywordize-keys true)
                  res (get-next-move (get data :board) (get data :color) (get data :search-depth))]
				(println "Search time:" (- (.now js/Date) start-time) "ms")
                (js/postMessage (clj->js res))))))
