package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.FieldName;

/**
 * Create DocBook5 authors formatter
 */
public class CreateDocBook5Authors implements LayoutFormatter {

    @Override
    public String format(String fieldText) {

        StringBuilder sb = new StringBuilder(100);
        AuthorList al = AuthorList.parse(fieldText);

        DocBookAuthorFormatter authorFormatter = new DocBookAuthorFormatter();
        authorFormatter.addBody(sb, al, FieldName.AUTHOR, DocBookVersion.DOCBOOK_5);
        return sb.toString();
    }
}
