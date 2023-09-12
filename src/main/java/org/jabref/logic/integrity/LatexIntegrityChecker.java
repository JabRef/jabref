package org.jabref.logic.integrity;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ed.ph.snuggletex.ErrorCode;
import uk.ac.ed.ph.snuggletex.InputError;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.definitions.CoreErrorCode;
import uk.ac.ed.ph.snuggletex.definitions.CoreErrorGroup;

public class LatexIntegrityChecker implements EntryChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnuggleSession.class);
    private static final SnuggleEngine ENGINE = new SnuggleEngine();
    private static final SnuggleSession SESSION = ENGINE.createSession();
    private static final ResourceBundle ERROR_MESSAGES = ENGINE.getPackages().get(0).getErrorMessageBundle();
    private static final Set<ErrorCode> EXCLUDED_ERRORS = new HashSet<>();

    static {
        // Static configuration.
        SESSION.getConfiguration().setFailingFast(true);

        // '#' only allowed inside and command/environment definitions.
        EXCLUDED_ERRORS.add(CoreErrorCode.TTEG04);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();

        for (Map.Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            SnuggleInput input = new SnuggleInput(field.getValue());
            try {
                SESSION.parseInput(input);
            } catch (IOException e) {
                LOGGER.error("Error at parsing", e);
            }
            if (!SESSION.getErrors().isEmpty()) {
                // Retrieve the first error only because it is likely to be more meaningful.
                // Displaying all (subsequent) faults may lead to confusion.
                // We further get a slight performance benefit from failing fast (see static config in class header).
                InputError error = SESSION.getErrors().get(0);
                ErrorCode errorCode = error.getErrorCode();
                // Exclude all DOM building errors as this functionality is not used.
                // Further, exclude individual errors.
                if (!errorCode.getErrorGroup().equals(CoreErrorGroup.TDE) && !EXCLUDED_ERRORS.contains(errorCode)) {
                    String jabrefMessageWrapper = errorMessageFormatHelper(errorCode, error.getArguments());
                    results.add(new IntegrityMessage(jabrefMessageWrapper, entry, field.getKey()));
                }
            }
            SESSION.reset();
        }
        return results;
    }

    public static String errorMessageFormatHelper(ErrorCode snuggleTexErrorCode, Object... arguments) {
        String snuggletexMessagePattern = LatexIntegrityChecker.ERROR_MESSAGES.getString(snuggleTexErrorCode.getName());
        String snuggletexErrorMessage = MessageFormat.format(snuggletexMessagePattern, arguments);
        return Localization.lang("LaTeX Parsing Error: %0", snuggletexErrorMessage);
    }
}



