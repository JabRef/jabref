package org.jabref.logic.formatter.bibtexfields;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.jgit.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesFormatter extends Formatter implements LayoutFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertMSCCodesFormatter.class);
    private static final BiMap<String, String> MSCMAP;
    private static boolean conversionPossible;
    private static BibEntryPreferences preferences;

    public ConvertMSCCodesFormatter() { }

    public ConvertMSCCodesFormatter(BibEntryPreferences pref) {
        preferences = pref;
    }

    public static void setPreferences(BibEntryPreferences pref) {
        preferences = pref;
    }

    static {
        MSCMAP = initializeMap();
    }

    private static HashBiMap<String, String> initializeMap() {
        HashBiMap<String, String> tempMap = HashBiMap.create();

        URL resourceUrl = ConvertMSCCodesFormatter.class.getClassLoader().getResource("msc_codes.json");

        // Check for valid mapping of msc codes
        if (resourceUrl == null) {
            LOGGER.error("Resource not found: msc_codes.json");
            conversionPossible = false;
            return tempMap;
        } else {
            try {
                tempMap = MscCodeUtils.loadMscCodesFromJson(resourceUrl).get();
                if (!tempMap.isEmpty()) {
                    conversionPossible = true;
                    return tempMap;
                }
            } catch (MscCodeLoadingException e) {
                LOGGER.error("Error loading MSC codes:", e);
                conversionPossible = false;
            }
            return tempMap;
        }
    }

    @NonNull
    @Override
    public String format(String text) {
        if (text.isEmpty() || !conversionPossible) {
            return text;
        }

        // get preferences
        Character dlim = preferences.getKeywordSeparator();
        Character hdlim = Keyword.DEFAULT_HIERARCHICAL_DELIMITER;
//        LOGGER.info("Delimiter = {}", dlim);
//        LOGGER.info("Hierarchical Delimiter = {}", hdlim);

        // create KeywordList to tokenize
        KeywordList keyList = KeywordList.parse(text, dlim, hdlim);
        Iterator<Keyword> list = keyList.iterator();
        List<Keyword> modifiedList = new ArrayList<Keyword>();
        while (list.hasNext()) {
            // check if key in map and add value to result string
            // non-code keyword is present leave as-is
            Keyword item = list.next();
            String code = item.toString().trim(); // remove whitespace
            String convertedText = MSCMAP.getOrDefault(code, code);
//            LOGGER.info("Original: {}\n After Conversion: {}\n", code, convertedText);

            modifiedList.add(new Keyword(convertedText));
        }

        return KeywordList.serialize(modifiedList, dlim);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Convert MSC Keyword codes to their respective descriptions.");
    }

    @Override
    public String getName() {
        return Localization.lang("MSC Codes to Descriptions");
    }

    @Override
    public String getKey() {
        return "MSC_codes_to_descriptions"; }

    @Override
    public String getExampleInput() {
        return "06E30";
    }
}
