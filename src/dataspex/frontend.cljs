(ns dataspex.frontend
  (:require [dataspex.icons :as icons]
            [dataspex.ui :as ui]
            [replicant.dom :as r]))

::icons/keep
::ui/keep

(defn prefers-dark-mode? []
  (.-matches (js/window.matchMedia "(prefers-color-scheme: dark)")))

(defn set-dispatch! [f]
  (r/set-dispatch!
   (fn [{:replicant/keys [^js dom-event]} actions]
     (.preventDefault dom-event)
     (.stopPropagation dom-event)
     (doseq [action actions]
       (apply prn action))
     (f actions))))

(defn render [hiccup]
  (r/render js/document.body hiccup))

(defn init-element [el]
  (when (prefers-dark-mode?)
    (.add (.-classList el) "dark")))
