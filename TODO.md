# TODO

## Mulligans policy optimization

### Define the mulligans policy

The mulligans policy is an ordered list of selection criteria:

* **positive** criteria: conditions to **keep** a hand (ex: _keep if I have at least one mana source and 2 elements of my combo_)
* **negative** criteria: conditions to **reject** a hand (ex: _mulligan if nb lands < 2 or nb lands > 4_)
* ability to **specialize** each criteria to a specific context (ex: _only OTD_ / _taken mulligans so far > 0_)

### Compute statistics

Basic mulligans statistics:

* percentage of hand keep (on opening hand)
* number and percentage of mulligans taken

At **any stage** of the opening hand selection pipeline, ability to compute stats such as:

* average number of green mana producers
* probability of having 2 or more lands
* average CCM of spells
* mana curve histogram
* number of _relevant_ cards


## Goldfish speed optimization

### Develop your own deck goldfish pilot (automaton)

Requires a simple framework.

### Debug

By being able to run goldfish games and follow the events.

### Computes statistics

Run a large number of goldfish games (OTP and/or OTD) and compute statistics:

* mulligans statistics (see above)
* number of wins per turn
* average kill turn + standard derivation
* breakdown:
    * by number of mulligans
    * by OTP/OTD
