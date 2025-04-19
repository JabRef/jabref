package org.jabref.logic.cleanup;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.msc.MscCodeLoadingException;
import org.jabref.logic.msc.MscCodeUtils;
import org.jabref.migrations.MSCMigration;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesCleanup implements CleanupJob {
    /*
     * Converts MSC codes found in keywords editor to their descriptions
     */

    private static final Logger logger = LoggerFactory.getLogger(ConvertMSCCodesCleanup.class);
    private static final BiMap<String, String> MSCMAP;
    private static boolean conversionPossible;
    private static BooleanProperty isEditorOpen;
    private final Character keywordSeparator;
    private final boolean convertToDescriptions;

    static {

        HashBiMap<String, String> tempMap = HashBiMap.create();
        
        URL resourceUrl = ConvertMSCCodesCleanup.class.getClassLoader().getResource("msc_codes.json");

        // Check for valid mapping of msc codes
        if (resourceUrl == null) {
            logger.error(Localization.lang("Resource not found: msc_codes.json"));
            conversionPossible = false;
        } else {
            try {
                tempMap = MscCodeUtils.loadMscCodesFromJson(resourceUrl).get();
                if (!tempMap.isEmpty()) {
                    conversionPossible = true;
                }
            } catch (MscCodeLoadingException e) {
                logger.error(Localization.lang("Error loading MSC codes:", e));
                conversionPossible = false;
            }
        }

        MSCMAP = tempMap;
    }

    public ConvertMSCCodesCleanup(BibEntryPreferences preferences, boolean convertToDescriptions) {
        this.keywordSeparator = preferences.getKeywordSeparator();
        this.convertToDescriptions = convertToDescriptions;
    }

    /**
     * Returns whether MSC code conversion is possible (i.e., if the MSC codes file was loaded successfully)
     */
    public static boolean isConversionPossible() {
        return conversionPossible;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if (!conversionPossible) {
            return new ArrayList<>();
        }

        List<FieldChange> changes = new ArrayList<>();

        if (!entry.hasField(StandardField.KEYWORDS)) {
            return changes;
        }

        String keywordsStr = entry.getField(StandardField.KEYWORDS).orElse("");
        if (keywordsStr.trim().isEmpty()) {
            return changes;
        }

        KeywordList rawKeywords = KeywordList.parse(keywordsStr, keywordSeparator);
        List<Keyword> newKeywords = new ArrayList<>();
        boolean hasChanges = false;

        for (Keyword keyword : rawKeywords) {
            String keywordStr = keyword.get();
            if (convertToDescriptions) {
                // Convert codes to descriptions
                if (MSCMAP.containsKey(keywordStr)) {
                    String description = MSCMAP.get(keywordStr);
                    newKeywords.add(new Keyword(description));
                    hasChanges = true;
                } else {
                    newKeywords.add(keyword);
                }
            } else {
                // Convert descriptions back to codes
                if (MSCMAP.inverse().containsKey(keywordStr)) {
                    String code = MSCMAP.inverse().get(keywordStr);
                    newKeywords.add(new Keyword(code));
                    hasChanges = true;
                } else {
                    newKeywords.add(keyword);
                }
            }
        }

        // If we made any changes, update the entry
        if (hasChanges) {
            String oldValue = keywordsStr;
            String newValue = KeywordList.serialize(newKeywords, keywordSeparator);

            MSCMigration tool = new MSCMigration();
            isEditorOpen = tool.isEditorOpen();

            if (!Platform.isFxApplicationThread() && isEditorOpen.get()) {
                
                // If the thread is not JavaFX and Editor is showing avoid error by adding to queue
                Platform.runLater(() -> {
                    entry.setField(StandardField.KEYWORDS, newValue);
                    changes.add(new FieldChange(entry, StandardField.KEYWORDS, oldValue, newValue));
                });
            } else {
                entry.setField(StandardField.KEYWORDS, newValue);
                changes.add(new FieldChange(entry, StandardField.KEYWORDS, oldValue, newValue));
            }
        }

        return changes;
    }
}
