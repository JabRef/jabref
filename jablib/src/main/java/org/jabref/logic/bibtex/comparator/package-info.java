/// Comparators and diffs for entries, fields and whole libraries.
///
/// - Sorting: comparators implementing `Comparator<BibEntry>`, for example [org.jabref.logic.bibtex.comparator.EntryComparator].
/// - The class [org.jabref.logic.bibtex.comparator.BibEntryCompare] provides comparison based on the set relation.
/// - A whole library can be compared using [org.jabref.logic.bibtex.comparator.BibDatabaseDiff].
///
/// Fuzzy similarity (duplicate detection) is not here but in [org.jabref.logic.database.DuplicateCheck].
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html#duplicate-finder">Duplicate finder</a>

package org.jabref.logic.bibtex.comparator;
