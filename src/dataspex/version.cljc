(ns dataspex.version
  (:require [dataspex.ui :as-alias ui]))

(def breaking-version 2)
(def version "2025.06.6")

(defn render-outdated-extension-error [host]
  [::ui/alert.m-2 {:data-color "error"}
   [:h2.h2 "Your Dataspex extension is out of date"]
   [:p (:host-str host)
    " is using a newer version of the Dataspex wire protocol than your installed
    browser extension. Inspecting data from this process may or may not work. It
    is recommended that you update the browser extension to version " (:version
    host) " or newer."]])

(defn render-outdated-library-error [host]
  [::ui/alert.m-2 {:data-color "error"}
   [:h2.h2 "The app's Dataspex library is out of date"]
   [:p (:host-str host)
    " is using an older version of the Dataspex wire protocol than your
    installed browser extension. Inspecting data from this process may or may
    not work. It is recommended that you update the library to version "
    version " or newer."]])

(defn check-version [data]
  (cond
    (< breaking-version (:breaking-version data))
    (render-outdated-extension-error data)

    (< (:breaking-version data) breaking-version)
    (render-outdated-library-error data)))
