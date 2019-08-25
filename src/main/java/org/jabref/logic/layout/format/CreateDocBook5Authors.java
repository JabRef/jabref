package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.field.StandardField;

/**
 * Create DocBook5 authors formatter
 */
public class CreateDocBook5Authors implements LayoutFormatter {

    @Override
    public String format(String fieldText) {

        StringBuilder sb = new StringBuilder(100);
        AuthorList al = AuthorList.parse(fieldText);

        DocBookAuthorFormatter authorFormatter = new DocBookAuthorFormatter();
        authorFormatter.addBody(sb, al, StandardField.AUTHOR.getName(), DocBookVersion.DOCBOOK_5);
        return sb.toString();
    }
}
