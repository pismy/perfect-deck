# TODO

## Opening hand optimization

### Define the opening hand selection pipeline

The selection pipeline is an ordered list of selection criteria:

* **positive** criteria: conditions to **keep** and opening hand
    * Ex: _keep if I have at least one mana source and 2 elements of my combo_
* **negative** criteria: conditions to **reject** and opening hand
    * Ex: _mulligan if nb lands < 2 or nb lands > 4_

### Sub-pipelines

Ability to define alternate pipelines after one or several mulligans (selection criteria tend to be softer with less cards).

### Compute statistics

At **any stage** of the opening hand selection pipeline, compute stats such as:

* average number of green mana producers
* probability of having 2 or more lands
* average CCM of spells
* mana curve histogram
* number of _relevant_ cards

