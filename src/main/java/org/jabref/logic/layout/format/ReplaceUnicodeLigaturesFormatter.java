package org.jabref.logic.layout.format;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.UnicodeLigaturesMap;
import org.jabref.model.cleanup.Formatter;

public class ReplaceUnicodeLigaturesFormatter implements LayoutFormatter, Formatter {

    private Map<Pattern, String> ligaturesMap;

    public ReplaceUnicodeLigaturesFormatter() {
        ligaturesMap = new HashMap<>();
        UnicodeLigaturesMap stringMap = new UnicodeLigaturesMap();
        for (String key : stringMap.keySet()) {
            ligaturesMap.put(Pattern.compile(key), stringMap.get(key));
        }
    }

    @Override
    public String getName() {
        return Localization.lang("Replace Unicode ligatures");
    }

    @Override
    public String getKey() {
        return "remove_unicode_ligatures";
    }

    @Override
    public String format(String fieldText) {
        String result = fieldText;

        for (Pattern key : ligaturesMap.keySet()) {
            result = key.matcher(result).replaceAll(ligaturesMap.get(key));
        }
        return result;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Replaces Unicode ligatures with their expanded form");
    }

    @Override
    public String getExampleInput() {
        return "Æneas";
    }
}
