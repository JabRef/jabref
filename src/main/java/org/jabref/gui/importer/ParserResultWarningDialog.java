package org.jabref.gui.importer;

import java.util.List;
import java.util.Objects;

import org.jabref.gui.JabRefFrame;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;

/**
 * Class for generating a dialog showing warnings from ParserResult
 */
public class ParserResultWarningDialog {

    private ParserResultWarningDialog() {
    }

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult - ParserResult for the current import/open
     * @param jabRefFrame  - the JabRefFrame
     */
    public static void showParserResultWarningDialog(final ParserResult parserResult, final JabRefFrame jabRefFrame) {
        Objects.requireNonNull(parserResult);
        Objects.requireNonNull(jabRefFrame);
        showParserResultWarningDialog(parserResult, jabRefFrame, -1);
    }

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult   - ParserResult for the current import/open
     * @param jabRefFrame    - the JabRefFrame
     * @param dataBaseNumber - Database tab number to activate when showing the warning dialog
     */
    public static void showParserResultWarningDialog(final ParserResult parserResult, final JabRefFrame jabRefFrame,
                                                     final int dataBaseNumber) {
        Objects.requireNonNull(parserResult);
        Objects.requireNonNull(jabRefFrame);
        // Return if no warnings
        if (!(parserResult.hasWarnings())) {
            return;
        }

        // Switch tab if asked to do so
        if (dataBaseNumber >= 0) {
            jabRefFrame.showLibraryTabAt(dataBaseNumber);
        }

        // Generate string with warning texts
        final List<String> warnings = parserResult.warnings();
        final StringBuilder dialogContent = new StringBuilder();
        int warningCount = 1;
        for (final String warning : warnings) {
            dialogContent.append(String.format("%d. %s%n", warningCount++, warning));
        }
        dialogContent.deleteCharAt(dialogContent.length() - 1);

        // Generate dialog title
        String dialogTitle;
        if (dataBaseNumber < 0 || parserResult.getPath().isEmpty()) {
            dialogTitle = Localization.lang("Warnings");
        } else {
            dialogTitle = Localization.lang("Warnings") + " (" + parserResult.getPath().get().getFileName() + ")";
        }

        // Show dialog
        jabRefFrame.getDialogService().showWarningDialogAndWait(dialogTitle, dialogContent.toString());
    }
}
