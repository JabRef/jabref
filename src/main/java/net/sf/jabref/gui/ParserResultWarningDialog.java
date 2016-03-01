/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import java.util.List;
import java.util.Objects;

import javax.swing.JOptionPane;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;

public class ParserResultWarningDialog {

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult - ParserResult for the current import/open
     * @param jabRefFrame - the JabRefFrame
     */
    public static void showParserResultWarningDialog(ParserResult parserResult, JabRefFrame jabRefFrame) {
        Objects.requireNonNull(parserResult);
        Objects.requireNonNull(jabRefFrame);
        showParserResultWarningDialog(parserResult, jabRefFrame, Integer.MAX_VALUE, -1);
    }

    /**
     * Shows a dialog with the warnings from an import or open of a file
     *
     * @param parserResult - ParserResult for the current import/open
     * @param jabRefFrame - the JabRefFrame
     * @param maxWarnings - Maximum number of warnings to display
     * @param dataBaseNumber - Database tab number to activate when showing the warning dialog
     */
    public static void showParserResultWarningDialog(ParserResult parserResult, JabRefFrame jabRefFrame,
            int maxWarnings, int dataBaseNumber) {
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
        List<String> warnings = parserResult.warnings();
        StringBuilder dialogContent = new StringBuilder();
        for (int j = 0; j < Math.min(maxWarnings, warnings.size()); j++) {
            dialogContent.append(j + 1).append(". ").append(warnings.get(j)).append("\n");
        }
        if (warnings.size() > maxWarnings) {
            dialogContent.append("... ");
            dialogContent.append(Localization.lang("%0 warnings", String.valueOf(warnings.size())));
        } else if (dialogContent.length() > 0) {
            dialogContent.deleteCharAt(dialogContent.length() - 1);
        }

        // Generate dialog title
        String dialogTitle;
        if (dataBaseNumber < 0) {
            dialogTitle = Localization.lang("Warnings");
        } else {
            dialogTitle = Localization.lang("Warnings") + " (" + parserResult.getFile().getName() + ")";
        }

        // Comment from the old code:
        //
        // Note to self or to someone else: The following line causes an
        // ArrayIndexOutOfBoundsException in situations with a large number of
        // warnings; approx. 5000 for the database I opened when I observed the problem
        // (duplicate key warnings). I don't think this is a big problem for normal situations,
        // and it may possibly be a bug in the Swing code.

        // Show dialog
        JOptionPane.showMessageDialog(jabRefFrame, dialogContent.toString(), dialogTitle, JOptionPane.WARNING_MESSAGE);
    }
}
