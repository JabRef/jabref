/*  Copyright (C) 2003-2015 JabRef contributors and Moritz Ringler, Simon Harrer
    Copyright (C) 2015 Ocar Gustafsson, Oliver Kopp

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
package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.casechanger.*;

import java.util.*;

/**
 * Class with static methods for changing the case of strings and arrays of strings.
 * <p>
 * This class must detect the words in the title and whether letters are protected for case changes via enclosing them with '{' and '}' brakets.
 * Hence, for each letter to be changed, it needs to be known wether it is protected or not.
 * This can be done by starting at the letter position and moving forward and backword to see if there is a '{' and '}, respectively.
 */
public class CaseChangers {
    public static final LowerCaseFormatter TO_LOWER_CASE = new LowerCaseFormatter();
    public static final UpperCaseFormatter TO_UPPER_CASE = new UpperCaseFormatter();
    public static final SentenceCaseFormatter TO_SENTENCE_CASE = new SentenceCaseFormatter();
    public static final CapitalizeFormatter CAPITALIZE = new CapitalizeFormatter();
    public static final TitleCaseFormatter TO_TITLE_CASE = new TitleCaseFormatter();
    public static final ProtectTermsFormatter PROTECT_CAPITALS = new ProtectTermsFormatter();

    public static final List<Formatter> ALL = Arrays.asList(TO_LOWER_CASE, TO_UPPER_CASE, TO_SENTENCE_CASE, CAPITALIZE, TO_TITLE_CASE,
            PROTECT_CAPITALS);
}
