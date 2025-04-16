(ns dataspex.client.local
  (:require [dataspex.icons :as icons]
            [dataspex.protocols :as dp]
            [dataspex.ui :as ui]
            [replicant.dom :as r]))

::icons/keep
::ui/keep

(defrecord LocalClient [el]
  dp/IClient
  (set-action-handler [_ handler]
    (r/set-dispatch!
     (fn [_ actions]
       (handler actions))))

  (render [_ hiccup]
    (r/render el hiccup)))

(defn create-client [el]
  (LocalClient. el))
