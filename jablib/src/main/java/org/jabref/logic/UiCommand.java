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

    /// Supports both BibTeX and non-BibTeX.
    ///
    /// @param library the target library; an empty Optional means the currently active library.
    record AppendFilesToLibrary(Optional<Path> library, List<Path> toAppend) implements UiCommand {
        public AppendFilesToLibrary(List<Path> toAppend) {
            this(Optional.empty(), toAppend);
        }
    }

    /// Supports both BibTeX and non-BibTeX
    record AppendStreamToCurrentLibrary(Reader toAppend) implements UiCommand {
    }

    /// "Twin" to [#AppendFilesToLibrary]. Accepts BibTeX as text instead stored in a file.
    ///
    /// @param library the target library; an empty Optional means the currently active library.
    /// @param targetGroup name of a group the imported entries are additionally assigned to. If the group does not exist, it is created as a top-level group. An empty Optional means no group assignment.
    record AppendBibTeXToLibrary(Optional<Path> library, String bibtex, Optional<String> targetGroup) implements UiCommand {
        public AppendBibTeXToLibrary(String bibtex) {
            this(Optional.empty(), bibtex, Optional.empty());
        }

        public AppendBibTeXToLibrary(String bibtex, String targetGroup) {
            this(Optional.empty(), bibtex, toGroup(targetGroup));
        }

        public AppendBibTeXToLibrary(Optional<Path> library, String bibtex, String targetGroup) {
            this(library, bibtex, toGroup(targetGroup));
        }

        private static Optional<String> toGroup(String targetGroup) {
            return targetGroup == null || targetGroup.isBlank() ? Optional.empty() : Optional.of(targetGroup);
        }
    }

    record Focus() implements UiCommand {
    }

    /// @deprecated used by the 2025 browser extension only
    @Deprecated
    record AppendFileOrUrlToCurrentLibrary(String location) implements UiCommand {
    }
}
