(ns title-sketch.private.kvstore
  (:require [util
             [io :as io]]))

(def ^:dynamic *kvfile-data-dir* "resources")

(defn- mk-path
  ([namespace k]
   (str *kvfile-data-dir* "/"
        namespace "/"
        k))
  ([namespace]
   (str *kvfile-data-dir* "/" namespace "/")))

(defn- create-path [path]
  (io/mkdir path))

(defn kv-read
  [namespace k]
  (try  (slurp (mk-path namespace k))
        (catch Throwable e
          nil)))

(defn kv-mread
  [namespace keys]
  (into {} (map #(vector % (kv-read namespace %))
                keys)))

(defn kv-write
  [namespace k data]
  (let [ns-path (create-path (mk-path namespace))]
    (spit (mk-path namespace k) data)))

(defn kv-delete
  [namespace k]
  (try  (io/rm-file (mk-path namespace k))
        (catch Throwable e
          nil)))
