package org.jabref.logic.formatter.bibtexfields;

import java.net.URL;
import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesFormatter extends Formatter implements LayoutFormatter {
    private static final Logger logger = LoggerFactory.getLogger(ConvertMSCCodesFormatter.class);
    private static final BiMap<String, String> MSCMAP;
    private static boolean conversionPossible;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertMSCCodesFormatter.class);

    static {

        HashBiMap<String, String> tempMap = HashBiMap.create();

        URL resourceUrl = ConvertMSCCodesFormatter.class.getClassLoader().getResource("msc_codes.json");

        // Check for valid mapping of msc codes
        if (resourceUrl == null) {
            logger.error("Resource not found: msc_codes.json");
            conversionPossible = false;
        } else {
            try {
                tempMap = MscCodeUtils.loadMscCodesFromJson(resourceUrl).get();
                if (!tempMap.isEmpty()) {
                    conversionPossible = true;
                }
            } catch (MscCodeLoadingException e) {
                logger.error("Error loading MSC codes:", e);
                conversionPossible = false;
            }
        }

        MSCMAP = tempMap;
    }

    @Override
    public String format(String text) {
        StringBuilder result = new StringBuilder(Objects.requireNonNull(text));

        if (result.isEmpty() || !conversionPossible) {
            return result.toString();
        }
        // text contains comma separated codes
        String[] codeList = result.toString().split(",");
        result = new StringBuilder();
        for (String code : codeList) {
            // check if key in map and add value to result string
            // non-code keyword is present leave as-is
            code = code.trim();
            result.append(MSCMAP.getOrDefault(code, code)).append(",");
        }
        if (result.toString().endsWith(",")) {
            result.deleteCharAt(result.length() - 1);
        }

        return result.toString();
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
