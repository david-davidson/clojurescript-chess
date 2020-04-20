(ns chess.pieces
    (:require [chess.utils :refer [key-by]]))

(defn build-piece [color type]
    (hash-map :color color :type type))

(def b (partial build-piece :black))
(def w (partial build-piece :white))

(defn vacant [] (hash-map :type :empty))
(defn is-vacant? [piece] (= (get piece :type) :empty))

(defn offsets->moves [offsets]
    (defn build-move [[offset-x offset-y]]
        (fn [[x y]]
            [(+ x offset-x) (+ y offset-y)]))
    (map build-move offsets))

(def piece-strings (hash-map :pawn "Pawn"
                             :rook "Rook"
                             :knight "Knight"
                             :bishop "Bishop"
                             :queen "Queen"
                             :king "King"))

(def pawn (hash-map :type :pawn
                    :limit 1
                    :moves (offsets->moves [[1 1] [1 -1] [-1 1] [-1 -1]])))

(def rook (hash-map :type :rook
                    :moves (offsets->moves [[0 1] [0 -1] [1 0] [-1 0]])))

(def knight (hash-map :type :knight
                      :limit 1
                      :moves (offsets->moves [[1 2] [2 1] [1 -2] [2 -1] [-1 -2] [-2 -1] [-1 2] [-2 1]])))

(def bishop (hash-map :type :bishop
                      :moves (offsets->moves [[1 1] [1 -1] [-1 1] [-1 -1]])))

(def queen (hash-map :type :queen
                     :moves (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])))

(def king (hash-map :type :king
                    :limit 1
                    :moves (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])))

(def pieces-by-type (key-by :type [pawn rook knight bishop queen king]))
