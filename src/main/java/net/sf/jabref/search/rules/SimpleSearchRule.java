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
package net.sf.jabref.search.rules;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.layout.format.RemoveLatexCommands;
import net.sf.jabref.search.SearchRule;

//TODO why have simple and basic search rule????
public class SimpleSearchRule implements SearchRule {

    private static final RemoveLatexCommands REMOVE_LATEX_COMMANDS = new RemoveLatexCommands();

    private final boolean caseSensitive;

    public SimpleSearchRule(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return true;
    }

    @Override
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {
        int score = 0;
        for (String field : bibtexEntry.getAllFields()) {
            Object fieldContentAsObject = bibtexEntry.getField(field);
            if (fieldContentAsObject == null) {
                continue;
            }

            try {
                String fieldContent = sanatizeFieldContent(fieldContentAsObject);
                score += getNumberOfOccurrences(fieldContent, sanatizeString(query));
            } catch (Throwable t) {
                System.err.println("sorting error: " + t);
            }
        }
        return score > 0;
    }

    private String sanatizeString(String query) {
        if (!caseSensitive) {
            return query.toLowerCase();
        } else {
            return query;
        }
    }

    private String sanatizeFieldContent(Object fieldContentAsObject) {
        return sanatizeString(SimpleSearchRule.REMOVE_LATEX_COMMANDS.format(fieldContentAsObject.toString()));
    }

    private static int getNumberOfOccurrences(String haystack, String needle) {
        int score = 0;
        int counter = haystack.indexOf(needle);
        while (counter >= 0) {
            score++;
            counter = haystack.indexOf(needle, counter + 1);
        }
        return score;
    }

}
