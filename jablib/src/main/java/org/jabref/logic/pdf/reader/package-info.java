/// Command lines that open an external PDF reader at a given page.
///
/// A PDF carries no standard way of being opened at a position, and no standard way of asking for
/// one either. Adobe's PDF Open Parameters (`/A "page=N"`) are a de-facto convention among the
/// Windows readers — Acrobat, Foxit, PDF-XChange and SumatraPDF from 3.5 on — while every Linux
/// reader has an option of its own (`-p`, `-i`, `-P`) and macOS has no command-line route at all.
///
/// [org.jabref.logic.pdf.reader.ExternalPdfReaders] therefore holds an allowlist of the readers
/// whose arguments are known, rather than one generic argument: handing `/A page=5` to a program
/// that does not understand it makes that program treat both tokens as file names.
///
/// Everything here only *assembles* a command line and stays free of preferences, so a caller in
/// `org.jabref.gui` reads the configured application and starts the process. The matching
/// implementation is found through
/// [org.jabref.logic.pdf.reader.ExternalPdfReaders#getReader(java.nio.file.Path)]; page numbers are
/// 1-based throughout, as in [org.jabref.model.pdf.FileAnnotation#getPage].

@NullMarked
package org.jabref.logic.pdf.reader;

import org.jspecify.annotations.NullMarked;
