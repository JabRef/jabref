package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.FieldName;

/**
 * Create DocBook authors formatter.
 */
public class CreateDocBookAuthors implements LayoutFormatter {

    private static final XMLChars XML_CHARS = new XMLChars();

    @Override
    public String format(String fieldText) {

        StringBuilder sb = new StringBuilder(100);

        AuthorList al = AuthorList.parse(fieldText);

        addBody(sb, al, FieldName.AUTHOR);
        return sb.toString();

    }

    public void addBody(StringBuilder sb, AuthorList al, String tagName) {
        for (int i = 0; i < al.getNumberOfAuthors(); i++) {
            sb.append('<').append(tagName).append('>');
            Author a = al.getAuthor(i);
            a.getFirst().filter(first -> !first.isEmpty()).ifPresent(first -> sb.append("<firstname>")
                    .append(CreateDocBookAuthors.XML_CHARS.format(first)).append("</firstname>"));
            a.getVon().filter(von -> !von.isEmpty()).ifPresent(von -> sb.append("<othername>")
                    .append(CreateDocBookAuthors.XML_CHARS.format(von)).append("</othername>"));
            a.getLast().filter(last -> !last.isEmpty()).ifPresent(last -> {
                sb.append("<surname>").append(CreateDocBookAuthors.XML_CHARS.format(last));
                a.getJr().filter(jr -> !jr.isEmpty())
                        .ifPresent(jr -> sb.append(' ').append(CreateDocBookAuthors.XML_CHARS.format(jr)));
                sb.append("</surname>");
            });

            if (i < (al.getNumberOfAuthors() - 1)) {
                sb.append("</").append(tagName).append(">\n       ");
            } else {
                sb.append("</").append(tagName).append('>');
            }
        }
    }

}
