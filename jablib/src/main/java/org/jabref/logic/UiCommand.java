package org.jabref.logic;

import java.nio.file.Path;
import java.util.List;

public sealed interface UiCommand {
    record BlankWorkspace() implements UiCommand {
    }

    record JumpToEntryKey(String citationKey) implements UiCommand {
    }

    record OpenLibraries(List<Path> toImport) implements UiCommand {
    }

    record AppendToCurrentLibrary(List<Path> toAppend) implements UiCommand {
    }

    record Focus() implements UiCommand {
    }

    /// @deprecated used by the browser extension only
    @Deprecated
    record AppendBibTeXToCurrentLibrary(String bibtex) implements UiCommand {
    }

    /// @deprecated used by the browser extension only
    @Deprecated
    record AppendFileOrUrlToCurrentLibrary(String location) implements UiCommand {
    }
}
