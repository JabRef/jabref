package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

/**
 * Will return the Authors to match the OrgSci format:
 *
 * <ul>
 * <li>That is the first author is LastFirst, but all others are FirstLast.</li>
 * <li>First names are abbreviated</li>
 * <li>Spaces between abbreviated first names are NOT removed. Use
 * NoSpaceBetweenAbbreviations to achieve this.</li>
 * </ul>
 * <p>
 * See the testcase for examples.
 * </p>
 * <p>
 * Idea from: http://stuermer.ch/blog/bibliography-reference-management-with-jabref.html
 * </p>
 */
public class AuthorOrgSci implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        AuthorList a = AuthorList.parse(fieldText);
        if (a.isEmpty()) {
            return fieldText;
        }
        Author first = a.getAuthor(0);
        StringBuilder sb = new StringBuilder();
        sb.append(first.getLastFirst(true));
        for (int i = 1; i < a.getNumberOfAuthors(); i++) {
            sb.append(", ").append(a.getAuthor(i).getFirstLast(true));
        }
        return sb.toString();
    }
}
