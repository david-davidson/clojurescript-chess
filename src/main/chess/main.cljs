(ns chess.main
    (:require [reagent.core :as reagent]
              [chess.components :refer [app]]
              [chess.worker_utils :refer [search-series search-parallel]]
              [chess.utils :refer [reverse-color]]
              [chess.moves :refer [is-check? is-checkmate?]]
              [chess.board :refer [get-initial-board move-piece]]))

(declare set-piece)

(defonce search-depth (reagent/atom 5))
(defn set-search-depth [depth] (reset! search-depth depth))

(defonce board (reagent/atom (get-initial-board)))
(defn set-board [new-board] (reset! board new-board))

(defonce history (atom (list @board)))
(defn push-history [board] (reset! history (conj @history board)))

(defonce hovered-coords (reagent/atom nil))
(defn set-hovered-coords [coords] (reset! hovered-coords coords))

(defonce active-color (reagent/atom "white"))
(defn set-active-color [color] (reset! active-color color))

(defonce parallel (reagent/atom true))
(defn set-parallel [new-parallel] (reset! parallel new-parallel))

(set! js/getBoard (fn [offset]
    "For debugging when no REPL is available (e.g., deployed builds) via `window.getBoard()`.
    `offset` represents number of steps back in game history (or current state if omitted)."
    (->> (or offset 0)
         (nth @history)
         (println))))

(defn init-next-move []
    (when (= @active-color "black")
        (let [start-time (.now js/Date)
              search-fn (if @parallel search-parallel search-series)]
            (-> (search-fn @board @active-color @search-depth)
                (.then (fn [[from to]]
                    (println "Search time:" (- (.now js/Date) start-time))
                    (set-piece @active-color from to)))))))

(defn alert-check-checkmate [new-board from-color]
    (js/setTimeout (fn []
        (when (is-check? new-board (reverse-color from-color))
              (if (is-checkmate? new-board (reverse-color from-color))
                  (js/alert "Checkmate")
                  (js/alert "Check")))) 20)) ; 20ms to give time for board to render latest move

(defn set-piece [from-color from to]
    (let [new-board (move-piece @board from to)
          new-color (reverse-color from-color)]
        (set-active-color new-color)
        (set-board new-board)
        (push-history new-board)
        (alert-check-checkmate new-board from-color)
        (init-next-move)))

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (reagent/render [app
                     board
                     hovered-coords
                     active-color
                     parallel
                     search-depth
                     {:set-hovered-coords set-hovered-coords
                      :set-piece set-piece
                      :set-search-depth set-search-depth
                      :set-parallel set-parallel}]
                    (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    ; We need to explicitly unmount the old app to correctly tear down `react-dnd` event listeners:
    (reagent/unmount-component-at-node (.getElementById js/document "app"))
    (mount!))
