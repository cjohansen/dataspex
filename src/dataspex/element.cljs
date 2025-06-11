(ns dataspex.element
  (:require [clojure.string :as str]
            [dataspex.ui :as-alias ui]))

(defn ->hiccup [^js el]
  (let [attrs (->> (.-attributes el)
                   (remove (comp #{"id" "class"} #(.-name ^js %)))
                   (mapv (fn [attr]
                           [(keyword (.-name attr)) (.-value attr)]))
                   (into {}))
        children (if (< 100 (count (.-outerHTML el)))
                   [(str (let [s (.-innerText el)]
                           (if (< 100 (count s))
                             (let [short-s (first (str/split s #"\n"))]
                               (if (< 100 (count short-s))
                                 (str/join (take 80 short-s))
                                 short-s))
                             s))
                         " ...")]
                   (->> (.-childNodes el)
                        (filterv (fn [^js node]
                                   (or (= js/Node.TEXT_NODE (.-nodeType node))
                                       (= js/Node.ELEMENT_NODE (.-nodeType node)))))
                        (mapv (fn [^js node]
                                (if (= js/Node.ELEMENT_NODE (.-nodeType node))
                                  (->hiccup node)
                                  (.-nodeValue node))))))]
    (cond-> [(keyword
              (str (str/lower-case (.-tagName el))
                   (when-let [id (not-empty (.-id el))]
                     (str "#" id))
                   (when-let [classes (seq (into [] (.-classList el)))]
                     (str "." (str/join "." classes)))))]
      (seq attrs) (conj attrs)
      (seq children) (into children))))
