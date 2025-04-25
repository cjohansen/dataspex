(ns dataspex.dev
  (:require [dataspex.core :as dataspex]))

(comment

  (dataspex/inspect "Map"
    {:hello "World!"
     :runtime "JVM, baby!"
     :numbers (range 1500)})

  (dataspex/inspect "Map"
    {:greeting {:hello "World"}
     :runtime "JVM, baby!"
     :numbers (range 1500)})

  )
