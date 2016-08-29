package net.sf.jabref.gui.importer;

import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Class for generating a dialog showing warnings from ParserResult
 *
 */
public class ParserResultWarningDialog {

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult - ParserResult for the current import/open
     * @param jabRefFrame - the JabRefFrame
     */
    public static void showParserResultWarningDialog(final ParserResult parserResult, final JabRefFrame jabRefFrame) {
        Objects.requireNonNull(parserResult);
        Objects.requireNonNull(jabRefFrame);
        showParserResultWarningDialog(parserResult, jabRefFrame, -1);
    }

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult - ParserResult for the current import/open
     * @param jabRefFrame - the JabRefFrame
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
            jabRefFrame.showBasePanelAt(dataBaseNumber);
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
        if (dataBaseNumber < 0) {
            dialogTitle = Localization.lang("Warnings");
        } else {
            dialogTitle = Localization.lang("Warnings") + " (" + parserResult.getFile().get().getName() + ")";
        }

        // Create JTextArea with JScrollPane
        final JTextArea textArea = new JTextArea(dialogContent.toString());
        final JScrollPane scrollPane = new JScrollPane(textArea) {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, Math.min(Math.max(100, warnings.size() * 15), 400)); // Guess a suitable height between 100 and 400
            }
        };

        // Show dialog
        JOptionPane.showMessageDialog(jabRefFrame, scrollPane, dialogTitle, JOptionPane.WARNING_MESSAGE);
    }
}
