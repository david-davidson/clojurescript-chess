(ns chess.pieces
    (:require [chess.utils :refer [key-by const]]))

(defn build-piece [color type]
    (hash-map :color color :type type :move-count 0))

(def b (partial build-piece :black))
(def w (partial build-piece :white))

(defn vacant [] (hash-map :type :empty))
(defn is-vacant? [piece] (= (get piece :type) :empty))

(defn offsets->moves [offsets]
    (defn build-move [[offset-x offset-y]]
        (fn [color [x y]]
            (let [x-operator (if (= color :white) + -)]
                [(x-operator x offset-x) (+ y offset-y)])))
    (map build-move offsets))

(def piece-strings (hash-map :pawn "Pawn"
                             :rook "Rook"
                             :knight "Knight"
                             :bishop "Bishop"
                             :queen "Queen"
                             :king "King"))

(def pawn (hash-map :type :pawn
                    :moves [{
                        :can-capture false
                        :transformations (offsets->moves [[-1 0]])
                        :get-limit (fn [piece] (if (= (get piece :move-count) 0) 2 1))
                    } {
                        :can-advance false
                        :transformations (offsets->moves [[-1 -1] [-1 1]])
                        :get-limit (const 1)
                    }]))

(def rook (hash-map :type :rook
                    :moves [{
                        :transformations (offsets->moves [[0 1] [0 -1] [1 0] [-1 0]])
                    }]))

(def knight (hash-map :type :knight
                      :moves [{
                        :get-limit (const 1)
                        :transformations (offsets->moves [[1 2] [2 1] [1 -2] [2 -1] [-1 -2] [-2 -1] [-1 2] [-2 1]])
                      }]))

(def bishop (hash-map :type :bishop
                      :moves [{
                        :transformations (offsets->moves [[1 1] [1 -1] [-1 1] [-1 -1]])
                      }]))

(def queen (hash-map :type :queen
                     :moves [{
                        :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
                     }]))

(def king (hash-map :type :king
                    :moves [{
                        :get-limit (const 1)
                        :transformations (offsets->moves [[0 1] [1 1] [1 0] [1 -1] [0 -1] [-1 -1] [-1 0] [-1 1]])
                    }]))

(def pieces-by-type (key-by :type [pawn rook knight bishop queen king]))
