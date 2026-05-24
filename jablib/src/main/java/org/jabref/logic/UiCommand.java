package org.jabref.logic;

import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface UiCommand {
    record BlankWorkspace() implements UiCommand {
    }

    record SelectEntryKeys(List<String> citationKey) implements UiCommand {
    }

    record OpenLibraries(List<Path> toImport) implements UiCommand {
    }

    /// Supports both BibTeX and non-BibTeX
    record AppendFilesToCurrentLibrary(List<Path> toAppend) implements UiCommand {
    }

    /// Supports both BibTeX and non-BibTeX
    record AppendStreamToCurrentLibrary(Reader toAppend) implements UiCommand {
    }

    /// "Twin" to [#AppendToCurrentLibrary]. Accepts BibTeX as text instead stored in a file.
    ///
    /// @param targetGroup name of a group the imported entries are additionally assigned to.
    ///                                                           If the group does not exist, it is created as a top-level group.
    ///                                                           An empty Optional means no group assignment.
    record AppendBibTeXToCurrentLibrary(String bibtex, Optional<String> targetGroup) implements UiCommand {
        public AppendBibTeXToCurrentLibrary(String bibtex) {
            this(bibtex, Optional.empty());
        }

        public AppendBibTeXToCurrentLibrary(String bibtex, String targetGroup) {
            this(bibtex, targetGroup.isBlank() ? Optional.empty() : Optional.of(targetGroup));
        }
    }

    record Focus() implements UiCommand {
    }

    /// @deprecated used by the 2025 browser extension only
    @Deprecated
    record AppendFileOrUrlToCurrentLibrary(String location) implements UiCommand {
    }
}
