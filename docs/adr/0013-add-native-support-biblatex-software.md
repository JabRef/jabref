# \[Added Native Support for BibLatex-Sotware\]

* Status: Proposed
* Deciders: Oliver Kopp

Technical Story: [6574-Adding support for biblatex-software](https://github.com/JabRef/jabref/issues/6574)

## Context and Problem Statement

JabRef does not right now have support for Biblatex-Software out of the box , users have to add custome entry type.
With citing software becoming fairly comen , native support would be helpful.


## Decision Drivers

* The new entry types definitions should be added to the Select Entry Pane and be separated by a divider
* None of the existing flows should be impacted

## Considered Options

* Adding the new entry types to the existing biblatex types , but it conflicted with an already existing type(software)
* Add a divider with label Biblatex-Software underwhich the new entries are listed : Native support for Biblatex-Software
* Support via customized entry types : A user can load a customized bib file

## Decision Outcome

Chosen option: Yet to be decided.

### Positive Consequences

* Inbuilt coverage for a entry type that is getting more and more importance

### Negative Consequences

* Adds a little bit more clutter to the Add Entry pane

## Pros and Cons of the Options

### Adding the new entry types to the existing biblatex types

* Good, since ther is no need for a new category in the add entry pane

### Add a divider with label Biblatex-Software with relevant types

* Good, since this gives the user a bit more clarity

### Support via customized entry types

*
