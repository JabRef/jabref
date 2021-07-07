# Add Native Support for BibLatex-Software

* Deciders: Oliver Kopp

Technical Story: [6574-Adding support for biblatex-software](https://github.com/JabRef/jabref/issues/6574)

## Context and Problem Statement

Right now, JabRef does not have support for Biblatex-Software out of the box, users have to add custom entry types.
With citing software becoming fairly common, native support is helpful.

## Decision Drivers

* None of the existing flows should be impacted

## Considered Options

* Add the new entry types to the existing biblatex types
* Add a divider with label Biblatex-Software under which the new entries are listed: Native support for Biblatex-Software
* Support via customized entry types: A user can load a customized bib file

## Decision Outcome

Chosen option: "Add a new divider", because comes out best (see below).

### Positive Consequences

* Inbuilt coverage for a entry type that is getting more and more importance

### Negative Consequences

* Adds a little bit more clutter to the Add Entry pane

## Pros and Cons of the Options

### Add the new entry types to the existing biblatex types

* Good, because there is no need for a new category in the add entry pane

### Add a divider with label Biblatex-Software under which the new entries are listed: Native support for Biblatex-Software

* Good, since this gives the user a bit more clarity


### Support via customized entry types: A user can load a customized bib file

* Good, because no code needs to be changed
* Bad, because documentation is needed
* Bad, because the users are not guided through the UI, but have to do other steps.
