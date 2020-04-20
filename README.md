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
So far, the only way to move is via the REPL:
```cljs
> (ns chess.main)
> (move-piece-repl "a2" "b3") ; Move piece from a2 to b3
```
There's no validation at all on the `from` and `to` fields.

Hovering a piece highlights in green the places it's _allowed_ to move (though again, the REPL doesn't respect these constraints). There's plenty of work left around getting the "places I'm allowed to move to" logic dialed in for each piece type.
