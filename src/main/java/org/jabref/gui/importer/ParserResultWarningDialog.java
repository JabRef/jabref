package org.jabref.gui.importer;

import java.util.List;
import java.util.Objects;

import org.jabref.gui.DialogService;
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
     * @param parserResult   - ParserResult for the current import/open
     */
    public static void showParserResultWarningDialog(final ParserResult parserResult,
                                                     final DialogService dialogService) {
        Objects.requireNonNull(parserResult);
        // Return if no warnings
        if (!parserResult.hasWarnings()) {
            return;
        }

        // Generate string with warning texts
        final List<String> warnings = parserResult.warnings();
        final StringBuilder dialogContent = new StringBuilder();
        int warningCount = 1;
        for (final String warning : warnings) {
            dialogContent.append("%d. %s%n".formatted(warningCount++, warning));
        }
        dialogContent.deleteCharAt(dialogContent.length() - 1);

        // Generate dialog title
        String dialogTitle;
        if (parserResult.getPath().isEmpty()) {
            dialogTitle = Localization.lang("Warnings");
        } else {
            dialogTitle = Localization.lang("Warnings") + " (" + parserResult.getPath().get().getFileName() + ")";
        }

        // Show dialog
        dialogService.showWarningDialogAndWait(dialogTitle, dialogContent.toString());
    }
}
