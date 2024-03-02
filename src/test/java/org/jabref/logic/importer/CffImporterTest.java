import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.CffImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CffImporterTest {

    @Test
    public void PreferredCitationTest() throws Exception {
        String cffContent = """
                cff-version: 1.2.0
                message: "If you use this software, please cite it as below."
                authors:
                - family-names: "Lisa"
                  given-names: "Mona"
                  orcid: "https://orcid.org/0000-0000-0000-0000"
                - family-names: "Bot"
                  given-names: "Hew"
                  orcid: "https://orcid.org/0000-0000-0000-0000"
                title: "My Research Software"
                version: 2.0.4
                doi: 10.5281/zenodo.1234
                date-released: 2017-12-18
                url: "https://github.com/github-linguist/linguist"
                preferred-citation:
                  type: article
                  authors:
                  - family-names: "Lisa"
                    given-names: "Mona"
                    orcid: "https://orcid.org/0000-0000-0000-0000"
                  - family-names: "Bot"
                    given-names: "Hew"
                    orcid: "https://orcid.org/0000-0000-0000-0000"
                  doi: "10.0000/00000"
                  journal: "Journal Title"
                  month: 9
                  start: 1
                  end: 10
                  title: "My awesome research software"
                  issue: 1
                  volume: 1
                  year: 2021
                """;

        CffImporter importer = new CffImporter();
        try (BufferedReader reader = new BufferedReader(new StringReader(cffContent))) {
            ParserResult result = importer.importDatabase(reader);
            List<BibEntry> entries = result.getDatabase().getEntries();

            assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);

            assertEquals("My awesome research software", entry.getField(StandardField.TITLE).orElse(null));
            assertEquals("Mona Lisa and Hew Bot", entry.getField(StandardField.AUTHOR).orElse(null));
            assertEquals("10.0000/00000", entry.getField(StandardField.DOI).orElse(null));
            assertEquals("Journal Title", entry.getField(StandardField.JOURNAL).orElse(null));
            assertEquals("1", entry.getField(StandardField.VOLUME).orElse(null));
            assertEquals("1", entry.getField(StandardField.ISSUE).orElse(null));
            assertEquals("1-10", entry.getField(StandardField.PAGES).orElse(null));
            assertEquals("2021", entry.getField(StandardField.YEAR).orElse(null));
            System.out.println("Test Successful!");
        }
    }
}
