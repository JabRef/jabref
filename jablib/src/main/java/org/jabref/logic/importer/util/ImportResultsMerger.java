package org.jabref.logic.importer.util;

import java.util.List;

import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;

public class ImportResultsMerger {

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
            resultDatabase.insertEntries(parserResult.getDatabase().getEntries());

            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format())) {
                // additional treatment of BibTeX
                new DatabaseMerger(keyWordSeparator).mergeMetaData(
                        result.getMetaData(),
                        parserResult.getMetaData(),
                        importResult.parserResult().getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                        parserResult.getDatabase().getEntries());
            }
            // TODO: collect errors into ParserResult, because they are currently ignored (see caller of this method)
        }

        // set timestamp and owner
        UpdateField.setAutomaticFields(resultDatabase.getEntries(), ownerPreferences, timestampPreferences);

        return result;
    }
}
