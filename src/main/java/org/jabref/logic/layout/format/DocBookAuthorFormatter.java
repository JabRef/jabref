package org.jabref.logic.layout.format;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

/**
 * DocBook author formatter for both version 4 and 5
 */
public class DocBookAuthorFormatter {

    private static final XMLChars XML_CHARS = new XMLChars();

    /**
     * @param sb      {@link StringBuilder}
     * @param al      {@link AuthorList}
     * @param tagName Editor or author field/tag
     * @param version @link {@link DocBookVersion}
     */
    public void addBody(StringBuilder sb, AuthorList al, String tagName, DocBookVersion version) {
        for (int i = 0; i < al.getNumberOfAuthors(); i++) {
            sb.append('<').append(tagName).append('>');
            if (version == DocBookVersion.DOCBOOK_5) {
                sb.append("<personname>");
            }
            Author a = al.getAuthor(i);
            a.getFirst().filter(first -> !first.isEmpty()).ifPresent(first -> sb.append("<firstname>")
                                                                                .append(XML_CHARS.format(first)).append("</firstname>"));
            a.getVon().filter(von -> !von.isEmpty()).ifPresent(von -> sb.append("<othername>")
                                                                        .append(XML_CHARS.format(von)).append("</othername>"));
            a.getLast().filter(last -> !last.isEmpty()).ifPresent(last -> {
                sb.append("<surname>").append(XML_CHARS.format(last));
                a.getJr().filter(jr -> !jr.isEmpty())
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
