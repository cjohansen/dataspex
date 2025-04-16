(ns dataspex.protocols)

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
