/// Opens a directory as a library: each Hayagriva `.yml` or Markdown sidecar
/// ([org.jabref.logic.directorylibrary.MarkdownSidecar]) next to a PDF holds one or more entries,
/// and the folder tree is mirrored as groups. Design: <https://devdocs.jabref.org/decisions/0071-directory-as-library-with-hayagriva-sidecars.html>,
/// requirements: <https://devdocs.jabref.org/requirements/directory-library.html>.
///
/// Entry points:
///
/// - [org.jabref.logic.directorylibrary.DirectoryLibraryScanner] builds the
///   [org.jabref.model.database.BibDatabaseContext] from a directory; sidecar-less PDFs become
///   stub entries that [org.jabref.logic.directorylibrary.PdfEnrichmentTask] enriches in the
///   background via [org.jabref.logic.directorylibrary.PdfEntryFactory].
/// - [org.jabref.logic.directorylibrary.DirectoryLibrarySynchronizer] keeps the open library
///   and its files in sync: external file changes flow into the model (inbound), user changes
///   are written back through [org.jabref.logic.directorylibrary.SidecarWriteBack], and
///   [org.jabref.logic.directorylibrary.BibMirror] maintains the library's `.bib` mirror with
///   three-way merge-back. [org.jabref.logic.directorylibrary.DirectoryLibraryCatalog] and
///   [org.jabref.logic.directorylibrary.TrackedFiles] hold the entry-to-file bookkeeping they
///   share; [org.jabref.logic.directorylibrary.PendingWrites] debounces the writes.
/// - [org.jabref.logic.directorylibrary.DirectoryLibraryConverter] turns a regular `.bib`
///   library into a directory library.
///
/// The Hayagriva mapping itself lives in `org.jabref.logic.importer.fileformat` (importer,
/// [org.jabref.logic.importer.fileformat.HayagrivaMapping]) and `org.jabref.logic.exporter`
/// ([org.jabref.logic.exporter.HayagrivaEntryWriter]).
package org.jabref.logic.directorylibrary;
