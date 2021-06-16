package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class CreateBibORDFAuthors implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        // Yeah, the format is quite verbose... sorry about that :)

        //      <bibo:contribution>
        //        <bibo:Contribution>
        //          <bibo:role rdf:resource="http://purl.org/ontology/bibo/roles/author" />
        //          <bibo:contributor><foaf:Person foaf:name="Ola Spjuth"/></bibo:contributor>
        //          <bibo:position>1</bibo:position>
        //        </bibo:Contribution>
        //      </bibo:contribution>

        StringBuilder sb = new StringBuilder(100);

        if (!fieldText.contains(" and ")) {
            singleAuthor(sb, fieldText, 1);
        } else {
            String[] names = fieldText.split(" and ");
            for (int i = 0; i < names.length; i++) {
                singleAuthor(sb, names[i], i + 1);
                if (i < (names.length - 1)) {
                    sb.append('\n');
                }
            }
        }

        return sb.toString();
    }

    private static void singleAuthor(StringBuilder sb, String author, int position) {
        sb.append("<bibo:contribution>\n");
        sb.append("  <bibo:Contribution>\n");
        sb.append("    <bibo:role rdf:resource=\"http://purl.org/ontology/bibo/roles/author\" />\n");
        sb.append("    <bibo:contributor><foaf:Person foaf:name=\"").append(author).append("\"/></bibo:contributor>\n");
        sb.append("    <bibo:position>").append(position).append("</bibo:position>\n");
        sb.append("  </bibo:Contribution>\n");
        sb.append("</bibo:contribution>\n");
    }
}
