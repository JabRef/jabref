package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.field.StandardField;

/**
 * Create DocBook4 editors formatter.
 */
public class CreateDocBook4Editors implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        // <editor><firstname>L.</firstname><surname>Xue</surname></editor>
        StringBuilder sb = new StringBuilder(100);
        AuthorList al = AuthorList.parse(fieldText);
        DocBookAuthorFormatter formatter = new DocBookAuthorFormatter();
        formatter.addBody(sb, al, StandardField.EDITOR.getName(), DocBookVersion.DOCBOOK_4);
        return sb.toString();
    }
}
