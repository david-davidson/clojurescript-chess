(ns chess.test.gameplay
    (:require [cljs.test :refer-macros [deftest is testing]]
              [chess.gameplay :refer [get-next-move select-best-child]]
			  [chess.pieces :as pieces :refer [b
                                               w
                                               vacant]]))

(testing "gameplay"
	(deftest finds-check-mate
		(def initial-board [
			[(b "rook") (b "knight") (b "bishop") (b "queen") (b "king") (b "bishop") (b "knight") (b "rook")]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
			[(vacant) (vacant) (vacant) (vacant) (w "king") (vacant) (vacant) (vacant)]
		])
		(def best-move (get-next-move initial-board "black" nil 5 nil nil select-best-child))

		(is (= (best-move :score) -99999))
		(is (= (best-move :move) [[0 0] [6 0]]))))
