package org.jabref.logic.integrity;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import javafx.util.Pair;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ed.ph.snuggletex.ErrorCode;
import uk.ac.ed.ph.snuggletex.InputError;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnugglePackage;
import uk.ac.ed.ph.snuggletex.SnuggleSession;
import uk.ac.ed.ph.snuggletex.definitions.CoreErrorCode;
import uk.ac.ed.ph.snuggletex.definitions.CoreErrorGroup;

import static uk.ac.ed.ph.snuggletex.definitions.Globals.TEXT_MODE_ONLY;

/**
 * Similar check to {@link HTMLCharacterChecker}.
 * Here, we use <a href="https://github.com/davemckain/snuggletex">SnuggleTeX</a>, in the {@link HTMLCharacterChecker}, it is searched for HTML characters.
 *
 * Unescaped ampersands cannot be checked by SnuggleTeX, therefore the {@link AmpersandChecker} is available additionaly.
 */
public class LatexIntegrityChecker implements EntryChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnuggleSession.class);
    private static final SnuggleEngine ENGINE = new SnuggleEngine();
    private static final SnuggleSession SESSION;
    private static final ResourceBundle ERROR_MESSAGES = ENGINE.getPackages().get(0).getErrorMessageBundle();
    private static final Set<ErrorCode> EXCLUDED_ERRORS = new HashSet<>();

    static {
        SnugglePackage snugglePackage = ENGINE.getPackages().get(0);
        snugglePackage.addComplexCommand("textgreater", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textless", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textbackslash", false, 0, TEXT_MODE_ONLY, null, null, null);
        snugglePackage.addComplexCommand("textbar", false, 0, TEXT_MODE_ONLY, null, null, null);
        // ENGINE.getPackages().get(0).addComplexCommandOneArg()
              // engine.getPackages().get(0).addComplexCommandOneArg("text", false, ALL_MODES,LR, StyleDeclarationInterpretation.NORMALSIZE, null, TextFlowContext.ALLOW_INLINE);

        SESSION = ENGINE.createSession();
        SESSION.getConfiguration().setFailingFast(true);

        // '#' only allowed inside and command/environment definitions.
        EXCLUDED_ERRORS.add(CoreErrorCode.TTEG04);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> !field.getKey().getProperties().contains(FieldProperty.VERBATIM))
                    .flatMap(LatexIntegrityChecker::getUnescapedAmpersandsWithCount)
                    // Exclude all DOM building errors as this functionality is not used.
                    .filter(pair -> !pair.getValue().getErrorCode().getErrorGroup().equals(CoreErrorGroup.TDE))
                    .filter(pair -> !EXCLUDED_ERRORS.contains(pair.getValue().getErrorCode()))
                    .map(pair ->
                            new IntegrityMessage(errorMessageFormatHelper(pair.getValue().getErrorCode(), pair.getValue().getArguments()), entry, pair.getKey()))
                    .toList();
    }

    private static Stream<Pair<Field, InputError>> getUnescapedAmpersandsWithCount(Map.Entry<Field, String> entry) {
        SESSION.reset();
        SnuggleInput input = new SnuggleInput(entry.getValue());
        try {
            SESSION.parseInput(input);
        } catch (IOException e) {
            LOGGER.error("Error at parsing", e);
            return Stream.empty();
        }
        if (SESSION.getErrors().isEmpty()) {
            return Stream.empty();
        }
        // Retrieve the first error only because it is likely to be more meaningful.
        // Displaying all (subsequent) faults may lead to confusion.
        // We further get a slight performance benefit from failing fast (see static config in class header).
        InputError error = SESSION.getErrors().get(0);
        return Stream.of(new Pair<>(entry.getKey(), error));
    }

    public static String errorMessageFormatHelper(ErrorCode snuggleTexErrorCode, Object... arguments) {
        String snuggletexMessagePattern = LatexIntegrityChecker.ERROR_MESSAGES.getString(snuggleTexErrorCode.getName());
        String snuggletexErrorMessage = MessageFormat.format(snuggletexMessagePattern, arguments);
        return Localization.lang("LaTeX Warning: %0", snuggletexErrorMessage);
    }
}
