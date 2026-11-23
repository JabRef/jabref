package org.jabref.logic.openoffice.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class RendererTestData {

    static final String SIMPLE_BST = """
            ENTRY { title year author } {} {}
            FUNCTION {article}
            { "\\bibitem{" write$ cite$ write$ "}" write$ newline$
              title write$ newline$
            }
            FUNCTION {misc} { article }
            FUNCTION {default.type} { article }
            READ
            ITERATE {call.type$}
            """;

    private RendererTestData() {
    }

    static BibEntry entry(String key, String title) {
        return new BibEntry().withCitationKey(key).withField(StandardField.AUTHOR, "Smith, Jane")
                             .withField(StandardField.YEAR, "2020").withField(StandardField.TITLE, title);
    }

    static CitationGroup group(String id, CitationType type, String... keys) {
        return new CitationGroup(new CitationGroupId(id), type,
                Arrays.stream(keys).map(Citation::new).toList(), Optional.of("mark-" + id));
    }

    static RenderRequest request(List<CitationGroup> groups, List<BibEntry> entries) {
        return new RenderRequest(groups, List.of(new BibDatabase(entries)), new RenderOptions(BstCitationFormat.NUMERIC));
    }

    static List<String> markers(RenderRequest request, RenderResult result) {
        return request.groupsInDocumentOrder().stream().map(group -> result.markers().get(group.groupId).toString())
                      .map(text -> text.replace("<span lang=\"zxx\">", "").replace("</span>", "")).toList();
    }

    static CitationStyle numericStyle(boolean sorted, boolean collapse) {
        String bibliographySort = sorted ? "<sort><key variable=\"title\"/></sort>" : "";
        String source = """
                <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                  <info><title>Engine numeric</title><id>http://www.zotero.org/styles/engine-numeric</id></info>
                  <citation %s>
                    <sort><key variable="citation-number"/></sort>
                    <layout prefix="[" suffix="]" delimiter=", "><text variable="citation-number"/></layout>
                  </citation>
                  <bibliography>%s
                    <layout><text variable="citation-number" suffix=". "/><text variable="title"/></layout>
                  </bibliography>
                </style>
                """.formatted(collapse ? "collapse=\"citation-number\"" : "", bibliographySort);
        return new CitationStyle("engine-numeric.csl", "engine-numeric", "in-text", "Engine numeric", "", true, true, sorted, false, source, false);
    }
}
