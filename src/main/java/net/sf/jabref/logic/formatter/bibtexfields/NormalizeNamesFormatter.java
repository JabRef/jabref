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
package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.AuthorList;

/**
 * Formatter normalizing a list of person names to the BibTeX format.
 */
public class NormalizeNamesFormatter implements Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize names of persons");
    }

    @Override
    public String getKey() {
        return "normalize_names";
    }

    @Override
    public String format(String value) {
        AuthorList authorList = AuthorList.parse(value);
        return authorList.getAsLastFirstNamesWithAnd(false);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes lists of persons to the BibTeX standard.");
    }

    @Override
    public String getExampleInput() {
        return "Albert Einstein and Alan Turing";
    }

}
