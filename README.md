# clojurescript-chess
An (extremely WIP) chess engine in Clojurescript.

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

### Progress
So far, all we can do is get the moves available from a given rank-and-file position. In the REPL:
```cljs
> (ns chess.main)
> (get-moves-repl "b8") ; Knight, white: b8 corresponds to [0, 1] in board matrix
([2 2] [2 0]) ; Available moves
```
