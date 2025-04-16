(ns dataspex.hiccup-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.helper :as h]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as-alias icons]
            [dataspex.ui :as-alias ui]
            [dataspex.views :as views]
            [lookup.core :as lookup]))

(deftest render-inline-test
  (testing "Renders string"
    (is (= (h/render-inline "String")
           [::ui/string "String"])))

  (testing "Renders keyword"
    (is (= (h/render-inline :keyword)
           [::ui/keyword :keyword])))

  (testing "Renders number"
    (is (= (h/render-inline 42)
           [::ui/number 42])))

  (testing "Renders boolean"
    (is (= (h/render-inline true)
           [::ui/boolean true])))

  (testing "Renders symbol"
    (is (= (h/render-inline 'hello)
           [::ui/symbol 'hello])))

  (testing "Renders nil"
    (is (= (h/render-inline nil)
           [::ui/code "nil"])))

  (testing "Renders UUID"
    (is (= (h/render-inline #uuid "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906")
           [::ui/literal {::ui/prefix "#uuid"}
            [::ui/string "ebb5b6b2-c3c0-4b44-b8c4-bcdfa4a3c906"]])))

  (testing "Renders insts"
    (is (= (h/render-inline #inst "2025-04-05")
           [::ui/literal {::ui/prefix "#inst"}
            [::ui/string "2025-04-05T00:00:00.000-00:00"]])))

  (testing "Renders short vector"
    (is (= (h/render-inline [:hello])
           [::ui/vector
            [::ui/keyword :hello]])))

  (testing "Renders longer vector"
    (is (= (h/render-inline
            [:hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello])
           [::ui/link "[19 keywords]"])))

  (testing "Renders longer vector of mixed contents"
    (is (= (h/render-inline
            [:hello :hello :hello :hello :hello
             :hello :hello :hello :hello 2
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello])
           [::ui/link "[19 items]"])))

  (testing "Renders short list"
    (is (= (h/render-inline '(:hello))
           [::ui/list
            [::ui/keyword :hello]])))

  (testing "Renders longer list"
    (is (= (h/render-inline
            (list
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello))
           [::ui/link "(19 keywords)"])))

  (testing "Renders longer list of mixed contents"
    (is (= (h/render-inline
            (list
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello 2
             :hello :hello :hello :hello :hello
             :hello :hello :hello :hello))
           [::ui/link "(19 items)"])))

  (testing "Renders short seq"
    (is (= (h/render-inline (range 0 5))
           [::ui/list
            [::ui/number 0]
            [::ui/number 1]
            [::ui/number 2]
            [::ui/number 3]
            [::ui/number 4]])))

  (testing "Renders longer seq"
    (is (= (h/render-inline (range 0 50))
           [::ui/link "(50 numbers)"])))

  (testing "Renders longer seq of mixed contents"
    (is (= (h/render-inline (concat (range 0 50) (repeat 3 :lol)))
           [::ui/link "(53 items)"])))

  (testing "Renders indefinite seq"
    (is (= (h/render-inline (range))
           [::ui/link "(1000+ items)"])))

  (testing "Renders short set"
    (is (= (h/render-inline #{:hello})
           [::ui/set
            [::ui/keyword :hello]])))

  (testing "Renders longer set"
    (is (= (h/render-inline
            #{:hello :hallo :hillo :hollo :hullo
              :hella :halla :hilla :holla :hulla
              :helle :halle :hille :holle :hulle
              :hellu :hallu :hillu :hollu})
           [::ui/link "#{19 keywords}"])))

  (testing "Renders longer set of mixed contents"
    (is (= (h/render-inline
            (set (conj (range 0 50) :lol)))
           [::ui/link "#{51 items}"])))

  #?(:cljs
     (testing "Renders short JS array"
       (is (= (h/render-inline #js ["hello"])
              [::ui/vector
               {:dataspex.ui/prefix "#js"}
               [::ui/string "hello"]]))))

  (testing "Renders short map"
    (is (= (-> {:hello "There", :lol "lul"}
               h/render-inline)
           [::ui/map
            [::ui/map-entry
             [::ui/keyword :hello]
             [::ui/string "There"]]
            [::ui/map-entry
             [::ui/keyword :lol]
             [::ui/string "lul"]]])))

  (testing "Sorts map keys"
    (is (= (->> {:c 1, :dataspex/hello "There", :b 2, "a" 3, 'dataspex/sym "Sym"}
                h/render-inline
                (lookup/select '[::ui/map-entry ":first-child"])
                (mapv second))
           [:dataspex/hello :b :c 'dataspex/sym "a"])))

  (testing "Summarizes just keys of longer map"
    (is (= (->> {:title "Everything Everywhere All At Once"
                 :year 2022
                 :directors ["Daniel Kwan" "Daniel Scheinert"]
                 :genres ["Action" "Comedy" "Drama" "Sci-Fi"]
                 :rating 8.1
                 :runtime-minutes 139
                 :language "English"
                 :awards {:oscars 7 :nominations 11}}
                h/render-inline
                (lookup/select '[::ui/map-entry])
                (mapv (comp second second)))
           [:awards :directors :genres :language :rating
            :runtime-minutes :title :year])))

  (testing "Summarizes when map is too large"
    (is (= (->> {:title "Everything Everywhere All At Once"
                 :year 2022
                 :directors ["Daniel Kwan" "Daniel Scheinert"]
                 :genres ["Action" "Comedy" "Drama" "Sci-Fi"]
                 :rating 8.1
                 :runtime-minutes 139
                 :language "English"
                 :awards {:oscars 7 :nominations 11}}
                (h/render-inline {:dataspex/summarize-above-w 50}))
           [::ui/link "{8 keys}"])))

  #?(:cljs
     (testing "Renders short JS object"
       (is (= (-> #js {:hello "There"}
                  h/render-inline)
              [::ui/map {:dataspex.ui/prefix "#js"}
               [::ui/map-entry
                [::ui/keyword :hello]
                [::ui/string "There"]]]))))

  (testing "Renders inline atom"
    (is (= (h/render-inline (atom {}))
           [::ui/vector {::ui/prefix "#atom"}
            [::ui/map]]))))

(deftest render-dictionary-test
  (testing "Renders string"
    (is (= (h/render-dictionary
            {:dataspex/inspectee "Store"
             :dataspex/path [:val]} "A string")
           [::ui/dictionary
            [::ui/entry
             [::ui/symbol "Type"]
             [::ui/symbol "String"]]
            [::ui/entry
             [::ui/symbol "Value"]
             [::ui/string "A string"]
             [::ui/button
              {::ui/title "Copy to clipboard"
               ::ui/actions [[::actions/copy "Store" [:val]]]}
              [::icons/copy]]]])))

  (testing "Renders keyword"
    (is (= (->> :keyword
                (h/render-dictionary
                 {:dataspex/inspectee "Conn"
                  :dataspex/path [:k]})
                (lookup/select '[::ui/entry ::ui/keyword])
                lookup/text)
           ":keyword")))

  (testing "Renders number"
    (is (= (->> 42
                (h/render-dictionary
                 {:dataspex/inspectee "Store"
                  :dataspex/path [:num]})
                (lookup/select '[::ui/entry ::ui/number])
                lookup/text)
           "42")))

  (testing "Renders boolean"
    (is (= (->> (h/render-dictionary {:dataspex/path [:bool]} true)
                (lookup/select '[::ui/entry ::ui/boolean])
                lookup/text)
           "true")))

  (testing "Renders symbol"
    (is (= (->> (h/render-dictionary {:dataspex/path [:sym]} 'namespaced/symbol)
                (lookup/select '[::ui/entry ::ui/symbol])
                last
                lookup/text)
           "namespaced/symbol")))

  (testing "Renders vector"
    (is (= (h/render-dictionary
            {:dataspex/path [:libs]
             :dataspex/inspectee "Clojars"}
            [{:library/name "Replicant"}
             {:library/name "Dataspex"}])
           [::ui/dictionary
            [::ui/entry
             {::ui/actions [[::actions/assoc-in ["Clojars" :dataspex/path] [:libs 0]]]}
             [::ui/number 0]
             [::ui/map
              [::ui/map-entry
               [::ui/keyword :library/name]
               [::ui/string "Replicant"]]]
             [::ui/button
              {::ui/title "Copy to clipboard"
               ::ui/actions [[::actions/copy "Clojars" [:libs 0]]]}
              [::icons/copy]]]
            [::ui/entry
             {::ui/actions [[::actions/assoc-in ["Clojars" :dataspex/path] [:libs 1]]]}
             [::ui/number 1]
             [::ui/map
              [::ui/map-entry
               [::ui/keyword :library/name]
               [::ui/string "Dataspex"]]]
             [::ui/button
              {::ui/title "Copy to clipboard"
               ::ui/actions [[::actions/copy "Clojars" [:libs 1]]]}
              [::icons/copy]]]])))

  (testing "Displays vector meta data"
    (is (= (->> (with-meta
                  [{:library/name "Replicant"}
                   {:library/name "Dataspex"}] {:secret "Additional data"})
                (h/render-dictionary {:dataspex/path [:libs]})
                lookup/children
                first
                lookup/text)
           "^meta :secret Additional data")))

  (testing "Navigates with current path"
    (is (= (->> [{:library/name "Replicant"}
                 {:library/name "Dataspex"}]
                (h/render-dictionary
                 {:dataspex/inspectee "Libs"
                  :dataspex/path [:libraries]})
                (lookup/select ::ui/entry)
                (mapv lookup/attrs))
           [{::ui/actions [[::actions/assoc-in ["Libs" :dataspex/path] [:libraries 0]]]}
            {::ui/actions [[::actions/assoc-in ["Libs" :dataspex/path] [:libraries 1]]]}])))

  (testing "Paginates vector"
    (is (= (->> ["Replicant" "Portfolio" "Dataspex" "m1p" "lookup" "phosphor-clj"]
                (mapv (fn [lib] {:library/name lib}))
                (h/render-dictionary
                 {:dataspex/path [:libs]
                  :dataspex/pagination {[:libs] {:offset 2}
                                       :page-size 3}})
                (lookup/select ::ui/number)
                (mapv lookup/text))
           ["2" "3" "4"])))

  (testing "Browses list as dictionary"
    (is (= (->> (h/render-dictionary
                 (list {:library/name "Replicant"}
                       {:library/name "Dataspex"}))
                (lookup/select ::ui/number)
                (mapv lookup/text))
           ["0" "1"])))

  (testing "Browses seq as dictionary"
    (is (= (->> ["Replicant" "Dataspex"]
                (map (fn [lib] {:library/name lib}))
                h/render-dictionary
                (lookup/select ::ui/number)
                (mapv lookup/text))
           ["0" "1"])))

  (testing "Browses set as dictionary"
    (is (= (->> #{"Replicant" "Dataspex"}
                (h/render-dictionary
                 {:dataspex/inspectee "Datas"
                  :dataspex/path [:lib-names]})
                (lookup/select ::ui/entry)
                first)
           [::ui/entry
            {::ui/actions [[::actions/assoc-in ["Datas" :dataspex/path] [:lib-names "Dataspex"]]]}
            ""
            [::ui/string "Dataspex"]
            [::ui/button
             {::ui/title "Copy to clipboard"
              ::ui/actions [[::actions/copy "Datas" [:lib-names "Dataspex"]]]}
             [::icons/copy]]])))

  (testing "Browses map as dictionary"
    (is (= (->> (h/render-dictionary
                 {:library/name "Replicant"
                  :library/version "2025.03.27"})
                (lookup/select ::ui/keyword)
                (mapv lookup/text))
           [":library/name" ":library/version"])))

  (testing "Renders atom dictionary"
    (is (= (->> (atom {:name "Dataspex"})
                h/render-dictionary
                (lookup/select ::ui/keyword)
                (mapv lookup/text))
           [":name"])))

  #?(:cljs
     (testing "Renders date as dictionry"
       (is (= (->> (js/Date. 2025 3 16 11 20 0)
                   h/render-dictionary
                   (lookup/select ::ui/keyword)
                   (mapv lookup/text))
              [":iso"
               ":locale-date-string"
               ":year"
               ":month"
               ":date"
               ":time"
               ":timezone"
               ":timestamp"]))))

  #?(:cljs
     (testing "Renders JS object as dictionary"
       (is (= (->> #js {:name "Christian"}
                   h/render-dictionary
                   (lookup/select ::ui/keyword)
                   (mapv lookup/text))
              [":name"]))))

  #?(:cljs
     (testing "Renders JS array as dictionary"
       (is (= (->> #js ["Christian"]
                   h/render-dictionary
                   (lookup/select ::ui/number)
                   (mapv lookup/text))
              ["0"])))))

(deftest render-table-test
  (testing "Renders collection of maps as table"
    (is (= (->> (h/render-table
                 [{:point/label "Bulbasaur"
                   :point/latitude 37.807962
                   :point/longitude -122.475238}
                  {:point/label "Charmander"
                   :point/latitude 34.062759
                   :point/longitude -118.35718}
                  {:point/label "Squirtle"
                   :point/latitude 37.805929
                   :point/longitude -122.429582}
                  {:point/label "Magnemite"
                   :point/latitude 37.8269775
                   :point/longitude -122.425144}
                  {:point/label "Magmar"
                   :point/latitude 37.571414
                   :point/longitude -122.00004}])
                (lookup/select '::ui/th)
                (mapv (comp lookup/text first lookup/children)))
           ["" ":point/label" ":point/latitude" ":point/longitude"])))

  (testing "Paginates huge map collection table"
    (is (= (->> [{:point/label "Bulbasaur"
                  :point/latitude 37.807962
                  :point/longitude -122.475238}
                 {:point/label "Charmander"
                  :point/latitude 34.062759
                  :point/longitude -118.35718}
                 {:point/label "Squirtle"
                  :point/latitude 37.805929
                  :point/longitude -122.429582}
                 {:point/label "Magnemite"
                  :point/latitude 37.8269775
                  :point/longitude -122.425144}
                 {:point/label "Magmar"
                  :point/latitude 37.571414
                  :point/longitude -122.00004}]
                (h/render-table
                 {:dataspex/path []
                  :dataspex/pagination {[] {:page-size 2 :offset 2}}})
                (lookup/select [::ui/tbody ::ui/tr ::ui/string])
                (mapv lookup/text))
           ["Squirtle" "Magnemite"])))

  (testing "Allows sorting by column headers"
    (is (= (->> [{:point/label "Bulbasaur"
                  :point/latitude 37.807962
                  :point/longitude -122.475238}]
                (h/render-table
                 {:dataspex/path []
                  :dataspex/inspectee "Store"})
                (lookup/select '::ui/th)
                second
                lookup/attrs
                ::ui/actions)
           [[::actions/assoc-in ["Store" :dataspex/sorting [] :key] :point/label]
            [::actions/assoc-in ["Store" :dataspex/sorting [] :order] :dataspex.sort.order/ascending]])))

  (testing "Flips sorting order when clicking on sorted column"
    (is (= (->> [{:point/label "Bulbasaur"
                  :point/latitude 37.807962
                  :point/longitude -122.475238}]
                (h/render-table
                 {:dataspex/path []
                  :dataspex/inspectee "Conn"
                  :dataspex/sorting {[] {:key :point/label
                                        :order :dataspex.sort.order/ascending}}})
                (lookup/select '::ui/th)
                second
                lookup/attrs
                ::ui/actions)
           [[::actions/assoc-in ["Conn" :dataspex/sorting [] :order] :dataspex.sort.order/descending]])))

  (testing "Flips sorting order back when clicking on sorted column a second time"
    (is (= (->> [{:point/label "Bulbasaur"
                  :point/latitude 37.807962
                  :point/longitude -122.475238}]
                (h/render-table
                 {:dataspex/path []
                  :dataspex/inspectee "Store"
                  :dataspex/sorting {[] {:key :point/label
                                        :order :dataspex.sort.order/descending}}})
                (lookup/select '::ui/th)
                second
                lookup/attrs
                ::ui/actions)
           [[::actions/assoc-in ["Store" :dataspex/sorting [] :order] :dataspex.sort.order/ascending]]))))
