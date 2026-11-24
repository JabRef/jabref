package org.jabref.logic.layout.format;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

// [impl->adr~export-ordered-bibo-authors-and-creators~1]
public class CreateBibORDFAuthors implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        AuthorList authors = AuthorList.parse(fieldText);
        if (authors.isEmpty()) {
            return "";
        }

        XMLChars xmlChars = new XMLChars();
        StringBuilder result = new StringBuilder();
        List<String> authorIds = new ArrayList<>();
        for (Author author : authors) {
            String id = "author" + UUID.randomUUID();
            authorIds.add(id);
            result.append("<dcterms:creator><foaf:Person rdf:nodeID=\"")
                  .append(id)
                  .append("\" foaf:name=\"")
                  .append(xmlChars.format(author.getGivenFamily(false)))
                  .append("\"/></dcterms:creator>\n");
        }
        result.append("<bibo:authorList><rdf:Seq>\n");
        for (String id : authorIds) {
            result.append("<rdf:li rdf:nodeID=\"").append(id).append("\"/>\n");
        }
        return result.append("</rdf:Seq></bibo:authorList>\n").toString();
    }
}
