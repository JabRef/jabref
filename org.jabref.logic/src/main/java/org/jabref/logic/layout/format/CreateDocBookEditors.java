package org.jabref.logic.layout.format;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.FieldName;

/**
 * Create DocBook editors formatter.
 */
public class CreateDocBookEditors extends CreateDocBookAuthors {

    @Override
    public String format(String fieldText) {
        //		<editor><firstname>L.</firstname><surname>Xue</surname></editor>
        StringBuilder sb = new StringBuilder(100);
        AuthorList al = AuthorList.parse(fieldText);
        addBody(sb, al, FieldName.EDITOR);
        return sb.toString();

    }

}
