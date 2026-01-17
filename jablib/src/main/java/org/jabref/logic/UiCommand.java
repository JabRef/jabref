package org.jabref.logic;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface UiCommand {
    record BlankWorkspace() implements UiCommand {
    }

    record SelectEntryKeys(List<String> citationKey) implements UiCommand {
    }

    record OpenLibraries(List<Path> toImport) implements UiCommand {
    }

    record AppendToCurrentLibrary(List<Path> toAppend) implements UiCommand {
    }

    /// "Twin" to [#AppendToCurrentLibrary]. Accepts BibTeX as text instead stored in a file.
    record AppendBibTeXToCurrentLibrary(String bibtex) implements UiCommand {
    }

    record Focus() implements UiCommand {
    }

    /// @deprecated used by the 2025 browser extension only
    @Deprecated
    record AppendFileOrUrlToCurrentLibrary(String location) implements UiCommand {
    }
}
