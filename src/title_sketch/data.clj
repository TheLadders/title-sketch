(ns title-sketch.data
  (:import [java.io File FileReader StringReader OutputStreamWriter InputStreamReader])
  (:require [clojure.string :as cstr]
            [clojure.java.io :as jio]))

(def ^:dynamic *data-path* "data/")
(def ^:dynamic *limit-file-size* false)

(defn get-resource-path
  ([file]
   (jio/resource (str *data-path* "/" file))))

(defn to-kw [id]
  (cond
    (string? id) (keyword id)
    (keyword? id) id
    (symbol? id) (keyword (name id))
    :default (keyword id) ))


(defn parse-log-line
  "parse a log line separated by :separators enclosed by :enclosing-map"
  [log-str &
   {:keys [enclosing separators]
    :or   {enclosing  {\[ \] \" \"}
           separators #{ \space \newline \tab}}}]
  (loop [line         (apply vector log-str)
         field        []
         result       []
         closing-char nil]
    (let [c (first line)]
      (cond
       ;; nili is the end of the string
        (nil? c) (if (> (count field) 0)
                   (conj result (apply str field) )
                   result)
        ;; we are in an enclosed string -- add everything to the field until we find the close
        (not (nil? closing-char))
        (cond
          ;; we are no longer in an enclosure - just end it.
          ;; the field will end with the next separator
          (= c closing-char)
          (recur (rest line)
                 field
                 result
                 nil)
          :default
          (recur (rest line)
                 (conj field c)
                 result
                 closing-char))
        ;; check to see if we need to start an enclosed string
        (contains? enclosing c)
        (recur (rest line)
               field
               result
               (get enclosing c))
        ;; now check for the field separators so we can commit the field to our list
        (contains? separators c)
        (recur  (rest line)
                []
                (conj result (apply str field) )
                closing-char)
         ;;; regular characters just get added to the current field
        :default (recur
                  (rest line)
                  (conj field c)
                  result
                  closing-char)))
    ))


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



(defn read-csv-with-reader
  [r &
   {:keys [separators enclosing lines line-end]
    :or   {lines 100 }}]
  {:pre [(if (nil? lines) true (> lines 0))
         (complement (nil? r)) ]}
  (let [seq   (line-seq r)
        ;; force max lines to be 1000
        lines (if *limit-file-size*
                (if-not (and lines (< lines 10000)) 10000 lines)
                lines)]
    (loop [file-seq seq
           v        []
           n        0]
      (if (and file-seq
               (not (empty? file-seq))
               (or (and lines (< n lines))
                   (not lines))) ;; don't pay attention to the count if lines is nil

        (recur (rest file-seq)
               (conj
                v
                (parse-log-line (first file-seq)
                                :enclosing enclosing
                                :separators separators))
               ;; (string/split (first file-seq) (re-pattern separator)))
               (inc n))
        v))))



(defn read-csv
  "returns a rowm matrix for a data / csv file pointed to by path.
Optional parameters
===================
separators is a set of characters that separate fields. defaults to , .
enclosing is a map of begin character to end character for enclosing fields that contain 'separators' in them."
  ([path &
    {:keys [separators enclosing line-end max-lines header]
     :or {separators #{ ","}
          line-end #{"\n"} }}]
     (with-open [r (cond
                    (string? path) (java.io.BufferedReader. (FileReader. ^String path))
                    (= (type path) java.net.URL) (java.io.BufferedReader.
                                                  (InputStreamReader. (.openStream ^java.net.URL path))))]
       (read-csv-with-reader r
                             :separators separators
                             :enclosing enclosing
                             :lines max-lines
                             :line-end line-end)))
  ([data-src] (read-csv (:in-path data-src)
                        :separators (:separators data-src)
                        :enclosing (:enclosing data-src)
                        :lines (:max-lines data-src)
                        :line-end (:line-end data-src))))


(defn- convert-data
  "Uses the first row as column labels and returns a collection of hash maps"
  [data]
  (let [labels (map to-kw (first data))]
    (map (partial zipmap labels) (rest data))))

(defn load-csv-header
  [file-name & {:keys [key-fn]
                             :or {key-fn identity}}]
  (convert-data  (read-csv (get-resource-path file-name)
                               :separators #{\,}
                               :enclosing {\" \"})))
