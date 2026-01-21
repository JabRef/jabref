package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

/// Will return the Authors to match the OrgSci format:
///
///
/// - That is the first author is LastFirst, but all others are FirstLast.
/// - First names are abbreviated
/// - Spaces between abbreviated first names are NOT removed. Use
/// NoSpaceBetweenAbbreviations to achieve this.
///
///
/// See the testcase for examples.
///
///
/// Idea from: http://stuermer.ch/blog/bibliography-reference-management-with-jabref.html
///
public class AuthorOrgSci implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        AuthorList a = AuthorList.parse(fieldText);
        if (a.isEmpty()) {
            return fieldText;
        }
        Author first = a.getAuthor(0);
        StringBuilder sb = new StringBuilder();
        sb.append(first.getFamilyGiven(true));
        for (int i = 1; i < a.getNumberOfAuthors(); i++) {
            sb.append(", ").append(a.getAuthor(i).getGivenFamily(true));
        }
        return sb.toString();
    }
}
