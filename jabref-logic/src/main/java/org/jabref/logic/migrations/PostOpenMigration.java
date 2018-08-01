package org.jabref.logic.migrations;

import org.jabref.logic.importer.ParserResult;

public interface PostOpenMigration {
    void performMigration(ParserResult parserResult);
}
