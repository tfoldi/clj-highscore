(ns net-starschema.utils.inflector
  (:require [clojure.string :as str]))

(defn- keyword-to-str [s] (if (keyword? s) (subs (str s) 1) s))

(defn keywordize
  "Convert a string to keyword"
  [s] (-> (if (keyword? s) (subs (str s) 1) s)
          (str/lower-case)
          (str/replace "_" "-")
          (str/replace "." "-")
          (keyword)))


(defn clojurize
  [s] (-> (str/lower-case s)
          (str/replace "_" "-")
          (str/replace "." "-")))

(defn sqlify-str
  "Converts the clojure form of dashed string into an underscored one"
  [s]
  (-> (keyword-to-str s)
      (str/lower-case)
      (str/replace "-" "_")))

(defn sqlify
  "Converts the clojure form of dashed string into an underscored one and turns it into a
  keyword"
  [s]
  (keyword (sqlify-str s)))

