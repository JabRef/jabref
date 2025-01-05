---
parent: Code Howtos
---
# JabRef's handling of BibTeX

The main class to handle a single BibTeX entry is `org.jabref.model.entry.BibEntry`.
The content of a `.bib` file is handled in `org.jabref.model.database.BibDatabase`.
Things not written in the `.bib` file, but required for handling are stored in `org.jabref.model.database.BibDatabaseContext`.
For instance, this stores the mode of the library, which can be BibTeX or `biblatex`.

Standard BibTeX fields known to JabRef are modeled in `org.jabref.model.entry.field.StandardField`.
A user-defined field not known to JabRef's code is modelled in `org.jabref.model.entry.field.UnknownField`.
Typically, to get from a String to a `Field`, one needs to use `org.jabref.model.entry.field.FieldFactory#parseField(java.lang.String)`.

## Cross-references

BibTeX allows for referencing other entries by the field `crossref` (`org.jabref.model.entry.field.StandardField#CROSSREF`).
Note that BibTeX and `biblatex` handle this differently.
The method `org.jabref.model.entry.BibEntry#getResolvedFieldOrAlias(org.jabref.model.entry.field.Field, org.jabref.model.database.BibDatabase)` handles this difference.
