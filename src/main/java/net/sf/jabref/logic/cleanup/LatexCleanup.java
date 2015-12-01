/*  Copyright (C) 2012-2015 JabRef contributors.
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
package net.sf.jabref.logic.cleanup;

import net.sf.jabref.model.entry.BibtexEntry;

public class LatexCleanup {

    private final BibtexEntry entry;


    public LatexCleanup(BibtexEntry entry) {
        this.entry = entry;
    }

    public LatexCleanup() {
        this.entry = null;
    }

    public void cleanup() {
        if (this.entry == null) {
            return;
        }

        final String field = "title";

        String newValue = format(entry.getField(field));

        entry.setField(field, newValue);
    }

    public String format(String oldString) {
        String newValue = oldString;

        // Remove redundant $, {, and }, but not if the } is part of a command argument: \mbox{-}{GPS} should not be adjusted
        newValue = newValue.replace("$$", "").replaceAll("(?<!\\\\[\\p{Alpha}]{0,100}\\{[^\\}]{0,100})\\}([-/ ]?)\\{",
                "$1");
        // Move numbers, +, -, /, and brackets into equations
        newValue = newValue.replaceAll("(([^$]|\\\\\\$)*)\\$", "$1@@"); // Replace $, but not \$ with @@
        newValue = newValue.replaceAll("([^@]*)@@([^@]*)@@", "$1\\$$2@@"); // Replace every other @@ with $
        //newValue = newValue.replaceAll("([0-9\\(\\.]+) \\$","\\$$1\\\\ "); // Move numbers followed by a space left of $ inside the equation, e.g., 0.35 $\mu$m
        newValue = newValue.replaceAll("([0-9\\(\\.]+[ ]?[-+/]?[ ]?)\\$", "\\$$1"); // Move numbers, possibly with operators +, -, or /,  left of $ into the equation
        newValue = newValue.replaceAll("@@([ ]?[-+/]?[ ]?[0-9\\)\\.]+)", " $1@@"); // Move numbers right of @@ into the equation
        newValue = newValue.replace("@@", "$"); // Replace all @@ with $
        newValue = newValue.replace("  ", " "); // Clean up
        newValue = newValue.replace("$$", "");
        newValue = newValue.replace(" )$", ")$");
        return newValue;
    }

}
