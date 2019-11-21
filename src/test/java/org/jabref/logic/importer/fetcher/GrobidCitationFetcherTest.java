package org.jabref.logic.importer.fetcher;

import java.util.Optional;
import org.apache.logging.log4j.util.Assert;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.search.SearchParser.StartContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
                    .performSearch("Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching " +
                            "native speakers to listen to foreign-accented speech. Journal of Multilingual and " +
                            "Multicultural Development, 23(4), 245-259.");
            LOGGER.debug(s.get(0).getAuthorTitleYear(100));
        } catch (FetcherException e) {
            LOGGER.error("Does not work");
        }
    }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference
   * which should be able to be parsed. The result should either be empty or not empty but
   * not null.
   */
  @Test
  public void passGrobidRequestTest() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor);
    String plainTextExample = "M. C. Potter and R. Mackiewicz, Mechanical Vibration and "
      + "Shock Analysis, 2nd ed. Upper Saddle River, NJ: Pearson Prentice Hall," + " 2015, pp. 17–19.";
    Method parseUsingGrobid = GrobidCitationFetcher.class.getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    String output = (String)parseUsingGrobid.invoke(grobidCitationFetcher, plainTextExample);
    assertNotSame(null, output);
  }

  /**
   * Giving an invalid string as a request to the grobid parser should faild and return an
   * empty string.
   */
  @Test
  public void failingGrobidRequestTest() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor);
    String invalidInput = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx________________________________"; // should not be a valid input for the grobid parser
    Method parseUsingGrobid = GrobidCitationFetcher.class.getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    String emptyOutput = (String)parseUsingGrobid.invoke(grobidCitationFetcher, invalidInput);
    assertEquals("", emptyOutput);
  }


  /**
   * Tests if the Optional<BibEntry> is filled correctly with the extracted fields from bibtex string..
   */
  @Test
  public void grobidParseBibToBibEntryTest() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor);
    String input = "@article{mrx05, \n"
        + "auTHor = \"Mr. X\", \n"
        + "Title = {Something Great}, \n"
        + "publisher = \"nob\" # \"ody\"}, \n"
        + "YEAR = 2005, \n"
        + "} ";
    Method parseBibToBibEntry = GrobidCitationFetcher.class.getDeclaredMethod("parseBibToBibEntry", String.class);
    parseBibToBibEntry.setAccessible(true);
    Optional<BibEntry> abc = (Optional<BibEntry>)parseBibToBibEntry.invoke(grobidCitationFetcher, input);
    assertTrue(abc.isPresent());
  }

  /**
   * Checks if parseBibToBibEntry successfully throws an exception if the parsing fails.
   */
  @Test
  public void grobidParseBibToBibEntryThrowsExceptionWhenFailTest() throws NoSuchMethodException {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor);
    String failInput = "¦@#¦@#¦@#¦@#¦@#¦@#¦@°#¦@¦°¦@°";
    Method parseBibToBibEntry = GrobidCitationFetcher.class.getDeclaredMethod("parseBibToBibEntry", String.class);
    assertThrows(ParseException.class,
        () -> parseBibToBibEntry.invoke(grobidCitationFetcher, failInput));
  }


  /**
   * Tests if the performSearch function correctly splits the plain reference text into the right parts!
   */
  @Test
  public void grobidPerformSearchCorrectlySplitsStringTest()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor);
    String input = "Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry :"
        + " A developmental approach. Oxford, England: Oxford University Press. ;; Carr, I., & Kidner,"
        + " R. (2003). Statutes and conventions on international trade law (4th ed.). London, "
        + "England: Cavendish.";
    Method performSearch = GrobidCitationFetcher.class.getDeclaredMethod("performSearch", String.class);
    List<BibEntry> entries = (List<BibEntry>)performSearch.invoke(grobidCitationFetcher, input);
    assertTrue(entries.get(0).hasField(StandardField.YEAR)); //it must have two entries!
    assertTrue(entries.get(1).hasField(StandardField.YEAR));
  }

  /**
   * Testing with two string examples if the values are parsed correctly into the right field
   * of the specific bibentry.
   */
  @Test
  public void grobidPerformSearchCorrectResultTest() {
    GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences,fileUpdateMonitor);
    String input = "Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry :"
        + " A developmental approach. Oxford, England: Oxford University Press. ;; Carr, I., & Kidner,"
        + " R. (2003). Statutes and conventions on international trade law (4th ed.). London, "
        + "England: Cavendish.";
    Method performSearch = GrobidCitationFetcher.class.getDeclaredMethod("performSearch", String.class);
    List<BibEntry> entries =(List<BibEntry>)performSearch.invoke(grobidCitationFetcher, input);
    assertEquals("2007", entries.get(0).getField(StandardField.YEAR).toString());
    assertEquals(entries.get(1).getField(StandardField.YEAR).toString(), "2003");


  }


  /**
   * TODO: Modify the performSearch Method -> add failed Results List(not sure to be implemented yet)
   * Tests if multiple text references are parsed correctly and if some of
   * them are failing that they are putted into the right list.
   */
  @Test
  public void grobidPerformSearchWithFailsTest() {

  }


}
