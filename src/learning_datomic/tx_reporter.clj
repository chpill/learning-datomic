(ns learning-datomic.tx-reporter
  (:require [datomic.api :as datomic]))


(defn extract-tx-from-report [{:as tx-report :keys [db-after tx-data]}]
  (datomic.api/q '[:find ?tx ?e ?a-name ?v ?status
                   :in $ [[?e ?a ?v ?tx ?added]]
                   :where
                   ;; display the name of the of the attribute
                   [?a :db/ident ?a-name]
                   [({true "asserted"
                      false "retracted"} ?added) ?status]]
                 db-after
                 tx-data))

(defn format-tx [datoms]
  (let [tx (ffirst datoms)
        datoms-without-transaction (filter #(= tx (first %) datoms))
        datoms-by-entity (group-by second datoms)
        _ (println "here be datoms by entity")
        _ (println datoms-by-entity)
        result (reduce-kv (fn [acc k v] (conj acc [k v]))
                          {}
                          datoms-by-entity)]
    (println "=======================")
    (println result)
    result))

;; Lock before printing so that we don't get intertwined lines if someone else
;; prints at the same time
;; TODO FIXME There has got to be a cleaner way to do this >< logging library maybe?
;; NB the problem might stem from the fact that our logging is not atomic (ie
;; multiple function calls)
(defn print-tx! [datoms]
  (locking *out*
    (println "---------------------------------------------------------------------")
    (println "                     tx" (ffirst datoms)":" (count datoms) "datoms" )
    (println "---------------------------------------------------------------------")
    (let [datoms-without-transaction (map rest datoms)]
      (doseq [datom datoms-without-transaction]
        (println datom)))))

(defn start-tx-reporter! [conn]
  (let [tx-queue (datomic/tx-report-queue conn)]
    ;; This is seriously ugly, but it'll do for now.
    ;; TODO FIXME Check out how to roll processes with core async
    ;; (can we take from the tx-queue providing a callback?)
    (future
      (loop []
        ;; take will return one transaction at a time
        (->> (.take tx-queue)
            extract-tx-from-report
            (sort-by (juxt first second))
            print-tx!)
        (recur)))))
