package org.jabref.logic;

import java.nio.file.Path;
import java.util.List;

public sealed interface UiCommand {
    record BlankWorkspace() implements UiCommand { }

    record JumpToEntryKey(String citationKey) implements UiCommand { }

    record OpenLibraries(List<Path> toImport) implements UiCommand { }

    record AppendToCurrentLibrary(List<Path> toAppend) implements UiCommand { }
}
