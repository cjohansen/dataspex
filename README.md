# Dataspex

**See the shape of your data.** Dataspex is a point-and-click Clojure(Script)
data browser that lets you peer into maps, vectors, and seqs; explore Datascript
and Datomic connections; trace changes to atoms and databases over time; and
more -- with minimal effort and a short learning curve.

```clj
no.cjohansen/dataspex {:mvn/version "2025.05.7"}
```

Dataspex runs as a Chrome and Firefox extension — or as a standalone web app —
and connects to local ClojureScript apps, Clojure REPLs, or across the network
so you can debug apps on your phone.

Whether you're debugging a live system or scrying the hidden structure of your
app state, Dataspex gives you eyes on the flow.

(Watch the grand tour of Dataspex in 10 minutes)[https://youtu.be/5AKvD3nGCYY].

Dataspex is the spiritual successor to [Gadget
Inspector](https://github.com/cjohansen/gadget-inspector).

## Status

Dataspex is currently "hardening". It works for a wide variety of use cases, but
there are some sharp edges. The browser extensions are not currently in the
official extension stores because the contract between the library and the
extensions may still change. At this point you should build the extensions (see
below) for the corresponding library version you are using.

I expect Dataspex to be stable and with officially released browser extensions
by the end of June 2025.

## Usage

Using Dataspex is easy: require `dataspex.core` and call `inspect` on your data.
When used from Clojure, it will start a server on port 7117. The browser
extension automatically connects to this port.

```clj
(require '[dataspex.core :as dataspex])

;; Atoms will automatically be watched for changes, including a live updated
;; changelog with diffs
(def store (atom {}))
(dataspex/inspect "App state" store)

;; Inspect any piece of data with a label. Multiple calls to `inspect` with the
;; same label will update Dataspex' view of the data, and produce an audit
;; trail.
(def page-data {:title "My page"})
(dataspex/inspect "Page data" page-data)

;; Datascript databases can be viewed through a custom UI
(require '[datascript.core :as d])
(def conn
  (d/create-conn
   {:person/id {:db/unique :db.unique/identity}
    :person/friends {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many}}))

,,,

(dataspex/inspect "DB" conn)

;; Inspect taps
(dataspex/inspect-taps)
```

## Features

Display data from your ClojureScript app and/or JVM backend live in your
browser's devtools panel, and enjoy a bunch of finely tuned point-and-click
features, including:

- Browse immutable data
- Browse JavaScript objects, arrays and dates
- Browse JWT strings
- Browse any data that implements `clojure.core.protocols/Datafiable`
- Browse Datascript databases, indexes and entities
- Browse Datomic databases and entities (indexes to follow)
- Navigate Datascript/Datomic refs, including reverse refs
- Browse sequences of maps in tables
- Browse meta data of data
- View full data in "syntax highlight mode"
- Browse hiccup with customized rendering
- Copy data at any point in the datastructure
- Paginate large or indefinite data structures
- Consistently sort collections (including respecting indexed and sorted ones)
- View changes to inspected data over time, including diffs
- Browse any historic version of inspected data, side-by-side with other
  versions
- Light and dark themes
- Build custom views for your own data types by implementing the various
  rendering protocols.

And much more.

## Chrome extension

To build the Chrome extension:

```sh
make chrome-extension
```

Then go to [chrome://extensions/](chrome://extensions/), enable devtools, and
click "Load unpacked". Choose the `brower-extension` directory in this repo, and
you should be good to go.

## Firefox extension

To build the Firefox extension:

```sh
make firefox-extension
```

You will need [Firefox Developer Edition](https://www.mozilla.org/en-US/firefox/developer/).

1. Go to [about:config](about:config)
2. Set `devtools.chrome.enabled` to `true`
3. Set `devtools.debugger.remote-enabled` to `true`
4. Set `devtools.debugger.prompt-connection` to `false` (optional, reduces prompts)
5. Set `extensions.webextensions.remote` to `true` (required for devtools
   extensions)
6. Go to [about:debugging](about:debugging)
7. Click "This Firefox" in the sidebar
8. Click "Load Temporary Add-on"
9. Select manifest.json from the browser-extension/firefox folder

If you make changes to the extension code, click "reload".

## Developing Dataspex

Dataspex' primary use case is as an extension to the browser's devtools. But
this isn't the only way to run Dataspex, and it certainly isn't the most
ergonomical way to develop it. Luckily, you can run Dataspex as any old webapp.

To run the development version of Dataspex:

```sh
npx shadow-cljs watch app portfolio
```

You can now find Portfolio (all the UI elements) on
[http://localhost:9090/portfolio.html](http://localhost:9090/portfolio.html).
The entry point is
[portfolio/dataspex/scenes.cljs](portfolio/dataspex/scenes.cljs).

...and a sample Dataspex instance on
[http://localhost:9090](http://localhost:9090). Its entry point is
[dev/dataspex/dev.cljs](dev/dataspex/dev.cljs).

Finally, there's a comprehensible test suite that can be run with:

```sh
bin/kaocha --watch
```

A guide to the code and some more docs are pending. In the meantime, here's the
bird's eye overview.

`dataspex.core` defines the public API. All other namespaces are internal until
publicly documented.

### Core namespaces

- `dataspex.data`: Datafy and navigate data structures
- `dataspex.hiccup`: Rendering logic for data structures, most important
  entry-points are `render-inline`, `render-dictionary`, `render-source`, and
  `render-table`
- `dataspex.audit_log`: Rendering logic for the audit log
- `dataspex.protocols`: The protocols that power Dataspex' rendering
- `dataspex.views`: Shared view options and utilities
- `dataspex.inspector`: Inspecting values: subscribing to changes, rendering,
  recording history/audit trail, etc.
- `dataspex.tap_inspector`: Inspecting taps

### The UI library:

- `dataspex.ui`: UI elements, the building blocks.
- `dataspex.icons`: The Phosphor icons used (see namespace docstring).

### The Dataspex UI

- `dataspex.panel`: The Dataspex
- `dataspex.actions`: UI interaction dispatch

### Various utilities

- `dataspex.codec`: To and from strings for out of process communication
- `dataspex.diff`: Dataspex' use of Editscript
- `dataspex.time`: Very limited date-time stuff
- `dataspex.user_agent`: User agent parser


### Type-specifix extensions:

- `dataspex.datalog`: Shared logic for Datomic/Datascript
- `dataspex.datascript`
- `dataspex.datomic`
- `dataspex.date`: JavaScript Dates
- `dataspex.jwt`: JSON Web Tokens

## Infrastructure/wiring

- `dataspex.server`: The Dataspex server, used when inspecting data on the JVM
- `dataspex.browser_extension`: Entry-point for the browser extension
- `dataspex.remote_inspector`: Entry-point for the remote inspector (e.g. http://localhost:7117)
- `dataspex.browser_extension_client`: Client that receives render calls and
  sends actions through the browser extension messaging system
- `dataspex.in_process_client`: Client that subscribes to render calls and sends
  actions in-process: basically the engine of the development version of
  Dataspex
- `dataspex.server_client`: Client that subscribes to render calls and sends
  actions over the HTTP server: how Dataspex renders data inspected on the JVM
- `dataspex.in_process_host`: Host that sends renders and subscribes to actions in-process
- `dataspex.remote_host`: Host that sends renders and subscribes actions over HTTP
- `dataspex.render_client`: Client wiring
- `dataspex.render_host`: Host wiring

## License

Copyright © 2025 Christian Johansen. Distributed under the [MIT
License](https://opensource.org/license/mit).
