package org.jabref.logic.formatter.bibtexfields;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ConvertMSCCodesFormatter extends Formatter implements LayoutFormatter {

    private @Nullable JabRefCliPreferences cliPreferences;

    public ConvertMSCCodesFormatter() {
        this.cliPreferences = null;
    }

    ConvertMSCCodesFormatter(JabRefCliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    @NonNull
    @Override
    public String format(@NonNull String text) {
        JabRefCliPreferences preferences = getCliPreferences();
        if (text.isEmpty() || !preferences.shouldEnableMscKeywordDescriptions()) {
            return text;
        }

        // get preferences for BibEntry
        BibEntryPreferences bibPreferences = preferences.getBibEntryPreferences();
        Character dlim = bibPreferences.getKeywordSeparator();

        // create KeywordList to tokenize
        KeywordList keyList = KeywordList.parse(text, dlim);
        Iterator<Keyword> list = keyList.iterator();
        List<Keyword> modifiedList = new ArrayList<>();
        while (list.hasNext()) {
            // check if key in map and add value to result string
            // non-code keyword is present leave as-is
            Keyword item = list.next();
            String code = item.toString().trim(); // remove whitespace
            String convertedText = MscCodeUtils.getMscCodeRepository()
                    .flatMap(repository -> repository.getDescription(code))
                    .orElse(code);

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
        return "MSC_codes_to_descriptions";
    }

    @Override
    public String getExampleInput() {
        return "06E30";
    }

    private JabRefCliPreferences getCliPreferences() {
        if (cliPreferences != null) {
            return cliPreferences;
        }
        return Injector.instantiateModelOrService(JabRefCliPreferences.class);
    }

    @VisibleForTesting
    public static void setMscCodes(MscCodeRepository repository) {
        MscCodeUtils.setMscCodeRepository(repository);
    }
}
