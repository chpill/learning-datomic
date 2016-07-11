(ns learning-datomic.tdd-test
  (:require [clojure.test :refer :all]
            [datomic.api :as datomic]
            [learning-datomic.tdd :refer :all]))

;; This code is taken from an example of TDD with datomic by mtnygard
;; https://www.youtube.com/watch?v=JaZ1Tm6ixCY

(def ^:dynamic *conn* nil)

(defn fresh-database []
  (let [db-name (gensym)
        db-uri (str "datomic:mem://" db-name)]
    (datomic/create-database db-uri)
    (datomic/connect db-uri)))

(defn database-fixture [f]
  (binding [*conn* (fresh-database)]
    (f)))

(use-fixtures :each database-fixture)

(deftest todo-db-test
  (testing "connect to the database"
    (is (some? *conn*))
    (is (some? (datomic/db *conn*))))

  (testing "load the schemas"
    (let [virgin-db (datomic/db *conn*)]
      (is (nil? (datomic/entity virgin-db [:db/ident :todo/title]))))
    ;; Very important to dereference the result of the transaction here!
    ;; Otherwise, if there is an error in the schema, we won't see the error
    ;; returned by datomic!
    @(datomic/transact *conn* schema)
    (let [db-with-schema (datomic/db *conn*)]
      (is (some? (datomic/entity db-with-schema [:db/ident :todo/title])))))
  (testing "add and query a todo by title"
    @(add-todo *conn* "I am a test todo item")
    (let [db (datomic/db *conn*)]
      (is (= 1 (count (todo-by-title db "I am a test todo item")))))))

(deftest tags-db-test
  (testing "load initial tags into db"
    @(datomic/transact *conn* initial-tags)
    (is (every? #(datomic/entity (datomic/db *conn*) [:db/ident %])
                [:todo.tags/bug
                 :todo.tags/chore
                 :todo.tags/feat
                 :todo.tags/refactor])))
  (testing "find tags in db"
    (is (= 4 (count (find-all-tags (datomic/db *conn*))))))
  (testing "add tag to db"
    @(add-tag *conn* :todo.tags/test)
    (is (= 5 (count (find-all-tags (datomic/db *conn*))))))
  (testing "trying to add broken tag to db"
    (is (thrown? AssertionError
                 @(add-tag *conn* :not-the-right-namespace/test)))))

(run-tests)
