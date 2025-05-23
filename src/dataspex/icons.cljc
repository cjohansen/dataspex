(ns dataspex.icons
  "Since Dataspex will prepare its hiccup in one process and render it in another,
  it can't use `icons/icon` to load an icon into the build as it's preparing the
  hiccup. Instead, this namespace reifies all icons used by Dataspex, so they
  are included in the rendering build."
  (:require [phosphor.icons :as icons]
            [replicant.alias :refer [defalias]]))

(defn render-icon [attrs icon]
  (icons/render icon (update attrs :class conj :icon)))

(defalias copy [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/copy)))

(defalias sort-ascending [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/sort-ascending)))

(defalias sort-descending [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/sort-descending)))

(defalias browser [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/browser)))

(defalias table [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/table)))

(defalias brackets-curly [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/brackets-curly)))

(defalias brackets-round [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/brackets-round)))

(defalias brackets-square [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/brackets-square)))

(defalias arrows-in-simple [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/arrows-in-simple)))

(defalias arrows-out-simple [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/arrows-out-simple)))

(defalias arrow-counter-clockwise [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/arrow-counter-clockwise)))

(defalias x [attrs _]
  (render-icon attrs (icons/icon :phosphor.bold/x)))

(defalias plus-circle [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/plus-circle)))

(defalias caret-left [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/caret-left)))

(defalias caret-right [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/caret-right)))

(defalias sun [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/sun)))

(defalias moon [attrs _]
  (render-icon attrs (icons/icon :phosphor.regular/moon)))

(defalias wifi-high [attrs _]
  (render-icon (assoc-in attrs [:style :fill] "currentColor") (icons/icon :phosphor.regular/wifi-high)))

(defalias wifi-x [attrs _]
  (render-icon (assoc-in attrs [:style :fill] "currentColor") (icons/icon :phosphor.regular/wifi-x)))
