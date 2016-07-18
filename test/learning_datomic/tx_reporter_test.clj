(ns learning-datomic.tx-reporter-test
  (:require [learning-datomic.tx-reporter :refer :all]
            [clojure.test :refer :all]))

(def create-todo-tx
  [[13194139534318 13194139534318 :db/txInstant #inst "2016-07-14T21:49:17.249-00:00" true]
   [13194139534318 17592186045423 :todo/title "I am a test todo item" true]
   [13194139534318 17592186045423 :todo/completed? false true]])

(deftest formatting-tx-test
  (is (= (format-tx create-todo-tx)
         {:tx 13194139534318
          :entities {17592186045423 {:asserted {:todo/title "I am a test todo item"
                                                :todo/completed? false}
                                     :retracted {}}}})))

