package org.jabref.logic.layout.format;

import java.util.function.Predicate;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import static java.util.function.Predicate.not;

/// DocBook author formatter for both version 4 and 5
public class DocBookAuthorFormatter {

    private static final XMLChars XML_CHARS = new XMLChars();

    /// @param tagName Editor or author field/tag
    public void addBody(StringBuilder sb, AuthorList al, String tagName, DocBookVersion version) {
        for (int i = 0; i < al.getNumberOfAuthors(); i++) {
            sb.append('<').append(tagName).append('>');
            if (version == DocBookVersion.DOCBOOK_5) {
                sb.append("<personname>");
            }
            Author a = al.getAuthor(i);
            a.getGivenName().filter(not(String::isEmpty)).ifPresent(first -> sb.append("<firstname>")
                                                                                    .append(XML_CHARS.format(first)).append("</firstname>"));
            a.getNamePrefix().filter(not(String::isEmpty)).ifPresent(von -> sb.append("<othername>")
                                                                               .append(XML_CHARS.format(von)).append("</othername>"));
            a.getFamilyName().filter(not(String::isEmpty)).ifPresent(last -> {
                sb.append("<surname>").append(XML_CHARS.format(last));
                a.getNameSuffix().filter(not(String::isEmpty))
                 .ifPresent(jr -> sb.append(' ').append(XML_CHARS.format(jr)));
                sb.append("</surname>");
                if (version == DocBookVersion.DOCBOOK_5) {
                    sb.append("</personname>");
                }
            });

            if (i < (al.getNumberOfAuthors() - 1)) {
                sb.append("</").append(tagName).append(">\n       ");
            } else {
                sb.append("</").append(tagName).append('>');
            }
        }
    }
}
