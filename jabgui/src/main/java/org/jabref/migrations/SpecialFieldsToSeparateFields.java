package org.jabref.migrations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

/// Moves special field values (`prio1`, `rank3`, `printed`, ...) that JabRef 5.2 and older stored in the
/// `keywords` field into the special fields themselves.
public class SpecialFieldsToSeparateFields implements PostOpenMigration {
    private final KeywordList possibleKeywordsToMigrate;
    private final Character keywordDelimiter;
    private final Map<String, SpecialField> migrationTable = getMigrationTable();

    public SpecialFieldsToSeparateFields(Character keywordDelimiter) {
        List<SpecialFieldValue> specialFieldValues = Arrays.asList(SpecialFieldValue.values());
        possibleKeywordsToMigrate = new KeywordList(specialFieldValues.stream()
                                                                      .map(SpecialFieldValue::getKeyword)
                                                                      .filter(Optional::isPresent)
                                                                      .map(Optional::get)
                                                                      .collect(Collectors.toList()));
        this.keywordDelimiter = keywordDelimiter;
    }

    @Override
    public boolean isMigrationNecessary(ParserResult parserResult) {
        return parserResult.getDatabase().getEntries().stream().anyMatch(this::hasKeywordToMigrate);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Special fields (ranking, priority, read status, ...) are stored in the field 'keywords' (JabRef 5.2 and older). Move them to their own fields.");
    }

    @Override
    public void performMigration(ParserResult parserResult) {
        parserResult.getDatabase().getEntries().forEach(this::migrateEntry);
    }

    private boolean hasKeywordToMigrate(BibEntry entry) {
        KeywordList keywords = entry.getKeywords(keywordDelimiter);
        return possibleKeywordsToMigrate.stream().anyMatch(keywords::contains);
    }

    private void migrateEntry(BibEntry entry) {
        for (Keyword keyword : possibleKeywordsToMigrate) {
            if (entry.getKeywords(keywordDelimiter).contains(keyword) && migrationTable.containsKey(keyword.get())) {
                entry.setField(migrationTable.get(keyword.get()), keyword.get());
            }
        }

        entry.removeKeywords(possibleKeywordsToMigrate, keywordDelimiter);
    }

    /// Mapping of special field values (contained in the keywords) to their corresponding special field
    private Map<String, SpecialField> getMigrationTable() {
        Map<String, SpecialField> map = new HashMap<>();
        map.put("printed", SpecialField.PRINTED);

        map.put("prio1", SpecialField.PRIORITY);
        map.put("prio2", SpecialField.PRIORITY);
        map.put("prio3", SpecialField.PRIORITY);

        map.put("qualityAssured", SpecialField.QUALITY);

        map.put("rank1", SpecialField.RANKING);
        map.put("rank2", SpecialField.RANKING);
        map.put("rank3", SpecialField.RANKING);
        map.put("rank4", SpecialField.RANKING);
        map.put("rank5", SpecialField.RANKING);

        map.put("read", SpecialField.READ_STATUS);
        map.put("skimmed", SpecialField.READ_STATUS);

        map.put("relevant", SpecialField.RELEVANCE);

        return map;
    }
}
