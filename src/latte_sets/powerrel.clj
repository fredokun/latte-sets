(ns latte-sets.powerrel

  "Notions about the relational powerset construction.

  This is akin to [[latte-sets.powerset]] but for relations."

    (:refer-clojure :exclude [and or not set])

    (:require [latte.core :as latte :refer [definition defthm defaxiom defnotation
                                            defimplicit
                                            forall lambda
                                            assume have pose proof qed lambda]]
              [latte-prelude.quant :as q :refer [exists]]
              [latte-prelude.prop :as p :refer [<=> and or not]]
              [latte-prelude.equal :as eq :refer [equal]]

              [latte-sets.core :as s :refer [set elem seteq subset]]
              [latte-sets.rel :as r :refer [rel releq]]))

(definition powerrel
  "The powerset constructor for relations.

The term `(powerrel T U)' is the type
of sets whose elements are relations of type `(rel T U)`."
  [[T :type] [U :type]]
  (==> (rel T U) :type))

(definition rel-elem-def
  "Membership for powersets.
Th relation `R` is an element of the set `X`."
  [[T :type] [U :type] [R (rel T U)] [X (powerrel T U)]]
  (X R))

(defn fetch-powerrel-types [def-env ctx r-type]
  "Fetch the `T` and `U`  in a powerrel-type `s-type` of the form `(powerrel T U)` (fails otherwise).
This function is used for implicit in relations."
  (let [[RT _] (p/decompose-impl-type def-env ctx r-type)]
    (r/fetch-rel-type def-env ctx RT)))

(defimplicit rel-elem
  "The relation `R` is an element of the powerrel `X`, cf. [[rel-elem-def]]."
  [def-env ctx [R R-ty] [X X-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-elem-def T U R X)))

(definition rel-ex-def
  "The powerset existential for relations.
This is the definition of [[latte.quant/ex]] but
adpated for relations."
  [[T :type] [U :type] [X (powerrel T U)]]
  (forall [α :type]
    (==> (forall [R (rel T U)]
           (==> (rel-elem R X) α))
         α)))

(defimplicit rel-ex
  "There exists an element relation `R` of the powerset `X` such that... cf. [[rel-ex-def]]."
  [def-env ctx [X X-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-ex-def T U X)))

(defthm rel-ex-elim-thm
  "The elimination rule for the relation existential."
  [[T :type] [U :type] [X (powerrel T U)] [A :type]]
  (==> (rel-ex X)
       (forall [R (rel T U)]
         (==> (rel-elem R X) A))
       A))

(proof 'rel-ex-elim-thm
  (assume [H1 (rel-ex X)
           H2 (forall [R (rel T U)] (==> (rel-elem R X) A))]
    (have <a> (==> (forall [R (rel T U)]
                           (==> (rel-elem R X) A))
                   A) :by (H1 A))
    (have <b> A :by (<a> H2)))
  (qed <b>))

(defimplicit rel-ex-elim
  "The elimination rule for the relation existential, cf. [[rel-ex-elim-thm]]."
  [def-env ctx [X X-ty] [A A-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-ex-elim-thm T U X A)))

(defthm rel-ex-intro-thm
  "Introduction rule for [[rel-ex]]."
  [[T :type] [U :type] [X (powerrel T U)] [R (rel T U)]]
  (==> (rel-elem R X)
       (rel-ex X)))

(proof 'rel-ex-intro-thm
  (assume [H (rel-elem R X)
           A :type
           Q (forall [S (rel T U)] (==> (rel-elem S X) A))]
    (have <a> (==> (rel-elem R X) A) :by (Q R))
    (have <b> A :by (<a> H)))
  (qed <b>))

(defimplicit rel-ex-intro
  "Introduction rule for relation existential, cf. [[rel-ex-intro-thm]]."
  [def-env ctx [X X-ty] [R R-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-ex-intro-thm T U X R)))

(definition rel-single-def
  "The relational powerset version of [[latte-prelude.quant/single]].
There exists at most one set ..."
  [[T :type] [U :type] [X (powerrel T U)]]
  (forall [R S (rel T U)]
    (==> (rel-elem R X)
         (rel-elem S X)
         (releq R S))))

(defimplicit rel-single
  "There is a single set element in `X` such that...
cf. [[set-single-def]]"
  [def-env ctx [X X-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-single-def T U X)))

(definition rel-unique-def
  "The relational powerset version of [[latte-prelude.quant/unique]].
There exists a unique set ..."
  [[T :type] [U :type] [X (powerrel T U)]]
  (and (rel-ex X)
       (rel-single X)))

(defimplicit rel-unique
  "There exists a unique set element in `X` such that...
cf. [[rel-unique-def]]"
  [def-env ctx [X X-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'rel-unique-def T U X)))

(defaxiom the-rel-ax
  "The relation powerset version of [[latte-prelude.quant/the]]."
  [[T :type] [U :type] [X (powerrel T U)] [u (rel-unique X)]]
  (rel T U))

(defimplicit the-rel
  "The unique descriptor for relation powerset `X`, cf. [[the-rel-ax]]."
  [def-env ctx [X X-ty] [u u-ty]]
  (let [[T U] (fetch-powerrel-types def-env ctx X-ty)]
    (list #'the-rel-ax T U X u)))

(defaxiom the-rel-prop
  "The property of the unique set descriptor [[the-rel]]."
  [[T :type] [U :type] [X (powerrel T U)] [u (rel-unique X)]]
  (rel-elem (the-rel X u) X))

(defthm the-rel-lemma
  "The unique relation ... is unique."
  [[T :type] [U :type] [X (powerrel T U)] [u (rel-unique X)]]
  (forall [R (rel T U)]
    (==> (rel-elem R X)
         (releq R (the-rel X u)))))

(proof 'the-rel-lemma
  (have <a> (rel-single X) :by (p/and-elim-right u))
  (have <b> (rel-elem (the-rel X u) X) :by (the-rel-prop T U X u))
  (assume [R (rel T U)
           HR (rel-elem R X)]
    (have <c> (==> (rel-elem R X)
                   (rel-elem (the-rel X u) X)
                   (releq R (the-rel X u))) :by (<a> R (the-rel X u)))
    (have <d> (releq R (the-rel X u)) :by (<c> HR <b>)))
  (qed <d>))


(comment

  ;; XXX : are generalization to relations of the following useful?

(definition unions-def
  "Generalized union.
This is the set {y:T | ∃x∈X, y∈x}."
  [[T :type] [X (powerset T)]]
  (lambda [y T]
          (set-ex (lambda [x (set T)]
                          (and (set-elem x X)
                               (elem y x))))))

(defimplicit unions
  "Generalized union.
This is the set {y:T | ∃x∈X, y∈x}."
  [def-env ctx [X X-ty]]
  (let [T (fetch-powerset-type def-env ctx X-ty)]
    (list #'unions-def T X)))

(defthm unions-upper-bound
   "The generalized union is an upper bound wrt. 
the subset relation."
   [[T :type] [X (powerset T)]]
   (forall [x (set T)]
     (==>  (set-elem x X)
           (subset x (unions X)))))

(proof 'unions-upper-bound
  (assume [x (set T)
           Hx (set-elem x X)]
    (assume [y T
             Hy (elem y x)]
      (pose I := (lambda [x (set T)]
                         (and (set-elem x X)
                              (elem y x))))
      (have <a> (set-elem x I) :by (p/and-intro Hx Hy))
      (have <b> (elem y (unions X)) :by ((set-ex-intro I x) <a>))))
  (qed <b>))

(definition intersections-def
  "Generalize intersections.
This is the set {y:T | ∀x∈X, y∈x}."
  [[T :type] [X (powerset T)]]
  (lambda [y T]
    (forall [x (set T)]
      (==> (set-elem x X)
           (elem y x)))))

(defimplicit intersections
  "Generalize intersections.
This is the set {y:T | ∀x∈X, y∈x}."
  [def-env ctx [X X-ty]]
  (let [T (fetch-powerset-type def-env ctx X-ty)]
    (list #'intersections-def T X)))

(defthm intersections-lower-bound
  "The generalized intersection is a lower bound wrt. the subset relation."
  [[T :type] [X (powerset T)]]
  (forall [x (set T)]
    (==> (set-elem x X)
         (subset (intersections X) x))))

(proof 'intersections-lower-bound
  (assume [x (set T)
           Hx (set-elem x X)]
    (assume [y T
             Hy (elem y (intersections X))]
      (have <a> (elem y x) :by (Hy x Hx))))
  (qed <a>))

(defthm intersections-prop
  "Preservation of properties on intersections."
  [[T :type] [P (==> T :type)] [X (powerset T)]]
  (forall [x (set T)]
    (==> (set-elem x X)
         (forall [y T]
           (==> (elem y x)
                (P y)))
         (forall [z T]
           (==> (elem z (intersections X))
                (P z))))))

(proof 'intersections-prop
  (assume [x (set T)
           H1 (set-elem x X)
           H2 (forall [y T]
                (==> (elem y x)
                     (P y)))]
    (assume [z T
             Hz (elem z (intersections X))]
      (have <a> (==> (elem z x)
                     (P z)) :by (H2 z))
      (have <b> (elem z x)
            :by ((intersections-lower-bound T X) x H1 z Hz))
      (have <c> (P z) :by (<a> <b>))))
  (qed <c>))


(definition full-powerset
  "The powerset containing all the subsets of type `T`."
  [[T :type]]
  (lambda [x (set T)]
    p/truth))

(defthm full-powerset-prop
  [[T :type]]
  (forall [x (set T)]
    (set-elem x (full-powerset T))))

(proof 'full-powerset-prop
  (assume [x (set T)]
    (have <a> ((full-powerset T) x) :by p/truth-is-true))
  (qed <a>))

(definition empty-powerset
  "The empty powerset."
  [[T :type]]
  (lambda [x (set T)]
    p/absurd))

(defthm empty-powerset-prop
  [[T :type]]
  (forall [x (set T)]
    (not (set-elem x (empty-powerset T)))))

(proof 'empty-powerset-prop
  (assume [x (set T)
           H (set-elem x (empty-powerset T))]
    (have <a> p/absurd :by H))
  (qed <a>))

(definition powerset1
  "The powerset of all the non-empty subsets of type `T`."
  [[T :type]]
  (lambda [x (set T)]
    (not (s/set-equal x (s/emptyset T)))))

(defthm powerset1-prop
  [[T :type] [x (set T)]]
  (==> (not (s/set-equal x (s/emptyset T)))
       (set-elem x (powerset1 T))))

(proof 'powerset1-prop
  (assume [H (not (s/set-equal x (s/emptyset T)))]
    (have <a> (set-elem x (powerset1 T)) :by H))
  (qed <a>))

(defthm powerset1-prop-conv
  [[T :type] [x (set T)]]
  (==> (set-elem x (powerset1 T))
       (not (s/set-equal x (s/emptyset T)))))

(proof 'powerset1-prop-conv
  (assume [H (set-elem x (powerset1 T))]
    (assume [Heq (s/set-equal x (s/emptyset T))]
      (have <a> (not (s/set-equal x (s/emptyset T)))
            :by H)
      (have <b> p/absurd :by (<a> Heq))))
  (qed <b>))

(defthm powerset1-prop-equiv
  [[T :type] [x (set T)]]
  (<=> (set-elem x (powerset1 T))
       (not (s/set-equal x (s/emptyset T)))))

(proof 'powerset1-prop-equiv
  (qed (p/and-intro (powerset1-prop T x)
                    (powerset1-prop-conv T x))))
)