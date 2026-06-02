package org.jabref.logic.formatter.bibtexfields;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jabref.logic.util.MscCodeUtils;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesFormatter extends Formatter implements LayoutFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertMSCCodesFormatter.class);
    private static final MscCodeRepository MSC_CODES;
    private static boolean conversionPossible;

    static {
        MSC_CODES = initializeRepository();
    }

    private static MscCodeRepository initializeRepository() {
        URL resourceUrl = ConvertMSCCodesFormatter.class.getClassLoader().getResource("MSC_2020.csv");
        if (resourceUrl == null) {
            LOGGER.error("Resource not found: MSC_2020.csv");
            conversionPossible = false;
            return new MscCodeRepository();
        }

        try {
            MscCodeRepository repository = MscCodeUtils.loadMscCodeRepositoryFromCsv(resourceUrl).orElseGet(MscCodeRepository::new);
            conversionPossible = !repository.getAllLoaded().isEmpty();
            return repository;
        } catch (MscCodeLoadingException e) {
            LOGGER.error("Error loading MSC codes", e);
            conversionPossible = false;
            return new MscCodeRepository();
        }
    }

    @NonNull
    @Override
    public String format(@NonNull String text) {
        if (text.isEmpty() || !conversionPossible) {
            return text;
        }

        // Using Injector to avoid widespread refactoring for constructor injection.
        // Class that calls formatters (FieldFormatterCleanupActions.java) has many usages that would need updates.
        JabRefCliPreferences cliPreferences = Injector.instantiateModelOrService(JabRefCliPreferences.class);

        // get preferences for BibEntry
        BibEntryPreferences bibPreferences = cliPreferences.getBibEntryPreferences();
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
            String convertedText = MSC_CODES.getDescription(code).orElse(code);

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
}
