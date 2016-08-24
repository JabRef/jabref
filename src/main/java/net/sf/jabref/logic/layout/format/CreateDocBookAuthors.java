package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.FieldName;

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
            if ((a.getFirst() != null) && !a.getFirst().isEmpty()) {
                sb.append("<firstname>").append(CreateDocBookAuthors.XML_CHARS.format(a.getFirst()))
                        .append("</firstname>");
            }
            if ((a.getVon() != null) && !a.getVon().isEmpty()) {
                sb.append("<othername>").append(CreateDocBookAuthors.XML_CHARS.format(a.getVon()))
                        .append("</othername>");
            }
            if ((a.getLast() != null) && !a.getLast().isEmpty()) {
                sb.append("<surname>").append(CreateDocBookAuthors.XML_CHARS.format(a.getLast()));
                if ((a.getJr() != null) && !a.getJr().isEmpty()) {
                    sb.append(' ').append(CreateDocBookAuthors.XML_CHARS.format(a.getJr()));
                }
                sb.append("</surname>");
            }

            if (i < (al.getNumberOfAuthors() - 1)) {
                sb.append("</").append(tagName).append(">\n       ");
            } else {
                sb.append("</").append(tagName).append('>');
            }
        }
    }

}
