package org.jabref.logic.importer.util;

import java.util.List;

import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportResultsMerger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportResultsMerger.class);

    public static ParserResult mergeImportResults(
            List<ImportFormatReader.UnknownFormatImport> imports,
            Character keyWordSeparator,
            OwnerPreferences ownerPreferences,
            TimestampPreferences timestampPreferences) {
        BibDatabase resultDatabase = new BibDatabase();
        ParserResult result = new ParserResult(resultDatabase);

        for (ImportFormatReader.UnknownFormatImport importResult : imports) {
            if (importResult == null) {
                continue;
            }

            ParserResult parserResult = importResult.parserResult();
            if (parserResult.hasWarnings()) {
                LOGGER.warn("Imported library has errors", parserResult.getErrorMessage());
                // TODO: collect errors into ParserResult, because they are currently ignored (see caller of this method)
            }

            resultDatabase.insertEntries(parserResult.getDatabase().getEntries());

            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format())) {
                // additional treatment of BibTeX
                new DatabaseMerger(keyWordSeparator).mergeMetaData(
                        result.getMetaData(),
                        parserResult.getMetaData(),
                        importResult.parserResult().getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                        parserResult.getDatabase().getEntries());
            }
        }

        // set timestamp and owner
        UpdateField.setAutomaticFields(resultDatabase.getEntries(), ownerPreferences, timestampPreferences);

        return result;
    }
}
