(ns chess.components
    (:require [chess.moves :refer [get-moves-from-position]]
              [chess.pieces :refer [piece-strings is-vacant?]]))

(def square-size 75)
(def piece-size 60)

(defn reverse-color [color] (if (= color :black) "white" "black"))

(defn get-square-background-color [row-idx col-idx available-moves]
    (if (some #(= % [row-idx col-idx]) available-moves)
        "springgreen"
        (if (= (even? row-idx) (even? col-idx)) "white" "gray")))

(defn square-ui [{:keys [row-idx col-idx available-moves set-hovered-coords]} children]
    ^{:key (str row-idx col-idx)}
    [:div {
        :style {
            :height square-size
            :width square-size
            :align-items "center"
            :justify-content "center"
            :background-color (get-square-background-color row-idx col-idx available-moves)
            :display "flex"}
        :on-mouse-enter #(apply set-hovered-coords [[row-idx col-idx]])
        :on-mouse-leave #(apply set-hovered-coords [nil])}
        children])

(defn piece-ui [piece]
    (when (not (is-vacant? piece))
        [:div {:style {
                :background-color (get piece :color)
                :color (reverse-color (get piece :color))
                :height piece-size
                :width piece-size
                :border-radius piece-size
                :display "flex"
                :align-items "center"
                :justify-content "center"
                :border "1px solid"}}
            [:div (piece-strings (get piece :type))]]))

(defn row-label [idx]
    [:div {:style {:align-self "center" :padding-right 10}} (- 8 idx)])

(defn col-labels [char]
    [:div {:style {:display "flex" :margin-left 18 :margin-top 10}}
        (map (fn [char]
                ^{:key char}
                [:div {:style {
                        :width square-size
                        :height square-size}}
                    char])
             ["A" "B" "C" "D" "E" "F" "G" "H"])])

(defn board-ui [board hovered-coords {:keys [set-hovered-coords]}]
    [:div {:style {:display "inline-block"}}
        (let [available-moves (get-moves-from-position board hovered-coords)]
        (map-indexed (fn [row-idx row]
                        ^{:key row-idx}
                        [:div {:style {:display "flex"}}
                            [row-label row-idx]
                            (map-indexed (fn [col-idx piece]
                                            ^{:key (str row-idx col-idx)}
                                            [square-ui {
                                                :row-idx row-idx
                                                :col-idx col-idx
                                                :available-moves available-moves
                                                :set-hovered-coords set-hovered-coords}
                                                [piece-ui piece]])
                                         row)])
                     board))
        [col-labels]])

(defn app [board hovered-coords handlers]
    [:div {:style { :text-align "center" :margin-top 50 }}
        [board-ui @board @hovered-coords handlers]])
