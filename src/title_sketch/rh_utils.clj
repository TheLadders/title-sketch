(ns title-sketch.rh-utils
  (:require [clojure.java.io :as jio]
            [clojure.string :as cstr]
            [title-sketch.data :as data])
  (:import org.apache.commons.lang3.StringEscapeUtils
           (info.debatty.java.stringsimilarity
            QGram
            NGram)))



(def level-regex  (data/load-txt-header "level_regex.txt"))
(def abbr-rep     (data/load-csv-header "abbreviation_replacement.csv"))
(def html-rep     (data/load-csv-header "html_replacement.csv"))
(def level-map    (data/load-csv-header "level_map.csv"))
(def levels       (data/load-csv-header "levels.csv"))
(def stopword-rep (data/load-csv-header "stopword_replacement.csv"))

;-------------------------------------------------------------------------------
;----------PRE PROCESS----------------------------------------------------------
;-------------------------------------------------------------------------------

(defn- replace-html
  "Search for raw HTML (ascii codes) and replace with respective ascii character."
  [title]
  (reduce (fn [new-title [hex display]]
            (cstr/replace new-title hex display))
          title
          (map (juxt :hex :display) html-rep)))

(defn- word-replacement
  "Match words between a space or at beginning/end of a string."
  [title replacement-set]
  (reduce (fn [new-title [orig repl]]
           (cstr/replace new-title (re-pattern (str "(^| )(" orig ")($| )")) (str repl)))
            title (doall (map (juxt :original :replacement) replacement-set))))
; DOES DOALL THIS MAKE SENSE HERE?!?!?!?!

(defn- stopword-replacement
  "Replace all stopwords using word-replacement function."
  [title]
  (word-replacement title stopword-rep))

(defn- abbreviation-replacement
  "Replace all abbreviations using word-replacement function."
  [title]
  (word-replacement title abbr-rep))

(defn- rep-symbol-space
  "Remove all characters except for numbers and letters."
  [title]
  (cstr/replace title #"[^0-9a-z]" " "))

(defn- remove-whitespace
  "Remove all white space and leave only single spaces between words.
  This is due to the remove-symbols function."
  [title]
  (cstr/trim (cstr/replace title #"\s+" " ")))



;-------------------------------------------------------------------------------
;----------POST PROCESS---------------------------------------------------------
;-------------------------------------------------------------------------------
(defn- level-indexer
  "Retrieve level term according to index."
  [n]
  (:name (nth levels n)))

(defn- remove-level
  "Function to remove title from a cleaned job title.
  This is the second to last step before string similarity calculated."
  [title]
  (loop [i 0 new-title title]
    (cond
      (>= i (count levels)) (remove-whitespace new-title)
      :else (recur (inc i) (cstr/replace new-title (re-pattern (level-indexer i)) "")))))

(defn- remove-duplicates
  "Function to remove duplicate words from job title.
  This is the last step before string similarity calculated."
  [title]
  (cstr/join " " (distinct (cstr/split title #" "))))





;--------------------------------------------------------------------------------------------------------------
;----------LEVEL EXTRACTION------------------------------------------------------------------------------------
;--------------------------------------------------------------------------------------------------------------
(defn- re-pos
  "Input a regular expression and string. Return the string and index of match"
  [re string]
  (loop [m (re-matcher re string) res {}]
    (if (.find m)
      (recur m (assoc res (.start m) (.group m)))
      res)))

(defn- match-level
  "Match regular expression from level-regex source."
  [title n]
  (re-pos (re-pattern (:match (nth level-regex n))) title))

(defn- match-level-id
  "Loop through all level-regex and return any matches.
  If none appear, then return 24 which indicates no match and level-group 5"
  [title]
  (loop [i 0 ids []]
    (cond
     (>= i (count level-regex)) (if (empty? ids) (str "24") (last ids))
     (empty? (match-level title i)) (recur (inc i) ids)
     :else (recur (inc i) (conj ids (:level_id (nth level-regex i)))))))

(defn- filter-level-map
  "Filter level-map by level-id and return full entry"
  [n]
  (filter #(= (% :level_id) n) level-map))






;-------------------------------------------------------------------------------
;----------EXTERNAL FUNCTIONS---------------------------------------------------
;-------------------------------------------------------------------------------
(defn pre-process-title
  "Apply series of functions to a job title."
  [title]
  (-> title
    replace-html
    cstr/lower-case
    stopword-replacement
    abbreviation-replacement
    rep-symbol-space
    remove-whitespace))


(defn post-process-title
  "Process title to have similarity performed."
  [title]
  (->
    title
    pre-process-title
    remove-level
    remove-duplicates))

(defn get-level
  "Use clean title to get level-ids and map to level-maps for a given title"
  [title]
  (->
    title
    pre-process-title
    match-level-id
    filter-level-map))

(defn get-level-group
  "Use levels from get-levels to extract only the level group."
  [title]
  (first (map :level_group (get-level title))))


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
