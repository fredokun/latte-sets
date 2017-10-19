(ns latte-sets.core
  "Set-theoretic notions based on the subset
  approach of type theory.

  The main idea is to consider a typed variant of
  a mathematical set as a predicate over a given type.

  What is called a **set** will be technically-speaking
  a subset of a type, hence a predicate over a given type.
  This means that the set theory developed here is *typed*
  and thus quite different than the standard axiomatic
set theories (e.g. ZF and ZFC), which are essentially untyped.

  But many set-theoretic constructions and results have a
natural translation to the typed setting.
"

  (:refer-clojure :exclude [and or not set])

  (:require [latte.core :as latte :refer [definition defthm defaxiom defnotation
                                          defimplicit
                                          forall lambda
                                          assume have pose proof qed]]
            [latte.quant :as q :refer [exists]]
            [latte.prop :as p :refer [<=> and or not]]
            [latte.equal :as eq :refer [equal]]))

(definition set
  "The type of sets whose elements are of type `T`."
  [[T :type]]
  (==> T :type))

(defn fetch-set-type [def-env ctx s-type]
  "Fetch the `T` in a set-type `s-type` of the form `(set T)` (fails otherwise).
This function is used for implicit in sets."
  (let [[T _] (p/decompose-impl-type def-env ctx s-type)]
    T))

(definition elem-def
  "Set membership. 

Given a type `T`, a value `x` of type `T` and
 a set `s`, then `(elem T x s)` means that `x` is an element of set `s`.
 The standard mathematical notation is: `x`∊`s` (leaving the type `T` implicit)."
  [[T :type] [x T] [s (set T)]]
  (s x))

(defimplicit elem
  "Object `x` is member of set `s`, cf. [[elem-def]]."
  [def-env ctx [x x-ty] [s s-ty]]
  (list #'elem-def x-ty x s))

(defnotation forall-in
  "Universal quantification over sets.

  `(forall-in [x T s] (P x))` is a 
shortcut for `(forall [x T]
                 (==> (elem T x s)
                      (P x)))`."
  [binding body]
  (if (not= (count binding) 3)
    [:ko {:msg "Binding of `forall-in` should be of the form `[x T s]`."
          :binding binding}]
    [:ok `(forall [~(first binding) ~(second binding)]
                  (==> (elem ~(second binding) ~(first binding) ~(nth binding 2))
                       ~body))]))

(alter-meta! #'forall-in update-in [:style/indent] (fn [_] [1 :form :form]))

(defnotation exists-in
  "Existential quantification over sets.

  `(exists-in [x T s] (P x))` is a 
shortcut for `(exists [x T]
                 (and (elem T x s)
                      (P x)))`."
  [binding body]
  (if (not= (count binding) 3)
    [:ko {:msg "Binding of `exists-in` should be of the form `[x T s]`."
          :binding binding}]
    [:ok `(exists [~(first binding) ~(second binding)]
                  (and (elem ~(second binding) ~(first binding) ~(nth binding 2))
                       ~body))]))

(alter-meta! #'exists-in update-in [:style/indent] (fn [_] [1 :form :form]))

(definition fullset
  "The full set of a type
(all the inhabitants of the type are element
of the full set)."
  [[T :type]]
  (lambda [x T] p/truth))

(defthm fullset-intro
  "Introduction rule for the full set."
  [[T :type]]
  (forall [x T]
    (elem x (fullset T))))

(proof 'fullset-intro :script
  (assume [x T]
    (have <a> (elem x (fullset T)) :by p/truth-is-true))
  (qed <a>))

(definition emptyset
  "The empty set of a type."
  [[T :type]]
  (lambda [x T] p/absurd))

(defthm emptyset-prop
  "The main property of the empty set."
  [[T :type]]
  (forall [x T]
    (not (elem x (emptyset T)))))

(proof 'emptyset-prop :script
  (assume [x T
           H (elem x (emptyset T))]
    (have <a> p/absurd :by H))
  (qed <a>))

(definition subset-def
  "The subset relation for type `T`.

The expression `(subset T s1 s2)` means that
 the set `s1` is a subset of `s2`, i.e. `s1`⊆`s2`."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (forall [x T]
    (==> (elem x s1)
         (elem x s2))))

(defimplicit subset
  "Set `s1` is a subset of `s2`, cf. [[subset-def]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'subset-def T s1 s2)))

(defthm subset-refl-thm
  "The subset relation is reflexive."
  [[T :type] [s (set T)]]
  (subset s s))

(proof 'subset-refl-thm :script
  (assume [x T
           H (elem x s)]
    (have <a> (elem x s) :by H))
  (qed <a>))

(defimplicit subset-refl
  "The subset relation is reflexive, cf. [[subset-refl-thm]]."
  [def-env ctx [s s-ty]]
  (let [T (fetch-set-type def-env ctx s-ty)]
    (list #'subset-refl-thm T s)))

(defthm subset-trans-thm
  "The subset relation is transitive."
  [[T :type] [s1 (set T)] [s2 (set T)] [s3 (set T)]]
  (==> (subset s1 s2)
       (subset s2 s3)
       (subset s1 s3)))

(proof 'subset-trans-thm :script
  (assume [H1 (subset s1 s2)
           H2 (subset s2 s3)]
    (assume [x T]
      (have <a> (==> (elem x s1)
                     (elem x s2)) :by (H1 x))
      (have <b> (==> (elem x s2)
                     (elem x s3)) :by (H2 x))
      (have <c> (==> (elem x s1)
                     (elem x s3)) :by (p/impl-trans <a> <b>))))
  (qed <c>))

(defimplicit subset-trans
  "The subset relation is transitive, cf. [[subset-trans-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty] [s3 s3-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'subset-trans-thm T s1 s2 s3)))

(defthm subset-prop
  "Preservation of properties on subsets."
  [[T :type] [P (==> T :type)][s1 (set T)] [s2 (set T)]]
  (==> (forall [x T]
         (==> (elem x s2)
              (P x)))
       (subset s1 s2)
       (forall [x T]
         (==> (elem x s1)
              (P x)))))

(proof 'subset-prop
    :script
  (assume [H1 (forall [x T]
                      (==> (elem x s2)
                           (P x)))
           H2 (subset s1 s2)]
    (assume [x T
             Hx (elem x s1)]
      (have <a> (elem x s2) :by (H2 x Hx))
      (have <b> (P x) :by (H1 x <a>))))
  (qed <b>))

(defthm subset-emptyset-lower-bound
  "The emptyset is a subset of every other sets."
  [[T :type] [s (set T)]]
  (subset (emptyset T) s))

(proof 'subset-emptyset-lower-bound
    :script
  (assume [x T
           Hx (elem x (emptyset T))]
    (have <a> p/absurd :by Hx)
    (have <b> (elem x s) :by ((p/ex-falso (elem x s)) <a>)))
  (qed <b>))

(defthm subset-fullset-upper-bound
  "The fullset is a superset of every other sets."
  [[T :type] [s (set T)]]
  (subset s (fullset T)))

(proof 'subset-fullset-upper-bound
    :script
  (assume [x T
           Hx (elem x s)]
    (have <a> (elem x (fullset T))
          :by p/truth-is-true))
  (qed <a>))

(definition seteq-def
  "Equality on sets.

This is a natural equality on sets based on the subset relation."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (and (subset s1 s2)
       (subset s2 s1)))

(defimplicit seteq
  "Set `s1` is equal to `s2`, cf. [[seteq-def]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'seteq-def T s1 s2)))

(defthm seteq-refl-thm
  "Set equality is reflexive."
  [[T :type] [s (set T)]]
  (seteq s s))

(proof 'seteq-refl-thm :script
  (have <a> (subset s s) :by (subset-refl s))
  (have <b> (and (subset s s)
                 (subset s s))
        :by (p/and-intro <a> <a>))
  (qed <b>))

(defimplicit seteq-refl
  "Set `s` is equal to itself, cf. [[seteq-refl-thm]]."
  [def-env ctx [s s-ty]]
  (let [T (fetch-set-type def-env ctx s-ty)]
    (list #'seteq-refl-thm T s)))

(defthm seteq-sym-thm
  "Set equality is symmetric."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (==> (seteq s1 s2)
       (seteq s2 s1)))

(proof 'seteq-sym-thm :script
  (assume [H (seteq s1 s2)]
    (have <a> (subset s1 s2) :by (p/and-elim-left H))
    (have <b> (subset s2 s1) :by (p/and-elim-right H))
    (have <c> (seteq s2 s1) :by (p/and-intro <b> <a>)))
  (qed <c>))

(defimplicit seteq-sym
  "Set equality is symmetric, cf. [[seteq-sym-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'seteq-sym-thm T s1 s2)))

(defthm seteq-trans-thm
  "Set equality is transitive."
  [[T :type] [s1 (set T)] [s2 (set T)] [s3 (set T)]]
  (==> (seteq s1 s2)
       (seteq s2 s3)
       (seteq s1 s3)))

(proof 'seteq-trans-thm :script
  (assume [H1 (seteq s1 s2)
           H2 (seteq s2 s3)]
    (have <a1> (subset s1 s2) :by (p/and-elim-left H1))
    (have <b1> (subset s2 s3) :by (p/and-elim-left H2))
    (have <c1> (subset s1 s3) :by ((subset-trans s1 s2 s3) <a1> <b1>))
    (have <a2> (subset s2 s1) :by (p/and-elim-right H1))
    (have <b2> (subset s3 s2) :by (p/and-elim-right H2))
    (have <c2> (subset s3 s1) :by ((subset-trans s3 s2 s1) <b2> <a2>))
    (have <d> (seteq s1 s3) :by (p/and-intro <c1> <c2>)))
  (qed <d>))

(defimplicit seteq-trans
  "Set equality is transitive, cf. [[seteq-trans-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty] [s3 s3-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'seteq-trans-thm T s1 s2 s3)))

(definition set-equality
  "A *Leibniz*-style equality for sets.

It says that two sets `s1` and `s2` are equal iff for 
any predicate `P` then `(P s1)` if and only if `(P s2)`.

Note that the identification with [[seteq]] is non-trivial,
 and requires an axiom."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (forall [P (==> (set T) :type)]
          (<=> (P s1) (P s2))))

(defimplicit set-equal
  "The *Leibniz*-style equality for sets, cf. [[set-equality]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'set-equality T s1 s2)))

(defthm set-equal-prop
  [[T :type] [s1 (set T)] [s2 (set T)] [P (==> (set T) :type)]]
  (==> (set-equal s1 s2)
       (P s1)
       (P s2)))

(proof 'set-equal-prop
    :script
  (assume [Heq (set-equal s1 s2)
           Hs1 (P s1)]
    (have <a> (<=> (P s1) (P s2))
          :by (Heq P))
    (have <b> (==> (P s1) (P s2))
          :by (p/and-elim-left <a>))
    (have <c> (P s2) :by (<b> Hs1)))
  (qed <c>))

(defthm set-equal-refl-thm
  "Reflexivity of set equality."
  [[T :type] [s (set T)]]
  (set-equal s s))

(proof 'set-equal-refl-thm :script
  (assume [P (==> (set T) :type)]
    (have a (<=> (P s) (P s))
          :by (p/iff-refl (P s))))
  (qed a))

(defimplicit set-equal-refl
  "Reflexivity of set equality, cf. [[set-equal-refl-thm]]."
  [def-env ctx [s s-ty]]
  (let [T (fetch-set-type def-env ctx s-ty)]
    (list #'set-equal-refl-thm T s)))

(defthm set-equal-sym-thm
  "Symmetry of set equality."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (==> (set-equal s1 s2)
       (set-equal s2 s1)))

(proof 'set-equal-sym-thm :script
  (assume [H (set-equal s1 s2)
           Q (==> (set T) :type)]
    (have <a> (<=> (Q s1) (Q s2)) :by (H Q))
    (have <b> (<=> (Q s2) (Q s1)) :by (p/iff-sym <a>)))
  (qed <b>))

(defimplicit set-equal-sym
  "Symmetry of set equality, cf. [[set-equal-sym-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'set-equal-sym-thm T s1 s2)))

(defthm set-equal-trans-thm
  "Transitivity of set equality."
  [[T :type] [s1 (set T)] [s2 (set T)] [s3 (set T)]]
  (==> (set-equal s1 s2)
       (set-equal s2 s3)
       (set-equal s1 s3)))

(proof 'set-equal-trans-thm :script
  (assume [H1 (set-equal s1 s2)
           H2 (set-equal s2 s3)
           Q (==> (set T) :type)]
    (have <a> (<=> (Q s1) (Q s2)) :by (H1 Q))
    (have <b> (<=> (Q s2) (Q s3)) :by (H2 Q))
    (have <c> (<=> (Q s1) (Q s3))
          :by (p/iff-trans <a> <b>)))
  (qed <c>))

(defimplicit set-equal-trans
  "Transitivity of set equality, cf. [[set-equal-trans-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty] [s3 s3-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'set-equal-trans-thm T s1 s2 s3)))

(defthm set-equal-implies-subset
  "Going from *Leibniz* equality on sets to the subset relation is easy."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (==> (set-equal s1 s2)
       (subset s1 s2)))

(proof 'set-equal-implies-subset :script
  (assume [H (set-equal s1 s2)
           x T]
    (pose Qx := (lambda [s (set T)]
                        (elem x s)))
    (have <a> (<=> (elem x s1) (elem x s2))
          :by (H Qx))
    (have <b> (==> (elem x s1) (elem x s2))
          :by (p/iff-elim-if <a>)))
  (qed <b>))

(defthm set-equal-implies-seteq
  "Subset-based equality implies *Leibniz*-style equality on sets."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (==> (set-equal s1 s2)
       (seteq s1 s2)))

(proof 'set-equal-implies-seteq :script
  (assume [H (set-equal s1 s2)]
    ;; "First we get s1⊆s2."
    (have <a> (subset s1 s2) :by ((set-equal-implies-subset T s1 s2) H))
    ;; "Then we get s2⊆s1."
    (have <b1> (set-equal s2 s1) :by ((set-equal-sym s1 s2) H))
    (have <b> (subset s2 s1) :by ((set-equal-implies-subset T s2 s1) <b1>))
    ;; "... and we can now conclude"
    (have <c> (seteq s1 s2) :by (p/and-intro <a> <b>)))
  (qed <c>))

(defaxiom seteq-implies-set-equal-ax
  "Going from subset-based equality to *Leibniz*-style equality
requires this axiom. This is because we cannot lift membership
 to an arbitrary predicate."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (==> (seteq s1 s2)
       (set-equal s1 s2)))

(defthm set-equal-seteq
  "Set equality and subset-based equality (should) coincide (axiomatically)."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (<=> (seteq s1 s2)
       (set-equal s1 s2)))

(proof 'set-equal-seteq :script
  (have <a> (==> (seteq s1 s2)
                 (set-equal s1 s2)) :by (seteq-implies-set-equal-ax T s1 s2))
  (have <b> (==> (set-equal s1 s2)
                 (seteq s1 s2)) :by (set-equal-implies-seteq T s1 s2))
  (qed (p/iff-intro <a> <b>)))

(definition psubset-def
  "The anti-reflexive variant of the subset relation.

The expression `(psubset T s1 s2)` means that
 the set `s1` is a subset of `s2`, but that the two
sets are distinct, i.e. `s1`⊂`s2` (or more explicitely `s1`⊊`s2`)."
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (and (subset s1 s2)
       (not (seteq s1 s2))))

(defimplicit psubset
  "`s1` is a propre subset of `s2`, cf. [[psubset-def]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'psubset-def T s1 s2)))

(defthm psubset-antirefl
  [[T :type] [s (set T)]]
  (not (psubset s s)))

(proof 'psubset-antirefl
    :script
  (assume [H (psubset s s)]
    (have <a> (not (seteq s s))
          :by (p/and-elim-right H))
    (have <b> (seteq s s) :by (seteq-refl s))
    (have <c> p/absurd :by (<a> <b>)))
  (qed <c>))

(defthm psubset-antisym
  [[T :type] [s1 (set T)] [s2 (set T)]]
  (not (and (psubset s1 s2)
            (psubset s2 s1))))

(proof 'psubset-antisym
    :script
  (assume [H (and (psubset s1 s2)
                  (psubset s2 s1))]
    (have <a> (not (seteq s1 s2))
          :by (p/and-elim-right (p/and-elim-left H)))
    (have <b> (subset s1 s2)
          :by (p/and-elim-left (p/and-elim-left H)))
    (have <c> (subset s2 s1)
          :by (p/and-elim-left (p/and-elim-right H)))
    (have <d> (seteq s1 s2)
          :by (p/and-intro <b> <c>))
    (have <e> p/absurd :by (<a> <d>)))
  (qed <e>))

(defthm psubset-trans-thm
  "The proper subset relation is transitive."
  [[T :type] [s1 (set T)] [s2 (set T)] [s3 (set T)]]
  (==> (psubset s1 s2)
       (psubset s2 s3)
       (psubset s1 s3)))

(proof 'psubset-trans-thm :script
  (assume [H1 (psubset s1 s2)
           H2 (psubset s2 s3)]
    (have <a> (subset s1 s3)
          :by ((subset-trans s1 s2 s3)
               (p/and-elim-left H1)
               (p/and-elim-left H2)))
    (assume [H (seteq s1 s3)]
      (have <b> (set-equal s1 s3)
            :by ((seteq-implies-set-equal-ax T s1 s3)
                 H))
      (have <c> (psubset s3 s2)
            :by ((set-equal-prop T s1 s3 (lambda [x (set T)]
                                           (psubset x s2)))
                 <b> H1))
      (have <d> p/absurd
            :by ((psubset-antisym T s2 s3)
                 (p/and-intro H2 <c>))))
    (have <e> _ :by (p/and-intro <a> <d>)))
  (qed <e>))

(defimplicit psubset-trans
  "The subset relation is transitive, cf. [[psubset-trans-thm]]."
  [def-env ctx [s1 s1-ty] [s2 s2-ty] [s3 s3-ty]]
  (let [T (fetch-set-type def-env ctx s1-ty)]
    (list #'psubset-trans-thm T s1 s2 s3)))

(defthm psubset-emptyset
  [[T :type] [s (set T)]]
  (==> (psubset (emptyset T) s)
       (not (seteq s (emptyset T)))))

(proof 'psubset-emptyset
    :script
  (assume [H (psubset (emptyset T) s)]
    (assume [H' (seteq s (emptyset T))]
      (have <a> (not (seteq (emptyset T) s))
            :by (p/and-elim-right H))
      (have <b> (seteq (emptyset T) s)
            :by ((seteq-sym s (emptyset T)) H'))
      (have <c> p/absurd :by (<a> <b>))))
  (qed <c>))

(defthm psubset-emptyset-conv
  [[T :type] [s (set T)]]
  (==> (not (seteq s (emptyset T)))
       (psubset (emptyset T) s)))

(proof 'psubset-emptyset-conv
    :script
  (assume [H (not (seteq s (emptyset T)))]
    (have <a> (subset (emptyset T) s)
          :by (subset-emptyset-lower-bound T s))
    (assume [H' (seteq (emptyset T) s)]
      (have <b> (seteq s (emptyset T))
            :by ((seteq-sym (emptyset T) s) H'))
      (have <c> p/absurd :by (H <b>)))
    (have <d> (psubset (emptyset T) s)
          :by (p/and-intro <a> <c>)))
  (qed <d>))

(defthm psubset-emptyset-equiv
  [[T :type] [s (set T)]]
  (<=> (psubset (emptyset T) s)
       (not (seteq s (emptyset T)))))

(proof 'psubset-emptyset-equiv
    :script
  (have <a> _ :by (p/and-intro (psubset-emptyset T s)
                               (psubset-emptyset-conv T s)))
  (qed <a>))



