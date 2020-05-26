(ns chess.pieces
    (:require [chess.utils :refer [key-by const]]))

(defn vacant [] {:type "empty"})
(defn is-vacant? [piece] (= (get piece :type) "empty"))

(defn offsets->moves [offsets]
    (defn build-move [[offset-x offset-y]]
        (fn [color [x y]]
            (let [x-operator (if (= color "white") + -)]
                [(x-operator x offset-x) (+ y offset-y)])))
    (map build-move offsets))

(def piece-symbols {"pawn" "\u2659"
                    "rook" "\u2656"
                    "knight" "\u2658"
                    "bishop" "\u2657"
                    "queen" "\u2655"
                    "king" "\u2654"})

(def pawn {:type "pawn"
           :weight 100
           :moves [{
            :can-capture false
            :transformations (offsets->moves [[-1 0]])
            :get-limit (fn [piece] (if (= (get piece :move-count) 0) 2 1))
           } {
            :can-advance false
            :transformations (offsets->moves [[-1 -1] [-1 1]])
            :get-limit (const 1)
           }]})

(def rook {:type "rook"
           :weight 500
           :moves [{
            :transformations (offsets->moves [[0 1] [0 -1] [1 0] [-1 0]])
           }]})

(def knight {:type "knight"
             :weight 320
             :moves [{
                :get-limit (const 1)
                :transformations (offsets->moves [[1 2] [2 1] [1 -2] [2 -1] [-1 -2] [-2 -1] [-1 2] [-2 1]])
             }]})

(def bishop {:type "bishop"
             :weight 330
             :moves [{
              :transformations (offsets->moves [[1 1] [1 -1] [-1 1] [-1 -1]])
             }]})

(def queen {:type "queen"
            :weight 900
            :moves [{
                :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
            }]})

(def king {:type "king"
           :weight 20000
           :moves [{
            :get-limit (const 1)
            :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
           }]})

(def pieces-by-type (key-by :type [pawn rook knight bishop queen king]))

(defn build-piece [color type]
    {:color color
     :type type
     :weight (let [weight (-> (get pieces-by-type type) (get :weight))]
        (if (= color "white")
            weight
            (- weight)))
     :move-count 0})

(def b (partial build-piece "black"))
(def w (partial build-piece "white"))

; ----------------------------------------------------------------------------------------------------------------------
; Position weightings: per https://www.chessprogramming.org/Simplified_Evaluation_Function, different pieces are more or
; less valuable at different places on the board. (A queen can do more in the center of the board than it can in a
; corner!) These per-position weightings nudge the raw material score up or down depending on board layout. Weightings
; are optimized for white and later mirrored for black.

(def position-weightings {
    "pawn" [[0 0 0 0 0 0 0 0]
            [50 50 50 50 50 50 50 50]
            [10 10 20 30 30 20 10 10]
            [5 5 10 25 25 10 5 5]
            [0 0 0 20 20 0 0 0]
            [5 -5 -10 0 0 -10 -5 5]
            [5 10 10 -20 -20 10 10 5]
            [0 0 0 0 0 0 0 0]]
    "knight" [[-50 -40 -30 -30 -30 -30 -40 -50]
              [-40 -20 0 0 0 0 -20 -40]
              [-30 0 10 15 15 10 0 -30]
              [-30 5 15 20 20 15 5 -30]
              [-30 0 15 20 20 15 0 -30]
              [-30 5 10 15 15 10 5 -30]
              [-40 -20 0 5 5 0 -20 -40]
              [-50 -40 -30 -30 -30 -30 -40 -50]]
    "bishop" [[-20 -10 -10 -10 -10 -10 -10 -20]
              [-10 0 0 0 0 0 0 -10]
              [-10 0 5 0 0 5 0 -10]
              [-10 5 5 0 0 5 5 -10]
              [-10 0 0 0 0 0 0 -10]
              [-10 0 0 0 0 0 0 -10]
              [-10 5 0 0 0 0 5 -10]
              [-20 -10 -10 -10 -10 -10 -10 -20]]
    "rook" [[0 0 0 0 0 0 0 0]
            [5 10 10 10 10 10 10 5]
            [-5 0 0 0 0 0 0 -5]
            [-5 0 0 0 0 0 0 -5]
            [-5 0 0 0 0 0 0 -5]
            [-5 0 0 0 0 0 0 -5]
            [-5 0 0 0 0 0 0 -5]
            [0 0 0 5 5 0 0 0]]
    "queen" [[-20 -10 -10 -5 -5 -10 -10 -20]
             [-10 0 0 0 0 0 0 -10]
             [-10 0 5 5 5 5 0 -10]
             [-5 0 5 5 5 5 0 -5]
             [0 0 5 5 5 5 0 -5]
             [-10 5 5 5 5 5 0 -10]
             [-10 0 5 0 0 0 0 -10]
             [-20 -10 -10 -5 -5 -10 -10 -20]]
    "king" [[-30 -40 -40 -50 -50 -40 -40 -30]
            [-30 -40 -40 -50 -50 -40 -40 -30]
            [-30 -40 -40 -50 -50 -40 -40 -30]
            [-30 -40 -40 -50 -50 -40 -40 -30]
            [-20 -30 -30 -40 -40 -30 -30 -20]
            [-10 -20 -20 -20 -20 -20 -20 -10]
            [20 20 0 0 0 0 20 20]
            [20 30 10 0 0 10 30 20]]
})

(defn mirror-position-weightings [board]
    "Takes a layout of position weightings for white, and flips it for black"
    (->> board
         (map (fn [row]
            (->> row
                 (map -)
                 vec)))
         reverse
         vec))

(def position-weightings-by-color {
    "white" position-weightings
    "black" (reduce (fn [total [color weighting]]
                        (assoc total color (mirror-position-weightings weighting)))
                    {}
                    position-weightings)
})

(defn get-position-weighting [color piece-type position]
    (-> (get position-weightings-by-color color)
        (get piece-type)
        (get-in position)))
