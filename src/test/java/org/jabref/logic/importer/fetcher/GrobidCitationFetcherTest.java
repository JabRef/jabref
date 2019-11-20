package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Used to test the whole logic of the external parser anystyle.
 * Therefore any possible outgoing when the user is working with the parser is going to be
 * tested in this class.
 */

public class GrobidCitationFetcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcherTest.class);

    ImportFormatPreferences importFormatPreferences;
    FileUpdateMonitor fileUpdateMonitor;

    @BeforeEach
    public void setup() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        fileUpdateMonitor = new DummyFileUpdateMonitor();
    }

    /**
     * Tests the base functionality of the parser is working by taking some example
     * strings and parses them with the external parser and checks if the corresponding fields
     * are filled in correctly.
     */
    @Test
    public void singleTextResourceParseTest() {
        try {
            List<BibEntry> s = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor)
                    .performSearch("Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.");
            LOGGER.debug(s.get(0).getAuthorTitleYear(100));
        } catch (FetcherException e) {
            LOGGER.error("Does not work");
        }
    }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference
   * which should be able to be parsed.
   */
  @Test
  public void passGrobidRequestTest() {
  String plainTextExample = "M. C. Potter and R. Mackiewicz, Mechanical Vibration and "
      + "Shock Analysis, 2nd ed. Upper Saddle River, NJ: Pearson Prentice Hall,"
      + " 2015, pp. 17–19.";

  }


  /**
   * Tests if multiple text references are parsed correctly if every input
   * is valid.
   */
  @Test
  public void grobidValidPerformSearchTest() {

  }

  /**
   * TODO: Modify the performSearch Method -> add failed Results List
   * Tests if multiple text references are parsed correctly and if some of
   * them are failing that they are putted into the right list.
   */
  @Test
  public void grobidPerformSearchWithFailsTest() {

  }


}
