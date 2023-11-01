package org.jabref.logic;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

public sealed interface UiCommand {
    record JumpToEntryKey(ParserResult parserResult, BibEntry bibEntry) implements UiCommand { }

    record OpenDatabases(List<ParserResult> parserResults) implements UiCommand { }

    record OpenDatabaseFromPath(Path path) implements UiCommand { }
}
