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

import java.util.ArrayList;

import javax.swing.JOptionPane;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;

public class ParserResultWarningDialog {

    private final ParserResult parserResult;
    private final int maxWarnings;
    private final JabRefFrame jabRefFrame;
    private final int dbCount;


    public ParserResultWarningDialog(ParserResult pr, JabRefFrame jrf) {
        parserResult = pr;
        maxWarnings = Integer.MAX_VALUE;
        jabRefFrame = jrf;
        dbCount = -1;
    }

    public ParserResultWarningDialog(ParserResult pr, JabRefFrame jrf, int maxWarns, int dataBaseCount) {
        parserResult = pr;
        maxWarnings = maxWarns;
        jabRefFrame = jrf;
        dbCount = dataBaseCount;
    }

    public void show() {
        if (parserResult.hasWarnings()) {
            if (jabRefFrame != null) {
                jabRefFrame.showBasePanelAt(dbCount);
            }
            // Note to self or to someone else: The following line causes an
            // ArrayIndexOutOfBoundsException in situations with a large number of
            // warnings; approx. 5000 for the database I opened when I observed the problem
            // (duplicate key warnings). I don't think this is a big problem for normal situations,
            // and it may possibly be a bug in the Swing code.

            JOptionPane.showMessageDialog(jabRefFrame, generateString(), generateDialogTitle(),
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private String generateString() {
        ArrayList<String> wrns = parserResult.warnings();
        StringBuilder wrn = new StringBuilder();
        for (int j = 0; j < Math.min(maxWarnings, wrns.size()); j++) {
            wrn.append(j + 1).append(". ").append(wrns.get(j)).append("\n");
        }
        if (wrns.size() > maxWarnings) {
            wrn.append("... ");
            wrn.append(Localization.lang("%0 warnings", String.valueOf(wrns.size())));
        } else if (wrn.length() > 0) {
            wrn.deleteCharAt(wrn.length() - 1);
        }
        return wrn.toString();
    }

    private String generateDialogTitle() {
        if (dbCount < 0) {
            return Localization.lang("Warnings");
        } else {
            return Localization.lang("Warnings") + " (" + parserResult.getFile().getName() + ")";
        }
    }
}
