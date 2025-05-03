(ns dataspex.hiccup-test
  (:require [clojure.test :refer [deftest is testing]]
            [dataspex.actions :as-alias actions]
            [dataspex.helper :as h]
            [dataspex.hiccup :as hiccup]
            [dataspex.icons :as-alias icons]
            [dataspex.ui :as-alias ui]
            [lookup.core :as lookup]))

(deftest inflect-test
  (testing "Inflects words"
    (is (= (hiccup/enumerate 1 "banana") "1 banana"))
    (is (= (hiccup/enumerate 2 "banana") "2 bananas"))
    (is (= (hiccup/inflect 2 "banana") "bananas"))
    (is (= (hiccup/inflect 2 "entity") "entities"))))

(deftest bounded-size
  (testing "0 is 0"
    (is (= (hiccup/bounded-size 0 :hello) 0)))

  (testing "Reports bounded size of keyword"
    (is (= (hiccup/bounded-size 100 :hello) 6))
    (is (= (hiccup/bounded-size 100 :hello/world) 12))
    (is (= (hiccup/bounded-size 10 :hello/world) 10)))

  (testing "Reports bounded size of symbol"
    (is (= (hiccup/bounded-size 100 'symbol) 6))
    (is (= (hiccup/bounded-size 100 'hello/world) 11))
    (is (= (hiccup/bounded-size 10 'hello/world) 10)))

  (testing "Reports bounded size of string"
    (is (= (hiccup/bounded-size 100 "Hello") 7))
    (is (= (hiccup/bounded-size 3 "Hello") 3)))

  (testing "Reports bounded size of boolean"
    (is (= (hiccup/bounded-size 100 true) 4))
    (is (= (hiccup/bounded-size 3 false) 3)))

  (testing "Reports bounded size of number"
    (is (= (hiccup/bounded-size 100 112) 3))
    (is (= (hiccup/bounded-size 3 1024) 3)))

  (testing "Reports bounded size of map"
    (is (= (hiccup/bounded-size 100 {:name "Christian"}) 19))
    (is (= (hiccup/bounded-size 100 {:name "Christian" :lang "Clojure"}) 36))
    (is (= (hiccup/bounded-size 5 {:name "Christian"}) 5)))

  (testing "Reports bounded size of vector"
    (is (= (hiccup/bounded-size 100 [:a :b]) 7))
    (is (= (hiccup/bounded-size 5 [:a :b :c]) 5)))

  (testing "Reports bounded size of set"
    (is (= (hiccup/bounded-size 100 #{:a :b}) 8))
    (is (= (hiccup/bounded-size 5 #{:a :b :c}) 5)))

  (testing "Reports bounded size of list"
    (is (= (hiccup/bounded-size 100 '(:a :b)) 7))
    (is (= (hiccup/bounded-size 5 '(:a :b :c)) 5)))

  (testing "Reports bounded size of seq"
    (is (= (hiccup/bounded-size 100 (map identity '(:a :b))) 7))
    (is (= (hiccup/bounded-size 5 (range)) 5)))

  #?(:cljs
     (testing "Reports bounded size of js array"
       (is (= (hiccup/bounded-size 100 #js [0 1 2 3]) 13))
       (is (= (hiccup/bounded-size 5 #js [0 1 2 3]) 5)))))

(defn MyConstructor [data]
  #?(:cljs (this-as this
             (set! (.-data this) data))
     :clj data))

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
    (is (= (-> (h/render-inline ["hello"])
               h/strip-attrs)
           [::ui/vector
            [::ui/string "hello"]])))

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

  (testing "Renders hiccup summary"
    (is (= (-> [:div {:data-theme "light"}
                [:h1.text-lg "Hello world"]
                [:p "Hope you're doing well"]]
               h/render-inline)
           [::ui/hiccup
            [::ui/vector {}
             [::ui/hiccup-tag :div]
             [::ui/map
              [::ui/map-entry
               [::ui/keyword :data-theme]
               [::ui/string "light"]]]
             [::ui/code "..."]]])))

  (testing "Renders short list"
    (is (= (-> (h/render-inline (list :hello))
               h/strip-attrs)
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
    (is (= (-> (h/render-inline (range 0 5))
               h/strip-attrs)
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
           [::ui/link "(100+ items)"])))

  (testing "Renders short set"
    (is (= (-> (h/render-inline #{:hello})
               h/strip-attrs)
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
       (is (= (-> (h/render-inline #js ["hello"])
                  (h/strip-attrs #{::ui/actions}))
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

  #?(:cljs
     (testing "Renders object with custom constructor"
       (is (= (-> (MyConstructor. "Secret data")
                  h/render-inline)
              [::ui/map {:dataspex.ui/prefix "#js/dataspex$hiccup_test$MyConstructor"}
               [::ui/map-entry
                [::ui/keyword :data]
                [::ui/string "Secret data"]]]))))

  (testing "Renders inline atom"
    (is (= (-> (h/render-inline (atom {}))
               (h/strip-attrs #{:dataspex.ui/actions}))
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
             :dataspex/inspectee "Clojars"
             :dataspex/activity :dataspex.activity/browse}
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
                  :dataspex/path [:libraries]
                  :dataspex/activity :dataspex.activity/browse})
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
                  :dataspex/path [:lib-names]
                  :dataspex/activity :dataspex.activity/browse})
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

(deftest source-test
  (testing "Renders string"
    (is (= (h/render-source "String")
           [::ui/source [::ui/string "String"]])))

  (testing "Renders keyword"
    (is (= (h/render-source :key/word)
           [::ui/source [::ui/keyword :key/word]])))

  (testing "Renders number"
    (is (= (h/render-source 42)
           [::ui/source [::ui/number 42]])))

  (testing "Renders boolean"
    (is (= (h/render-source true)
           [::ui/source [::ui/boolean true]])))

  (testing "Renders symbol"
    (is (= (h/render-source 'sym/bol)
           [::ui/source [::ui/symbol 'sym/bol]])))

  (testing "Renders vector"
    (is (= (-> (h/render-source ["Apples" "Bananas"])
               h/strip-attrs)
           [::ui/source
            [::ui/vector
             [::ui/string "Apples"]
             [::ui/string "Bananas"]]])))

  (testing "Does not abbreviate long vector"
    (is (= (->> [{:fruit/id :apple
                  :fruit/name "Apple"}
                 {:fruit/id :banana
                  :fruit/name "Banana"}
                 {:fruit/id :pear
                  :fruit/name "Pear"}
                 {:fruit/id :orange
                  :fruit/name "Orange"}
                 {:fruit/id :kiwi
                  :fruit/name "Kiwi"}
                 {:fruit/id :litchi
                  :fruit/name "Litchi"}
                 {:fruit/id :durian
                  :fruit/name "Durian"}]
                h/render-source
                (lookup/select '::ui/string)
                (mapv lookup/text))
           ["Apple" "Banana" "Pear" "Orange"
            "Kiwi" "Litchi" "Durian"])))

  (testing "Renders list"
    (is (= (-> (h/render-source (list "Apples" "Bananas"))
               h/strip-attrs)
           [::ui/source
            [::ui/list
             [::ui/string "Apples"]
             [::ui/string "Bananas"]]])))

  (testing "Does not abbreviate long list"
    (is (= (->> '({:fruit/id :apple
                   :fruit/name "Apple"}
                  {:fruit/id :banana
                   :fruit/name "Banana"}
                  {:fruit/id :pear
                   :fruit/name "Pear"}
                  {:fruit/id :orange
                   :fruit/name "Orange"}
                  {:fruit/id :kiwi
                   :fruit/name "Kiwi"}
                  {:fruit/id :litchi
                   :fruit/name "Litchi"}
                  {:fruit/id :durian
                   :fruit/name "Durian"})
                h/render-source
                (lookup/select '::ui/string)
                (mapv lookup/text))
           ["Apple" "Banana" "Pear" "Orange"
            "Kiwi" "Litchi" "Durian"])))

  (testing "Renders seq"
    (is (= (-> (h/render-source (map identity '("Apples" "Bananas")))
               h/strip-attrs)
           [::ui/source
            [::ui/list
             [::ui/string "Apples"]
             [::ui/string "Bananas"]]])))

  (testing "Does not abbreviate long seq"
    (is (= (->> '({:fruit/id :apple
                   :fruit/name "Apple"}
                  {:fruit/id :banana
                   :fruit/name "Banana"}
                  {:fruit/id :pear
                   :fruit/name "Pear"}
                  {:fruit/id :orange
                   :fruit/name "Orange"}
                  {:fruit/id :kiwi
                   :fruit/name "Kiwi"}
                  {:fruit/id :litchi
                   :fruit/name "Litchi"}
                  {:fruit/id :durian
                   :fruit/name "Durian"})
                (map identity)
                h/render-source
                (lookup/select '::ui/string)
                (mapv lookup/text))
           ["Apple" "Banana" "Pear" "Orange"
            "Kiwi" "Litchi" "Durian"])))

  (testing "Paginates indefinite seq"
    (is (= (->> (range)
                (h/render-source
                 {:dataspex/path []
                  :dataspex/pagination
                  {[] {:page-size 3
                       :offset 2}}})
                (lookup/select-one ::ui/list)
                lookup/children
                (mapv lookup/text))
           ["2 more" "2" "3" "4" "100+ more"])))

  (testing "Renders set"
    (is (= (-> (h/render-source #{"Apples" "Bananas"})
               h/strip-attrs)
           [::ui/source
            [::ui/set
             [::ui/string "Apples"]
             [::ui/string "Bananas"]]])))

  (testing "Does not abbreviate long set"
    (is (= (->> #{{:fruit/id :apple
                   :fruit/name "Apple"}
                  {:fruit/id :banana
                   :fruit/name "Banana"}
                  {:fruit/id :pear
                   :fruit/name "Pear"}
                  {:fruit/id :orange
                   :fruit/name "Orange"}
                  {:fruit/id :kiwi
                   :fruit/name "Kiwi"}
                  {:fruit/id :litchi
                   :fruit/name "Litchi"}
                  {:fruit/id :durian
                   :fruit/name "Durian"}}
                h/render-source
                (lookup/select '::ui/string)
                (mapv lookup/text))
           ["Apple" "Banana" "Durian" "Kiwi"
            "Litchi" "Orange" "Pear"])))

  (testing "Renders atom source"
    (is (= (-> (atom {:name "Dataspex"})
               h/render-source
               (h/strip-attrs #{::ui/actions}))
           [::ui/source
            [::ui/vector
             {::ui/prefix "#atom"}
             [::ui/map
              [::ui/map-entry
               [::ui/keyword :name]
               [::ui/string "Dataspex"]]]]])))

  (testing "Renders hiccup"
    (is (= (->> [:div
                 [:h1 "Hello world"]
                 [:p.text-sm "How are you doing?"]]
                (h/render-source
                 {:dataspex/path [:users]
                  :dataspex/inspectee "Page data"}))
           [::ui/hiccup
            [::ui/vector
             [::ui/hiccup-tag
              {:data-folded "false"
               ::ui/actions [[::actions/assoc-in
                              ["Page data" :dataspex/folding [:users 0]]
                              {:folded? true
                               :ident [:div]}]]}
              :div]
             [::ui/vector
              [::ui/hiccup-tag
               {:data-folded "false"
                ::ui/actions [[::actions/assoc-in
                               ["Page data" :dataspex/folding [:users 0 0]]
                               {:folded? true
                                :ident [:h1]}]]}
               :h1]
              [::ui/string "Hello world"]]
             [::ui/vector
              [::ui/hiccup-tag
               {:data-folded "false"
                ::ui/actions [[::actions/assoc-in
                               ["Page data" :dataspex/folding [:users 0 1]]
                               {:folded? true
                                :ident [:p.text-sm]}]]}
               :p.text-sm]
              [::ui/string "How are you doing?"]]]])))

  (testing "Collapses hiccup nodes after three levels"
    (is (= (->> [:div
                 [:div
                  [:div
                   [:section [:h1 "Hello"]]
                   [:section.text [:p "Aight?"]]
                   [:main
                    [:h2 "More stuff"]]]]]
                (h/render-source {:dataspex/inspectee "Page hiccup"
                                  :dataspex/path []})
                (lookup/select '[::ui/vector ::ui/vector ::ui/vector ::ui/vector])
                (mapv (juxt lookup/text (comp ::ui/actions lookup/attrs first lookup/children))))
           [[":section ..."
             [[::actions/assoc-in
               ["Page hiccup" :dataspex/folding [0 0 0 0]]
               {:folded? false
                :ident [:section]}]]]
            [":section.text ..."
             [[::actions/assoc-in
               ["Page hiccup" :dataspex/folding [0 0 0 1]]
               {:folded? false
                :ident [:section.text]}]]]
            [":main ..."
             [[::actions/assoc-in
               ["Page hiccup" :dataspex/folding [0 0 0 2]]
               {:folded? false
                :ident [:main]}]]]])))

  (testing "Explicitly collapses root hiccup node"
    (is (= (->> [:div
                 [:div
                  [:div
                   [:section [:h1 "Hello"]]
                   [:section.text [:p "Aight?"]]
                   [:main
                    [:h2 "More stuff"]]]]]
                (h/render-source {:dataspex/inspectee "Page hiccup"
                                  :dataspex/path []
                                  :dataspex/folding {[0]
                                                     {:folded? true
                                                      :ident [:div]}}}))
           [::ui/hiccup
            [::ui/vector
             [::ui/hiccup-tag
              {:data-folded "true"
               ::ui/actions
               [[::actions/assoc-in
                 ["Page hiccup" :dataspex/folding [0]]
                 {:folded? false
                  :ident [:div]}]]}
              :div]
             [::ui/code "..."]]])))

  (testing "Explicitly collapses hiccup node"
    (is (= (->> [:div
                 [:div
                  [:div
                   [:section [:h1 "Hello"]]
                   [:section.text [:p "Aight?"]]
                   [:main
                    [:h2 "More stuff"]]]]]
                (h/render-source {:dataspex/inspectee "Page hiccup"
                                  :dataspex/path []
                                  :dataspex/folding {[0 0]
                                                     {:folded? true
                                                      :ident [:div]}}})
                (lookup/select '[::ui/vector ::ui/vector])
                (mapv (juxt lookup/text (comp ::ui/actions lookup/attrs))))
           [[":div ..."
             [[:dataspex.actions/assoc-in
               ["Page hiccup" :dataspex/folding [0 0]]
               {:folded? false, :ident [:div]}]]]])))

  (testing "Displays explicitly expanded node"
    (is (= (->> [:div
                 [:div
                  [:div
                   [:main
                    [:h2 "More stuff"]]]]]
                (h/render-source
                 {:dataspex/path [:body]
                  :dataspex/inspectee "Page hiccup"
                  :dataspex/folding {[:body 0 0 0 0]
                                     {:folded? false
                                      :ident [:main]}}})
                (lookup/select '[:dataspex.ui/hiccup > ::ui/vector >
                                 ::ui/vector > ::ui/vector > ::ui/vector >
                                 :dataspex.ui/hiccup-tag])
                last
                lookup/attrs)
           {:data-folded "false"
            ::ui/actions
            [[::actions/assoc-in
              ["Page hiccup" :dataspex/folding [:body 0 0 0 0]]
              {:folded? true
               :ident [:main]}]]})))

  (testing "Does not fold empty nodes"
    (is (= (->> [:div]
                (h/render-source {:dataspex/inspectee "Page hiccup"
                                  :dataspex/path []}))
           [::ui/hiccup
            [::ui/vector
             [::ui/hiccup-tag :div]]]))

    (is (= (->> [:div [:h2]]
                (h/render-source {:dataspex/inspectee "Page hiccup"
                                  :dataspex/path []})
                (lookup/select-one [::ui/vector ::ui/vector]))
           [::ui/vector
            [::ui/hiccup-tag :h2]])))

  (testing "Does not display explicitly expanded node when ident has changed"
    ;; Avoids associating state with the node solely on the basis of position,
    ;; which means we can avoid the most glaring cases of state leakage when the
    ;; hiccup changes from render to render.
    (is (= (->> [:div
                 [:div
                  [:div
                   [:main
                    [:h2 "More stuff"]]]]]
                (h/render-source
                 {:dataspex/path []
                  :dataspex/inspectee "Page hiccup"
                  :dataspex/folding {[0 0 0 0] {:folded? false
                                                :ident [:div]}}})
                (lookup/select '[::ui/vector ::ui/vector ::ui/vector ::ui/vector])
                last
                (lookup/select-one ::ui/hiccup-tag)
                lookup/attrs
                :data-folded)
           "true")))

  (testing "Asks for attribute map to be rendered inline with the tag"
    (is (= (->> [:h2 {:data-tooltip "Learn here"} "More stuff"]
                h/render-source
                (lookup/select-one ::ui/map))
           [::ui/map {::ui/inline? true}
            [::ui/map-entry
             [::ui/keyword :data-tooltip]
             [::ui/string "Learn here"]]])))

  (testing "Does not put attribute map inline when tag is long"
    (is (empty?
         (->> [:h2.text-lg.flex.items-center {:data-tooltip "Learn here"} "More stuff"]
              h/render-source
              (lookup/select-one ::ui/map)
              lookup/attrs))))

  (testing "Renders lists of hiccup children as hiccup nodes"
    (is (= (->> [:div
                 (list [:h2 "More stuff"]
                       [:p "Text"])
                 (list (list [:p "More text"]))]
                h/render-source
                (lookup/select ::ui/hiccup-tag)
                (mapv lookup/text))
           [":div" ":h2" ":p" ":p"]))))
