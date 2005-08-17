/*
 Copyright (C) 2003 Nathan Dunn, Morten O. Alver

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref;

import net.sf.jabref.export.layout.format.RemoveBrackets;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

public class RegExpRule implements SearchRule {

    final boolean m_caseSensitiveSearch;
    static RemoveBrackets removeBrackets = new RemoveBrackets();

    public RegExpRule(boolean caseSensitive) {
        m_caseSensitiveSearch = caseSensitive;
    }

    public int applyRule(Map searchStrings, BibtexEntry bibtexEntry) throws PatternSyntaxException {

        int score = 0;
        Iterator e = searchStrings.values().iterator();

        String searchString = (String) e.next();

        int flags = 0;
        if (!m_caseSensitiveSearch)
            flags = Pattern.CASE_INSENSITIVE; // testing
        //System.out.println(searchString);
        Pattern pattern = Pattern.compile(searchString, flags);

        Object[] fields = bibtexEntry.getAllFields();
        score += searchFields(fields, bibtexEntry, pattern);

        return score;
    }

    protected int searchFields(Object[] fields, BibtexEntry bibtexEntry,
                               Pattern pattern) {
        int score = 0;
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                try {
                    Object value = bibtexEntry.getField((String)fields[i]);
                    if (value != null) {
                        Matcher m = pattern.matcher(removeBrackets.format((String)value));
                        if (m.find())
                            score++;
                    }
                }

                catch (Throwable t) {
                    System.err.println("Searching error: " + t);
                }
            }
        }
        return score;
    }

}
