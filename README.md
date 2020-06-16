# clojurescript-chess
A (WIP) chess engine, implemented as a project to learn Clojurescript: https://clojurescript-chess.surge.sh/

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
It implements the `minimax` algorithm, which walks the game tree and selects for _minimum_ or _maximum_ scores depending on which player "owns" the tree's current level. (White optimizes for highest score, black for lowest.) `minimax` is limited by performance: the average chess board has ~35 possible moves, which means that searching 5 plies (individual levels) deep means navigating `35^5` possible game states (around 52,000,000!). Thus, performance gets exponentially worse with depth and becomes the key constraint.

To work around this, we implement a handful of optimizations:
* First, there's alpha/beta pruning, as described [here](https://www.freecodecamp.org/news/simple-chess-ai-step-by-step-1d55a9266977/). This optimization works because `minimax` always selects the optimal move at a given level: as a result, if you encounter a board whose score implies the current subtree will never be chosen, you can skip searching the rest of the subtree. For example, if the algorithm is checking moves for black and encounters one worth -1, and if it knows the white subtree parent already has a move available worth 1, it knows that 1.) the current black subtree will score -1 or lower no matter what (because black selects for low scores) and 2.) the white subtree parent **will therefore never select this subtree** (because it already has a higher score available). That means we can bail early.
* Next, there's iterative deepening, as described [here](https://www.gamedev.net/tutorials/_/technical/artificial-intelligence/chess-programming-part-iv-basic-search-r1171/). Alpha/beta pruning is most effective when it checks optimal moves _first_; when it checks them _last_, it offers no advantage over vanilla `minimax`. Iterative deepening attempts to generate optimal move ordering: for a search of `n` plies, it first searches to depth 1, then depth 2, all the way up to `n`. As it goes, it caches the moves available to a given board, sorted best-first. When we revisit that board on the next iteration, we explore the child moves in the order specified by the cache: that order isn't _guaranteed_ to still be optimal at depth `n + 1`, but it's a reasonable guess. Thus, iterative deepening **trades repeated work (successive searches) for improved move ordering**. It's worth it because, with a branching factor of 35, we spend the vast majority of our search time on the final ply.
* In addition, we optimize the validation of moves that result in check. These moves are illegal, but they're expensive to detect, because that requires 1.) making the move in question, 2.) inspecting _all resulting moves_ available to the other team, and 3.) looking for a move that captures the king. So, rather than validating available moves before traversing them, **we validate as we go**. Moves that result in the capture of a king flag the parent move as illegal (and hide it from search), because the parent failed to get out of check.
* We split work across several web workers, in parallel. This is an interesting problem: alpha/beta pruning is inherently sequential, in that it relies on knowing the best moves visited _so far_. As noted [here](https://students.cs.byu.edu/~snell/Classes/CS584/projectsF99/steele/report.html), the parallel version can easily underperform the serial one! Our main departure from straightforward parallelization is to have **workers request small units of work when ready**, creating a load-balancing effect. (Concretely, this looks like worker 1 searching move A, worker 2 searching move B, worker 1 finishing A and starting C, and worker 2 finishing B and searching D—as opposed to assigning `[A, B]` and `[C, D]` to the two workers as equal partitions.) This way, if a worker hits an especially slow-to-search move, the other workers can keep searching fast while the slow worker finishes.

Most of this logic lives in [gameplay.cljs](https://github.com/david-davidson/clojurescript-chess/blob/master/src/main/chess/gameplay.cljs), and helpers for parallel search live in [worker_utils.cljs](https://github.com/david-davidson/clojurescript-chess/blob/master/src/main/chess/worker_utils.cljs).

### What's next?
I'd like to:
* Keep tuning the algorithm for performance, to support greater search depth. (In particular, I'd like to find more optimized ways to reconcile parallelization and alpha/beta pruning.)
* Consider alternative board representations like [bitboards](https://www.chessprogramming.org/Bitboards).
* Add some of the more obscure chess rules: we're skipping certain edge-case-y rules like castling, but at some point they may be worth adding!
