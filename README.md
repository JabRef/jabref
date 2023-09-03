# Abbreviations
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-3-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

A repository of abbreviations for references, e.g., for journals, conferences, and institutes.

## Journal abbreviations

Currently, a number of journal lists are offered.
Please check the direcotry [`journals`](journals/) for

- A list of abbreviations
- An explaining [`README.md`](journals/README.md) listing the sources of the CSV file. For instance, some lists are generated using Python, some are maintained manually.

## Conference and institute abbreviations

This is future work.

## Format of the CSV files

Since October 2019, the data files are in CSV format (using semicolons as separators):

 ```csv
<full name>;<abbreviation>[;<shortest unique abbreviation>]
```

The abbreviation should follow the ISO4 standard, see <https://marcinwrochna.github.io/abbrevIso/> for details on the abbreviation rules and a search form for title word abbreviations.
The last two fields are optional, and you can safely omit them.
JabRef supports the third field, which contains the "shortest unique abbreviation".
The third field is optional, one can omit it.

For instance both formats are valid

```csv
Accounts of Chemical Research;Acc. Chem. Res.
```

```csv
Accounts of Chemical Research;Acc. Chem. Res.;ACHRE4
```

The list should follow the ISO4 standard with dots.

*If you want to **add a list or submit corrections**, see the [contribution guidelines](CONTRIBUTING.md).*

## Relation to JabRef

JabRef can help you refactor your reference list by automatically abbreviating or unabbreviating journal names.
This requires that you keep one or more lists of journal names and their respective abbreviations.
To set up these lists, choose Options -> Manage journal abbreviations.
See <https://docs.jabref.org/advanced/journalabbreviations> for an extensive documentation.

At each release of JabRef all available journal lists using dots [are combined](https://github.com/JabRef/jabref/blob/main/.github/workflows/refresh-journal-lists.yml) and made available to the users.
In case of duplicate appearances in the journal lists, the last occurring abbreviation is chosen.

## Other projects

### abbrevIso

[`abbrevIso`](https://marcinwrochna.github.io/abbrevIso/) is an online service abbreviation a single journal title by using heuristics.
It takes the official list of ISO4 abbreviations of single words, plus the general rules defined in the ISO4 specifications to deduce the abbreviation for any journal name you input.
However, it does not handle unabbreviation, for which there is no alternative to lists.

Its source is available at <https://github.com/marcinwrochna/abbrevIso> and the API at <https://tools.wmflabs.org/abbreviso/>.
