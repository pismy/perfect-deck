# Perfect Hand

[Magic: The Gathering](https://magic.wizards.com) tool for optimizing a deck based on opening hand keeping rules.

## Usage

Clone the repository, install Maven, Java, and run:

```bash
mvn clean package
```

Then start the tool by running:

```bash
java -jar target/perfect-hand-1.0.0-SNAPSHOT.jar
```

Once in the tool shell, you can use the following:

```bash
# global help
shell:>help

# help on stats command
shell:>help mkstats

# exit shell
exit
```

You can test the program with a predefined deck and rules with:

```bash
shell:>mkstats -D src/test/resources/reanimator-deck.txt -R src/test/resources/reanimator-rules.txt -I 5000 -v
```

## Deck format

This tools supports MWS, Apprentice and [Cockatrice](https://github.com/Cockatrice/Cockatrice/wiki/Deck-List-Import-Formats) deck file formats.

Here is an example of a deck file:

```
// discard
4 Faithless Looting
4 Putrid Imp

// reanimation
4 Animate Dead
4 Exhume

// steaks
4 Hand of Emrakul
2 Greater Sandwurm
1 Pathrazer of Ulamog
4 Ulamog's Crusher

// mana
3 Dark Ritual
4 Lotus Petal
1 Crumbling Vestige
7 Mountain
10 Swamp

// others
3 Dragon Breath
3 Gitaxian Probe
2 Shred Memory

// Sideboard:
SB: 3 Apostle's Blessing
SB: 4 Duress
SB: 2 Electrickery
SB: 2 Mind Rake
SB: 2 Pyroblast
SB: 2 Shenanigans
```

Syntax is quite straightforward:

* an empty line is ignored
* a line starting with `#` or `//` is ignored (_comment_)
* a line starting with `SB: ` is considered as a card from sideboard
* a card can be defined as:
    * `{number} {name}` (ex: `3 Swamp`)
    * `{number}x {name}` (ex: `3x Swamp`)
    * `{name}` (ex: `Swamp`); counts only one card

## Rules definition

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


## License

This code is under [Apache-2.0 License](LICENSE.txt)
