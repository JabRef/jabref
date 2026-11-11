package org.jabref.logic.layout.format;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

public class Iso690NamesAuthors implements LayoutFormatter {

    @Override
    public String format(String s) {
        if (StringUtil.isBlank(s)) {
            return "";
        }

        List<Author> authors = AuthorList.parse(s).getAuthors();
        StringJoiner formatted = new StringJoiner(", ");
        for (int i = 0; i < authors.size(); i++) {
            Author author = authors.get(i);
            StringJoiner name = new StringJoiner(", ");
            name.add(author.getNamePrefixAndFamilyName().toUpperCase(Locale.ROOT));
            author.getNameSuffix().ifPresent(name::add);
            author.getGivenName().ifPresent(name::add);
            if ((i == authors.size() - 1) && (i > 0)) {
                return formatted + " y " + name;
            }
            formatted.add(name.toString());
        }
        return formatted.toString();
    }
}
