# clojurescript-chess
An (extremely WIP) chess engine in Clojurescript: https://clojurescript-chess.surge.sh/

## Getting started
Install dependencies:
```
yarn install
```

Then start the `shadow-cljs` build in one terminal window...
```
yarn watch
```
...and open `localhost:9090` to load the app.

To open a REPL, run `yarn repl` in another terminal window (with `yarn watch` running and the localhost window open). Execute `(ns chess.main)` to load the app's namespace.

### How does it work?
It implements the `minimax` algorithm, which walks the game tree and selects for _minimum_ or _maximum_ scores depending on which player "owns" the tree's current level. `minimax` is limited by performance: the average chess board has ~35 possible moves, which means that searching 3 plies (individual levels) deep means searching `35^3` nodes. Thus, performance gets exponentially worse with depth, and becomes the key constraint on search (and therefore gameplay) ability.

To work around this, we implement a handful of optimizations:
* First, there's alpha/beta pruning, as described [here](https://www.freecodecamp.org/news/simple-chess-ai-step-by-step-1d55a9266977/). This optimization works because `minimax` always selects the optimal move at a given level: as a result, if you encounter a board that implies the current subtree will never be chosen, you can skip searching the rest of the subtree. For example, if the algorithm is checking moves for black and encounters one worth -1, and if it knows the white subtree parent already has a move available worth 1, it knows that 1.) the black subtree will score -1 or lower no matter what (because black selects for low scores) and 2.) the white subtree parent will therefore never select this subtree (because it already has a higher score available).
* Next, there's iterative deepening, as described [here](https://www.gamedev.net/tutorials/_/technical/artificial-intelligence/chess-programming-part-iv-basic-search-r1171/). Alpha/beta pruning is most effective when it checks optimal moves _first_; when it checks them _last_, it offers no advantage over vanilla `minimax`. Iterative deepening attempts to generate optimal move ordering: for a search of `n` plies, it first searches to depth 1, then depth 2, all the way up to `n`. As it goes, it caches the moves available to a given board, sorted best-first. When we revisit that board on the next iteration, we explore the child moves in the order specified by the cache: that order isn't _guaranteed_ to still be optimal at depth `n + 1`, but it's a reasonable guess. Thus, iterative deepening trades repeated work (successive searches) for improved move ordering. It's worth it because, with a branching factor of 35, we spend the vast majority of our search time on the final ply.

### What's next?
I'd like to:
* Keep tuning the algorithm for performance, to support greater search depth
* Consider alternative board representations like [bitboards](https://www.chessprogramming.org/Bitboards)
* Add the notion of checkmate! (For now, kings are treated as regular pieces.) Likewise, we're skipping certain edge-case-y chess rules like castling; at some point they may be worth adding.
