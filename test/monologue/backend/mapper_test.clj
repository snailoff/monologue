(ns monologue.backend.mapper-test
  (:require [monologue.backend.mapper :refer :all]
            [monologue.backend.constant :refer :all]
            [clojure.test :refer [deftest is]]))


(deftest meta-test
  (let [meta-name "test-meta"
        meta-content "123"]
    (save-meta meta-name meta-content)
    (is (= meta-name (:meta (select-meta-one meta-name))))
    (is (= meta-content (select-meta-content meta-name)))
    (is (= 1 (first (delete-meta-content meta-name))))))


(deftest piece-base-test
  (let [conn db-config
        data {:id      "1234567890abcde"
              :subject "nana"
              :summary "hehe"
              :content "lala"
              }]
    (is (= 1 (first (upsert-piece conn data))))
    (let [data2 (select-piece-by-id conn (data :id))]
      (is (= (data :id) (data2 :id)))
      (is (= (data :subject) (data2 :subject)))
      (is (= (data :summary) (data2 :summary)))
      (is (= (data :content) (data2 :content))))

    (is (= (select-piece-by-id conn (data :id))
           (select-piece-by-subject conn (data :subject))))

    (is (= 1 (first (delete-piece conn (data :id)))))))


(deftest piece-subject-upsert-test
  "subject 는 unique. data 는 업데이트 되지만 id를 변경하지 않는다."
  (let [conn db-config
        data {:id      "1234567890abcde"
              :subject "nana"
              :summary "hehe"
              :content "lala"
              }
        data2 (merge data {:id "1234567890zzzzz" :content "lala2"})]
    (upsert-piece conn data)
    (upsert-piece conn data2)

    (let [data2 (select-piece-by-subject conn (data :subject))]
      (is (= (data :id) (data2 :id)))
      (is (= "lala2" (data2 :content))))))



(deftest tag-piece-link-test
  (let [knot-id "1234567890aaaaa"
        piece-id "1234567890bbbbb"]
    (is (= 1 (first (upsert-link-knot-piece db-config knot-id piece-id))))
    (is (= piece-id
           (:piece_id (first (select-link-knot-piece-by-knot-id db-config knot-id)))))
    (is (= knot-id
           (:tag_id (first (select-link-knot-piece-by-piece-id db-config piece-id)))))
    (is (= 1 (first (delete-link-knot-piece-by-piece-id db-config piece-id))))))


(deftest tag-piece-remove-by-piece-test
  (let [knot-id "1234567890aaaaa"
        piece-id1 "1234567890bbbbb"
        piece-id2 "1234567890ccccc"]
    (is (= 1 (first (upsert-link-knot-piece db-config knot-id piece-id1))))
    (is (= 1 (first (upsert-link-knot-piece db-config knot-id piece-id2))))

    ; delete p1
    (is (= 1 (first (delete-link-knot-piece-by-piece-id db-config piece-id1))))
    (is (= piece-id2
           (:piece_id (first (select-link-knot-piece-by-knot-id db-config knot-id)))))

    ; delete p2
    (is (= 1 (first (delete-link-knot-piece-by-piece-id db-config piece-id2))))
    (is (= 0 (count (select-link-knot-piece-by-knot-id db-config knot-id))))))

(deftest tag-piece-remove-by-knot-test
  (let [knot-id "1234567890aaaaa"
        piece-id1 "1234567890bbbbb"
        piece-id2 "1234567890ccccc"]
    (is (= 1 (first (upsert-link-knot-piece db-config knot-id piece-id1))))
    (is (= 1 (first (upsert-link-knot-piece db-config knot-id piece-id2))))

    ; check
    (is (= 2 (count (select-link-knot-piece-by-knot-id db-config knot-id))))

    ; delete knot
    (is (= 2 (first (delete-link-knot-piece-by-knot-id db-config knot-id))))

    (is (= 0 (count (select-link-knot-piece-by-knot-id db-config knot-id))))))





(deftest piece-piece-test
  (let [piece-id1 "1234567890bbbbb"
        piece-id2 "1234567890ccccc"]
    (is (= 1 (first (insert-link-piece-piece db-config piece-id1 piece-id2))))
    (is (= piece-id1
           (:from_piece_id (first (select-link-piece-piece-by-to-id db-config piece-id2)))))
    (is (= piece-id2
           (:to_piece_id (first (select-link-piece-piece-by-from-id db-config piece-id1)))))

    (is (= 1 (first (delete-link-piece-piece db-config piece-id1 piece-id2))))

    ; delete by from
    (is (= 1 (first (insert-link-piece-piece db-config piece-id1 piece-id2))))
    (is (= 1 (first (delete-link-piece-piece-by-from-id db-config piece-id1))))

    ; delete by to
    (is (= 1 (first (insert-link-piece-piece db-config piece-id1 piece-id2))))
    (is (= 1 (first (delete-link-piece-piece-by-to-id db-config piece-id2))))))






(deftest parse-knot-test
  (let [knot-name "9876543210"
        piece-id "000000000000000"]
    (is (= nil (select-piece-by-subject db-config knot-name)))
    (is (= nil (select-piece-by-subject db-config piece-id)))
    (upsert-piece db-config {:id      piece-id
                             :subject piece-id})

    ; knot 이 없을 때
    (parse-tag piece-id (str "lala #" knot-name " hehe"))

    (let [link-knot-piece (first (select-link-knot-piece-by-piece-id db-config piece-id))
          new-knot (select-piece-by-id db-config (link-knot-piece :tag_id))]
      ; new knot check
      (is (= knot-name
             (new-knot :subject))))

    ; knot 이 있을 때
    (parse-tag piece-id (str "lala #" knot-name " hehe"))
    (let [link-knot-piece (first (select-link-knot-piece-by-piece-id db-config piece-id))
          new-knot (select-piece-by-id db-config (link-knot-piece :tag_id))]
      ; new knot check
      (is (= knot-name
             (new-knot :subject)))
      (delete-piece db-config (new-knot :id))
      (delete-piece db-config piece-id))))




(deftest parse-link-test
  (let [piece-id1 "000000000000000"
        piece-id2 "111111111111111"]
    (is (= nil (select-piece-by-subject db-config piece-id1)))
    (is (= nil (select-piece-by-subject db-config piece-id2)))
    (upsert-piece db-config {:id piece-id1 :subject piece-id1})
    (upsert-piece db-config {:id piece-id2 :subject piece-id2})

    (parse-link piece-id1 (str "nana [[" piece-id2 "]] huhu"))

    (let [link-piece-piece (first (select-link-piece-piece-by-from-id db-config piece-id1))]
      ; check
      (is (= piece-id1 (link-piece-piece :from_piece_id)))
      (is (= piece-id2 (link-piece-piece :to_piece_id))))

    (delete-link-piece-piece db-config piece-id1 piece-id2)

    (delete-piece db-config piece-id1)
    (delete-piece db-config piece-id2)))





(deftest save-piece-test
  (let [subject "abcdefghijklmn"
        md-name (str subject ".md")
        content "nana"]
    (is (= nil (select-piece-by-subject db-config subject)))
    (save-piece {:path    md-name
                 :content content})

    (let [piece (select-piece-by-subject db-config subject)]
      (is (= subject (piece :subject)))
      (is (= content (piece :content)))
      (delete-piece db-config (piece :id)))))



(deftest remove-piece-test
  (let [subject "abcdefghijklmn"
        md-name (str subject ".md")
        content "nana"]
    (is (= nil (select-piece-by-subject db-config subject)))
    (save-piece {:path    md-name
                 :content content})

    (let [piece (select-piece-by-subject db-config subject)]
      (is (= subject (piece :subject)))
      (is (= content (piece :content))))
    (remove-piece md-name)))