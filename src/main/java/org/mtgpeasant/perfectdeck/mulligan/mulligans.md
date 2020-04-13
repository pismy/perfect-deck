# Mulligan rules definition

Here is an example of rules:

```
// ============
// === Matchers
// ============
<B>: [Swamp] | [Lotus Petal] | [Crumbling Vestige]
<R>: [Mountain] | [Lotus Petal] | [Crumbling Vestige] | [Simian Spirit Guide]
<X>: [Swamp] | [Mountain] | [Lotus Petal] | [Crumbling Vestige] | [Simian Spirit Guide]
<+1>: [Lotus Petal] | [Dark Ritual] | [Simian Spirit Guide]
<creature>: [Hand of Emrakul] | [Greater Sandwurm] | [Pathrazer of Ulamog] | [Ulamog's Crusher]
<rea>: [Exhume] | [Animate Dead] | [Reanimate]
<cantrip>: [Gitaxian Probe]

// ======================
// === Hand keeping rules
// ======================
// I can reanimate turn 1
<<turn 1 imp>>: <B> & [Dark Ritual] & [Putrid Imp] & <creature> & <rea>

// I can reanimate turn 2
<<turn 2 imp>>: <B> & <X> & [Putrid Imp] & <creature> & <rea>
<<turn 2 looting>>: <B> & <R> & [Faithless Looting] & <creature> & <rea>

// I can reanimate turn 2 by discarding a creature on 1st turn with a hand of 7 (OTD)
<<turn 2 OTD>>: <B> & <+1> & <creature> & <rea>

// I can reanimate turn 3 by discarding a creature on 1st turn with a hand of 7 (OTD)
<<turn 3 OTD>>: <B> & <X> & <creature> & <rea>

// one element of the combo is missing but I can play Faithless Looting turn 1
<<looting to find last element>>: <R> & [Faithless Looting] & @atleast(2)( <B> <creature> <rea> )

// I miss 1 mana source but I have a gitaxian probe: if I'm OTD, I have 4 draws to find my 2nd mana source
<<probe to find mana source>>: <X> & <cantrip> & <creature> & <rea>
```

This files defines matchers (pretty similar to [Regular expressions](https://en.wikipedia.org/wiki/Regular_expression)).

* an empty line is ignored
* a line starting with `#` is ignored (_comment_)
* a line starting with `<{name}>: {matcher}` declares a **named** matcher (that can be reused in other matchers or hand keeping rules)
* a line starting with `<<{name}>>: {matcher}` is considered to declare a hand keeping rule

Syntactically, a matcher and a rule are exactly the same. The only difference is that the tool will perform simulations
against **hand keeping rules only**. Atomic matchers are just useful internally to simplify complex rules writing.

A `{matcher}` can be of the following types:

* `[{name}]`: basic card matcher; matches if the card is available in hand
* `<{name}>`: references a declared named matcher or rule
* `({matcher 1} & {matcher 2} & ...)`: compound matcher that matches if **all** matchers match
* `({matcher 1} | {matcher 2} | ...)`: compound matcher that matches if **at least one** matcher matches
* `@atleast({nb})({matcher 1} {matcher 2} ...)`: compound matcher that matches if **at least** `{nb}` of the matchers match
* `@otp`: matches only if current game is **on the play**
* `@otd`: matches only if current game is **on the draw**
* `@mulligans({operator})({nb})`: matches only if mulligans taken match the operator (ex: `@mulligans(eq)(0)` or `@mulligans(<)(3)`)


