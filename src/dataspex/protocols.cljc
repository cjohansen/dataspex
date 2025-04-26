(ns dataspex.protocols)

(defprotocol INavigatable
  :extend-via-metadata true
  (nav-in [self ks]))

(defprotocol IKey
  :extend-via-metadata true
  (to-key [self]))

(defprotocol IKeyLookup
  :extend-via-metadata true
  (lookup [self x]))

(defprotocol IRenderInline
  :extend-via-metadata true
  (render-inline [self opts]))

(defprotocol IRenderDictionary
  :extend-via-metadata true
  (render-dictionary [self opts]))

(defprotocol IRenderTable
  :extend-via-metadata true
  (tableable? [self opts]
    "Returns true if `self` can render as a table.")
  (render-table [self opts]))

(defprotocol IRenderSource
  :extend-via-metadata true
  (render-source [self opts]))

(defprotocol IRenderHiccup
  :extend-via-metadata true
  (render-hiccup [self opts]))

(defprotocol IDiffable
  (->diffable [self]))

(defprotocol IAuditable
  (get-audit-summary [self])
  (get-audit-details [self]))

(defprotocol IRenderDiffSummary
  (render-diff-summary [self diff]))

(defprotocol IRenderDiff
  (render-diff [self diff]))

(defprotocol Watchable
  (get-val [watchable])
  (watch [watchable k f])
  (unwatch [watchable watcher]))
