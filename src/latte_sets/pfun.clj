(ns latte-sets.pfun
  "Partial functions are defined in this namespace as
  a relation (in the type theoretic sense) of type
  `(==> T U :type)` together with a domain `dom` of type `(set T)`
  and a codomain `cod` of type `(set U)`
   such that for any element of the domain there is a unique
  image in the codomain.

  **Remark**: in type theory, it is best to rely on total functions because
  these are 'native' through the function type `==>`. 
  Partial functions are encoded and thus more complex and less 'natural', use with care."

  (:refer-clojure :exclude [and or not set])

  (:require [latte.core :as latte :refer [definition defthm defaxiom defnotation
                                          defimplicit
                                          forall lambda
                                          assume have pose proof qed lambda]]
            [latte-prelude.quant :as q :refer [exists]]
            [latte-prelude.prop :as p :refer [<=> and and* or not]]
            [latte-prelude.equal :as eq :refer [equal]]
            [latte-prelude.fun :as fun]

            [latte-sets.core :as s :refer [set set-of elem set-equal]]
            [latte-sets.quant :as sq :refer [exists-in forall-in]]
            [latte-sets.algebra :as sa]
            [latte-sets.rel :as rel :refer [rel dom ran]]))

(definition pfun
  "A partial function `f` based on a relation together with
a restricted domain set `from`. Note that the relation `f` on
outside `from` or `to` need not be a function."
  [[?T ?U :type], f (rel T U), from (set T)]
  (forall-in [x from]
    (forall [y1 y2 U]
      (==> (f x y1)
           (f x y2)
           (equal y1 y2)))))

(defn fetch-pfun-type [def-env ctx t]
  (latte.utils/decomposer
   (fn [t]
     (if (clojure.core/and (seq? t)
                           (= (count t) 4)
                           (= (first t) #'latte-sets.pfun/pfun-def))
       (into [] (rest t))
       (throw (ex-info "Not a partial function type." {:type t}))))
   def-env ctx t))

(defthm ridentity-pfun
  "The identity relation is a partial function on
any domain set."
  [T :type]
  (forall [from (set T)]
    (pfun (rel/identity T) from)))

(proof 'ridentity-pfun
  (pose rid := (rel/identity T))
  (assume [from (set T)
           x T Hx (elem x from)
           y1 T y2 T
           Hid1 (rid x y1)
           Hid2 (rid x y2)]
    (have <a> (equal y1 y2) :by (eq/eq-trans (eq/eq-sym Hid1) Hid2)))
  (qed <a>))

(definition pfun-fun
  "The \"partial\" function of a (total) type-theoretic function `f` on its whole domain"
  [[?T ?U :type] [f (==> T U)]]
  (lambda [x T]
    (lambda [y U]
      (equal (f x) y))))

(defthm pfun-fun-prop
  "A type-theoretic function `f` is a partial function for any domain
restriction."
  [[?T ?U :type], f (==> T U)]
  (forall [from (set T)]
    (pfun (pfun-fun f) from)))

(proof 'pfun-fun-prop-thm
  (pose pf := (pfun-fun f))
  (assume [from (set T)
           x T Hx (elem x from)
           y1 U y2 U
           Hy1 (pf x y1)
           Hy2 (pf x y2)]
    (have <a> (equal y1 y2) :by (eq/eq-trans (eq/eq-sym Hy1) Hy2)))
  (qed <a>))

(definition pdom
  "The actual domain of partial function `f`, taking antecedents in `from`."
  [[?T ?U :type], f (rel T U), from (set T)]
  (set-of [x T] (and (elem x from)
                     (exists [y U] (f x y)))))

(definition pran
  "The range of partial function `f``, taking antecedents in `from`."
  [[?T ?U :type], f (rel T U), from (set T)]
  (set-of [y U] (exists-in [x from] (f x y))))

(definition ptotal
  "The partial function `f` is total wrt. the provided `from` domain set."
  [[?T ?U :type], f (rel T U), from (set T)]
  (set-equal (pdom f from) from))

(defthm ptotal-domain
  [[?T ?U :type], f (rel T U), from (set T)]
  (==> (ptotal f from)
       (forall-in [x from]
         (exists [y U]
           (f x y)))))

(proof 'ptotal-domain-thm
  (assume [Htot (ptotal f from)]
    (assume [x T
             Hx (elem x from)]
      (have <a> (set-equal from (pdom f from))
            :by ((s/set-equal-sym (pdom f from) from) Htot))
      (have <b> (s/seteq from (pdom f from))
            :by ((s/set-equal-implies-seteq from (pdom f from)) <a>))
      (have <c> (elem x (pdom f from))
            :by (((p/and-elim-left <b>) x) Hx))
      (have <d> (exists [y U] (f x y))
            :by (p/and-elim-right <c>))))
  (qed <d>))

(defthm pfun-fun-total
  "A type-theoretic function `f` is always total."
  [[?T ?U :type], f (==> T U)]
  (forall [from (set T)]
    (ptotal (pfun-fun f) from)))

(proof 'pfun-fun-total-thm
  (pose rf := (pfun-fun f))
  (assume [from (set T)]
    "subset (if) part"
    (assume [x T
             Hx (elem x (pdom rf from))]
      (have <a> (elem x from) :by (p/and-elim-left Hx)))
    "cosubset (only if) part"
    (assume [x T
             Hx (elem x from)]
      (have <b1> (rf x (f x)) :by (eq/eq-refl (f x)))
      (have <b2> (exists [y U] (rf x y))
            :by ((q/ex-intro (lambda [y U] (rf x y)) (f x)) <b1>))
      (have <b> (elem x (pdom rf from)) :by (p/and-intro Hx <b2>)))
    "(sub)set equality"
    (have <c> (s/seteq (pdom rf from) from) :by (p/and-intro <a> <b>))
    "to set equality"
    (have <d> (set-equal (pdom rf from) from)
          :by ((s/seteq-implies-set-equal (pdom rf from) from)
               <c>)))
  (qed <d>))

(definition pcompose
  [[?T ?U ?V :type], f (rel U V), ffrom (set U), g (rel T U), gfrom (set T)]
  (lambda [x T]
    (lambda [z V]
      (==> (elem x gfrom)
           (exists [y U]
             (and* (elem y ffrom)
                   (g x y)
                   (f y z)))))))

(defthm pcompose-pfun
  "The composition of two partial functions `f` and `g`."
  [[?T ?U ?V :type], f (rel U V), ffrom (set U), fto (set V), g (rel T U), gfrom (set T), gto (set U)]
  (==> (pfun f ffrom fto)
       (pfun g gfrom gto)
       (pfun (pcompose f ffrom fto g gfrom gto) gfrom fto)))

(proof 'pcompose-pfun-thm
  (pose R := (pcompose f ffrom fto g gfrom gto))
  (assume [Hf (pfun f ffrom fto)
           Hg (pfun g gfrom gto)]
    (assume [x T Hx (elem x gfrom)
             y1 V Hy1 (elem y1 fto)
             y2 V Hy2 (elem y2 fto)
             H1 (R x y1)
             H2 (R x y2)]
      (have <a> (exists [z U]
                  (and* (elem z gto)
                        (elem y1 ffrom)
                        (g x y1)
                        (f y1 z))) :by (H1 Hx Hy1))
      )))

(definition pinjective
  "An injective partial function."
  [[?T ?U :type] [f (rel T U)] [from (set T)] [to (set U)]]
  (forall-in [x1 from]
    (forall-in [x2 from]
      (forall-in [y1 to]
        (forall-in [y2 to]
          (==> (f x1 y1)
               (f x2 y2)
               (equal y1 y2)
               (equal x1 x2)))))))

(definition psurjective
  "A surjective partial function."
  [[?T ?U :type] [f (rel T U)] [from (set T)] [to (set U)]]
  (forall-in [y to]
    (exists-in [x from]
      (f x y))))

(definition pbijective
  "A bijective partial function."
  [[?T ?U :type] [f (rel T U)] [from (set T)] [to (set U)]]
  (and (pinjective f from to)
       (psurjective f from to)))

(comment


(defthm pinjective-single
  [[?T ?U :type] [f (rel T U)] [from (set T)] [to (set U)]]
  (==> (pfun f from to)
       (pinjective f from to)
       (forall-in [z to]
         (sq/single-in from (lambda [x T] (forall-in [w to] 
                                            (==> (f x w)
                                                 (equal w z))))))))

(proof 'pinjective-single-thm
  (assume [Hfun _
           Hinj _
           z U
           Hz (elem z to)]
    (pose P := (lambda [x T] (forall-in [w to]
                               (==> (f x w)
                                    (equal w z)))))
    (assume [x T Hx (elem x from)
             y T Hy (elem y from)
             HPx (P x)
             HPy (P y)]
      "We have to show that x equals y"
      (assume [Hcontra (not (equal x y))]
        (assume [w U Hw (elem w to)
                 Hfw (f x w)]
          (have <a> (equal w z) :by (HPx w Hw Hfw))
        ))
      )))



)
