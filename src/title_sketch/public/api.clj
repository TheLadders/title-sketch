(ns title-sketch.public.api
  (:require [title-sketch.private.rh-utils :as rhu]
            [clojure.string :as cstr])
  (:import org.apache.commons.lang3.StringEscapeUtils
           (info.debatty.java.stringsimilarity
            QGram
            NGram)))

;-------------------------------------------------------------------------------
;----------EXTERNAL FUNCTIONS---------------------------------------------------
;-------------------------------------------------------------------------------
(defn pre-process-title
  "Apply series of functions to a job title."
  [title]
  (-> title
      rhu/replace-html
      cstr/lower-case
      rhu/stopword-replacement
      rhu/abbreviation-replacement
      rhu/rep-symbol-space
      rhu/remove-whitespace))


(defn post-process-title
  "Process title to have similarity performed."
  [title]
  (-> title
      pre-process-title
      rhu/remove-level
      rhu/remove-duplicates))

(defn- get-level-id
  "Use clean title to get level-ids and map to level-maps for a given title"
  [title]
  (-> title
      pre-process-title
      rhu/match-level-id
      rhu/filter-level-map))

(defn get-level
  "Use levels from get-levels to extract only the level group."
  [title]
  (->> title
       get-level-id
       (map :level_group)
       first))


;(def dig (QGram. 2))
;(.distance dig "title1" "title2")
; (def qgram-6 (QGram. 6))
(defn ngram-similarity
  "Find distance between two titles.
  Optional third parameter c when set to 1 uses clean titles. Otherwise default of 0 is input title.
  Optional fourth parameter q is number n of n-grams. Default is 1."
  ([t1 t2] (ngram-similarity t1 t2 0 1))
  ([t1 t2 c] (ngram-similarity t1 t2 c 1))
  ([t1 t2 c n]
    (let [ngram-n (NGram. n)]
      (cond
        (= c 0) (.distance ngram-n t1 t2)
        (= c 1) (.distance ngram-n (pre-process-title t1) (pre-process-title t2))))))
; .7 Similarity Score
; What to do with the word consultant
