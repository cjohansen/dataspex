(ns dataspex.datascript
  (:require #?(:cljs [me.tonsky.persistent-sorted-set :as pss])
            [clojure.core.protocols :as p]
            [datascript.conn]
            [datascript.core :as d]
            [datascript.db]
            [datascript.impl.entity]
            [dataspex.data :as data]
            [dataspex.hiccup :as hiccup]
            [dataspex.protocols :as dp]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views])
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

(defn summarize-contents [db]
  (str (count (set (mapv first (:eavt db))))
       " entities, "
       (count (:eavt db))
       " datoms"))

(defn render-conn-inline [conn]
  [::ui/code (str "#datascript/Conn [" (summarize-contents (d/db conn)) "]")])

(defn render-database-inline [db]
  [::ui/code (str "#datascript/DB [" (summarize-contents db) "]")])

(defn navigate-to [opt xs]
  (->> (views/path-to opt xs)
       (views/navigate-to opt)))

(defn render-datom [[e a v t add?] opt & [{:keys [alias]}]]
  [(or alias ::ui/inline-tuple) {::ui/prefix "datom"}
   [::ui/number {::ui/actions [(navigate-to opt [e])]} e]
   [::ui/keyword {::ui/actions [(navigate-to opt [a])]} a]
   (-> (hiccup/render-inline v)
       (hiccup/add-attr ::ui/actions [(navigate-to opt [e a])]))
   [::ui/number {::ui/actions [(navigate-to opt [t])]} t]
   [::ui/boolean add?]])

(extend-type datascript.conn.Conn
  dp/INavigatable
  (nav-in [conn [p & path]]
    (nav-in-db (d/db conn) p path))

  dp/IRenderInline
  (render-inline [conn _]
    (render-conn-inline conn)))

(extend-type datascript.db.DB
  dp/INavigatable
  (nav-in [db [p & path]]
    (nav-in-db db p path))

  dp/IRenderInline
  (render-inline [db _]
    (render-database-inline db)))

(extend-type datascript.db.Datom
  dp/IRenderInline
  (render-inline [datom opt]
    (render-datom datom opt)))

(extend-type #?(:clj PersistentSortedSet
                :cljs pss/BTSet)
  dp/IRenderInline
  (render-inline [pss opt]
    (hiccup/render-inline-set pss opt)))

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
    (EntityKey. e))

  dp/IRenderInline
  (render-inline [entity opt]
    (hiccup/render-inline (summarize-entity entity) opt)))
