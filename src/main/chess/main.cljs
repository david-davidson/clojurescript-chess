(ns chess.main
    (:require [reagent.core :as reagent]
              [chess.pieces :refer [piece-strings vacant]]
              [chess.components :refer [app]]
              [chess.board :refer [get-initial-board to-coordinates lookup-coords move-piece]]))

(defonce board (reagent/atom (get-initial-board)))
(defn set-board [new-board] (reset! board new-board))

(defonce hovered-coords (reagent/atom nil))
(defn set-hovered-coords [coords] (reset! hovered-coords coords))

; For manually moving pieces from the REPL:
(defn move-piece-repl [from to]
    (let [from-coords (to-coordinates from)
          to-coords (to-coordinates to)
          new-board (move-piece @board from-coords to-coords)]
        (set-board new-board)))

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (reagent/render [app board hovered-coords {:set-hovered-coords set-hovered-coords}]
                    (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    (mount!))
