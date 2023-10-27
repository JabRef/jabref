---
nav_order: 28
parent: Decision Records
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->
# Return BibTeX string and CSL Item JSON in the API

## Context and Problem Statement

In the context of an http server, when a http client `GETs` a JSON data structure containing BibTeX data, which format should that have?

## Considered Options

* Offer both, BibTeX string and CSL JSON
* Return BibTeX as is as string
* Convert BibTeX to JSON

## Decision Outcome

Chosen option: "Offer both, BibTeX string and CSL JSON", because there are many browser libraries out there being able to parse BibTeX. Thus, we don't need to convert it.

## Pros and Cons of the Options

### Offer both, BibTeX string and CSL JSON

* Good, because this follows "Backend for Frontend"
* Good, because Word Addin works seamless with the data provided (and does not need another dependency)
* Good, because other clients can work with BibTeX data
* Bad, because two serializations have to be kept

### Return BibTeX as is as string

* Good, because we don't need to think about any conversion
* Bad, because it is unclear how to ship BibTeX data where the entry is dependent on
* Bad, because client needs an additional parsing logic

### Convert BibTeX to JSON

More thought has to be done when converting to JSON.
There seems to be a JSON format from [@citation-js/plugin-bibtex](https://www.npmjs.com/package/@citation-js/plugin-bibtex).
We could do an additional self-made JSON format, but this increases the number of available JSON serializations for BibTeX.

* Good, because it could flatten BibTeX data (example: `author = first # " and " # second`)
* Bad, because conversion is difficult in BibTeX special cases. For instance, if Strings are used (example: `author = first # " and " # second`) and one doesn't want to flatten ("normalize") this.

## More Information

Existing JavaScript BibTeX libraries:

* [bibtex-js](https://github.com/digitalheir/bibtex-js)
* [bibtexParseJS](https://github.com/ORCID/bibtexParseJs)
* [@citation-js/plugin-bibtex](https://www.npmjs.com/package/@citation-js/plugin-bibtex)
