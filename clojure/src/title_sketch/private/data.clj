(ns title-sketch.private.data
  (:import [java.io File FileReader StringReader OutputStreamWriter InputStreamReader])
  (:require [clojure.string :as cstr]
            [clojure.java.io :as jio]
            [util
             [util :as util]
             [io :as io]]))

(def ^:dynamic *data-path* "data/")

(defn get-resource-path
  ([file]
   (jio/resource (str *data-path* "/" file))))

(defn convert-data
  "Uses the first row as column labels and returns a collection of hash maps"
  [data]
  (let [labels (map util/to-kw (first data))]
    (map (partial zipmap labels) (rest data))))

(defn load-txt-header
  [file-name & {:keys [key-fn]
                :or {key-fn identity}}]
  (let [txt-resource (get-resource-path file-name)]
    (with-open [r (jio/reader txt-resource)]
      (let [[header & lines] (line-seq r)
            headers (map keyword (cstr/split header #"\t"))]
        (->> lines
             (map #(cstr/split % #"\t"))
             (map (partial zipmap headers))
             doall)))))

(defn load-csv-header
  [file-name & {:keys [key-fn]
                             :or {key-fn identity}}]
  (convert-data  (io/read-csv (get-resource-path file-name)
                               :separators #{\,}
                               :enclosing {\" \"})))
