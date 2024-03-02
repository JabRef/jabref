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
    public void preferredCitationTest() throws Exception {
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
                  type: misc
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
            List<BibEntry> actualEntries = result.getDatabase().getEntries();

            BibEntry expectedEntry = new BibEntry()
                    .withField(StandardField.AUTHOR, "Mona Lisa and Hew Bot")
                    .withField(StandardField.TITLE, "My awesome research software")
                    .withField(StandardField.DOI, "10.0000/00000")
                    .withField(StandardField.JOURNAL, "Journal Title")
                    .withField(StandardField.VOLUME, "1")
                    .withField(StandardField.URL, "https://github.com/github-linguist/linguist")
                    .withField(StandardField.VERSION, "2.0.4")
                    .withField(StandardField.ISSUE, "1")
                    .withField(StandardField.YEAR, "2021")
                    .withField(StandardField.PAGES, "1-10");

            assertEquals(List.of(expectedEntry), actualEntries);
        }
    }
}
