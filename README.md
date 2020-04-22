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

### What can it do so far?
There's not yet any notion of programmatic gameplay (you vs. the computer). But you can move pieces around the board at https://clojurescript-chess.surge.sh/!
