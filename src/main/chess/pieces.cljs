(ns chess.pieces
    (:require [chess.utils :refer [key-by const]]))

(defn build-piece [color type]
    {:color color :type type :move-count 0})

(def b (partial build-piece :black))
(def w (partial build-piece :white))

(defn vacant [] {:type :empty})
(defn is-vacant? [piece] (= (get piece :type) :empty))

(defn offsets->moves [offsets]
    (defn build-move [[offset-x offset-y]]
        (fn [color [x y]]
            (let [x-operator (if (= color :white) + -)]
                [(x-operator x offset-x) (+ y offset-y)])))
    (map build-move offsets))

(def piece-symbols {:pawn "\u2659"
                    :rook "\u2656"
                    :knight "\u2658"
                    :bishop "\u2657"
                    :queen "\u2655"
                    :king "\u2654"})

(def pawn {:type :pawn
           :moves [{
            :can-capture false
            :transformations (offsets->moves [[-1 0]])
            :get-limit (fn [piece] (if (= (get piece :move-count) 0) 2 1))
           } {
            :can-advance false
            :transformations (offsets->moves [[-1 -1] [-1 1]])
            :get-limit (const 1)
           }]})

(def rook {:type :rook
           :moves [{
            :transformations (offsets->moves [[0 1] [0 -1] [1 0] [-1 0]])
           }]})

(def knight {:type :knight
             :moves [{
                :get-limit (const 1)
                :transformations (offsets->moves [[1 2] [2 1] [1 -2] [2 -1] [-1 -2] [-2 -1] [-1 2] [-2 1]])
             }]})

(def bishop {:type :bishop
             :moves [{
              :transformations (offsets->moves [[1 1] [1 -1] [-1 1] [-1 -1]])
             }]})

(def queen {:type :queen
            :moves [{
                :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
            }]})

(def king {:type :king
           :moves [{
            :get-limit (const 1)
            :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
           }]})

(def pieces-by-type (key-by :type [pawn rook knight bishop queen king]))
