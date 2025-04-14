package org.jabref.logic.cleanup;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;

import org.jabref.logic.msc.MscCodeUtils;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesCleanup implements CleanupJob {
    /*
     * Converts MSC codes found in keywords editor to their descriptions
     */
    
    private static final Logger logger = LoggerFactory.getLogger(ConvertMSCCodesCleanup.class);
    private static final Map<String, String> MSCMAP;
    private static final Map<String, String> reverseMSCMAP;
    private static boolean conversionPossible;
    private final Character keywordSeparator;
    
    private final boolean convertToDescriptions;

    static {
        Map<String, String> tempMap = new HashMap<>();
        Map<String, String> tempReverseMap = new HashMap<>();
        URL resourceUrl = ConvertMSCCodesCleanup.class.getClassLoader().getResource("msc_codes.json");

        if (resourceUrl != null) {
            try {
                tempMap = MscCodeUtils.loadMscCodesFromJson(resourceUrl);
                // Create reverse mapping
                tempMap.forEach((code, desc) -> tempReverseMap.put(desc, code));
                logger.debug("Loaded {} MSC codes from file", tempMap.size());
                // Log a few sample entries to verify content
                if (!tempMap.isEmpty()) {
                    tempMap.entrySet().stream().limit(3).forEach(entry ->
                        logger.debug("Sample MSC code: {} -> {}", entry.getKey(), entry.getValue())
                    );
                    conversionPossible = true;
                } else {
                    logger.error("MSC codes file is empty");
                    conversionPossible = false;
                }
            } catch (Exception e) {
                logger.error("Error loading MSC codes: {}", e.getMessage(), e);
                conversionPossible = false;
            }
        } else {
            logger.error("Resource not found: msc_codes.json");
            conversionPossible = false;
        }
        MSCMAP = tempMap;
        reverseMSCMAP = tempReverseMap;
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
            logger.info("MSC code conversion was attempted but is not possible because the codes file could not be loaded");
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
                if (reverseMSCMAP.containsKey(keywordStr)) {
                    String code = reverseMSCMAP.get(keywordStr);
                    newKeywords.add(new Keyword(code));
                    hasChanges = true;
                } else {
                    newKeywords.add(keyword);
                }
            }
        }

        // If we made any changes, record them
        if (hasChanges) {
            String oldValue = keywordsStr;
            String newValue = KeywordList.serialize(newKeywords, keywordSeparator);
            
            // Update the field on the JavaFX thread
            Platform.runLater(() -> {
                entry.setField(StandardField.KEYWORDS, newValue);
                changes.add(new FieldChange(entry, StandardField.KEYWORDS, oldValue, newValue));
            });
            
            // Return the changes immediately, even though they'll be applied asynchronously
            return changes;
        }
        
        return changes;
    }
}
