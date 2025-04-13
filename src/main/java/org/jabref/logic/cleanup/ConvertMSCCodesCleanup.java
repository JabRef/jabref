package org.jabref.logic.cleanup;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.BibEntryPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jabref.logic.msc.MscCodeUtils;

public class ConvertMSCCodesCleanup implements CleanupJob {
    /*
     * Converts MSC codes found in keywords editor to their descriptions
     */
    
    private static final Logger logger = LoggerFactory.getLogger(ConvertMSCCodesCleanup.class);
    private static final Map<String, String> mscmap;
    private final Character keywordSeparator;
    private static boolean conversionPossible;

    static {
        Map<String, String> tempMap = new HashMap<>();
        URL resourceUrl = ConvertMSCCodesCleanup.class.getClassLoader().getResource("msc_codes.json");

        if (resourceUrl != null) {
            try {
                tempMap = MscCodeUtils.loadMscCodesFromJson(resourceUrl);
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
        mscmap = tempMap;
    }

    public ConvertMSCCodesCleanup(BibEntryPreferences preferences) {
        this.keywordSeparator = preferences.getKeywordSeparator();
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
            logger.warn("MSC code conversion was attempted but is not possible because the codes file could not be loaded");
            return new ArrayList<>();
        }

        List<FieldChange> changes = new ArrayList<>();
        
        // Get keywords from the entry
        if (!entry.hasField(StandardField.KEYWORDS)) {
            logger.debug("No keywords field found in entry {}", entry.getCitationKey().orElse("<no citation key>"));
            return changes;
        }

        String keywordsStr = entry.getField(StandardField.KEYWORDS).orElse("");
        if (keywordsStr.trim().isEmpty()) {
            logger.debug("Keywords field is empty in entry {}", entry.getCitationKey().orElse("<no citation key>"));
            return changes;
        }

        logger.debug("Processing keywords: {}", keywordsStr);
        // Instead of using getKeywords which may split on delimiters within descriptions,
        // we'll get the raw field and do our own careful splitting
        String[] rawKeywords = keywordsStr.split("\\s*" + keywordSeparator + "\\s*");
        logger.debug("Found {} keywords to process", rawKeywords.length);
        
        // Create new list to store converted keywords
        List<Keyword> newKeywords = new ArrayList<>();
        boolean hasChanges = false;

        // Check each keyword against the MSC map
        for (String keywordStr : rawKeywords) {
            keywordStr = keywordStr.trim();
            logger.debug("Processing keyword: {}", keywordStr);
            if (mscmap.containsKey(keywordStr)) {
                String description = mscmap.get(keywordStr);
                logger.debug("Found match for {}: {}", keywordStr, description);
                // If we find a match, use the description instead
                newKeywords.add(new Keyword(description));
                hasChanges = true;
            } else {
                logger.debug("No match found for keyword: {}", keywordStr);
                // Keep original keyword if no match
                newKeywords.add(new Keyword(keywordStr));
            }
        }

        // If we made any changes, record them
        if (hasChanges) {
            String oldValue = keywordsStr;
            String newValue = KeywordList.serialize(newKeywords, keywordSeparator);
            logger.debug("Updating keywords from '{}' to '{}'", oldValue, newValue);
            entry.setField(StandardField.KEYWORDS, newValue);
            changes.add(new FieldChange(entry, StandardField.KEYWORDS, oldValue, newValue));
        } else {
            logger.debug("No MSC codes were converted in this entry");
        }

        return changes;
    }
}
