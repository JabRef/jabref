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

import java.util.Map;

import net.sf.jabref.export.layout.format.RemoveLatexCommands;

public class SimpleSearchRule implements SearchRule {

    final boolean m_caseSensitiveSearch;
    //static RemoveBrackets removeBrackets = new RemoveBrackets();
    static RemoveLatexCommands removeBrackets = new RemoveLatexCommands();

    public SimpleSearchRule(boolean caseSensitive) {
        m_caseSensitiveSearch = caseSensitive;
    }

    public int applyRule(Map<String, String> searchStrings, BibtexEntry bibtexEntry) {
        String searchString = searchStrings.values().iterator().next();

        if (!m_caseSensitiveSearch)
            searchString = searchString.toLowerCase();
        int score = 0;
        int counter = 0;
        Object fieldContentAsObject;
        String fieldContent;
        for (String field : bibtexEntry.getAllFields()){
            fieldContentAsObject = bibtexEntry.getField(field); 
            if (fieldContentAsObject != null)
                try {
                    fieldContent = removeBrackets.format(fieldContentAsObject.toString());
                    if (!m_caseSensitiveSearch)
                        fieldContent = fieldContent.toLowerCase();
                    counter = fieldContent.indexOf(searchString, counter);
                    while (counter >= 0) {
                        ++score;
                        counter = fieldContent.indexOf(searchString, counter + 1);
                    }
                } catch (Throwable t) {
                    System.err.println("sorting error: " + t);
                }
            counter = 0;
        }
        return score;
    }

}
