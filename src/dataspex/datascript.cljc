(ns dataspex.datascript
  (:require [datascript.core :as d]
            [datascript.conn]
            [datascript.db]
            [datascript.impl.entity]
            #?(:cljs [me.tonsky.persistent-sorted-set :as pss])
            [dataspex.protocols :as dp]
            [dataspex.data :as data]
            [clojure.core.protocols :as p])
  #?(:clj (:import (me.tonsky.persistent_sorted_set PersistentSortedSet))))

(defn get-entities [db]
  (->> (:eavt db)
       (mapv first)
       set
       sort
       (mapv #(d/entity db %))))

(defn summarize-entity [e]
  (let [ks (keys e)]
    (or (when-let [ident (:db/ident e)]
          ident)
        (when-let [uniq (some (:db/unique (:rschema (d/entity-db e))) (keys e))]
          (select-keys e [uniq]))
        (when (< (count ks) 5)
          (into {} e))
        (select-keys e [:db/id]))))

(defn nav-in-db [db p path]
  (data/nav-in
   (case p
     ::entities (get-entities db)
     (get db p))
   path))

(defn get-entity-k [coll e]
  (if (map? coll)
    (val (first (filterv (comp #{(:db/id e)} :db/id key) coll)))
    (first (filterv (comp #{(:db/id e)} :db/id) coll))))

(extend-type datascript.conn.Conn
  dp/INavigatable
  (nav-in [conn [p & path]]
    (nav-in-db (d/db conn) p path)))

(extend-type datascript.db.DB
  dp/INavigatable
  (nav-in [db [p & path]]
    (nav-in-db db p path)))

(defrecord EntityKey [e]
  p/Datafiable
  (datafy [_]
    (summarize-entity e))

  dp/IKeyLookup
  (lookup [_ coll]
    (get-entity-k coll e)))

(extend-type datascript.impl.entity.Entity
  dp/IKey
  (to-key [e]
    (EntityKey. e)))
