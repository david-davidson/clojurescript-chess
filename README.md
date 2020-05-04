# clojurescript-chess
An (extremely WIP) chess engine in Clojurescript: https://clojurescript-chess.surge.sh/

## Getting started
Install dependencies:
```
npm install
```

Then start the `shadow-cljs` build in one terminal window...
```
shadow-cljs watch app
```
...and open `localhost:9090` to load the app.

To open a REPL, run `shadow-cljs cljs-repl app` in another terminal window (with `shadow-cljs watch app` running and the localhost window open). Execute `(ns chess.main)` to load the app's namespace.

### What can it do so far?
It implements the `minimax` algorithm, which walks the game tree and selects for _minimum_ or _maximum_ scores depending on which player "owns" the tree's current level. The search tree is limited to depth 3 (by performance concerns): so, look at all possible moves for black, then all possible moves (in response) for white, then all possible moves (in response) for black.

### What's next?
I'd like to:
* Keep tuning the algorithm for performance, to support greater search depth
* Consider dynamically increasing search depth in endgame scenarios, where there are fewer pieces on the board (and thus a smaller game tree to explore)
* Add the notion of checkmate! (For now, kings are treated as regular pieces)
