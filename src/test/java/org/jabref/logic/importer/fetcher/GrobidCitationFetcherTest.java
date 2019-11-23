package org.jabref.logic.importer.fetcher;

import java.util.Optional;
import org.apache.logging.log4j.util.Assert;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.search.SearchParser.StartContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;
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
import static org.mockito.Mockito.when;

/**
 * Used to test the whole logic of the external parser anystyle.
 * Therefore any possible outgoing when the user is working with the parser is going to be
 * tested in this class.
 */

public class GrobidCitationFetcherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcherTest.class);

    ImportFormatPreferences importFormatPreferences;
    FileUpdateMonitor fileUpdateMonitor;
    GrobidCitationFetcher grobidCitationFetcher;
    GrobidService grobidService;
    String example1 = "Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching \" +\n"
        + "                            \"native speakers to listen to foreign-accented speech. Journal of Multilingual and \" +\n"
        + "                            \"Multicultural Development, 23(4), 245-259.";
    String example2 = "M. C. Potter and R. Mackiewicz, Mechanical Vibration and \"\n"
        + "      + \"Shock Analysis, 2nd ed. Upper Saddle River, NJ: Pearson Prentice Hall,\" + \" 2015, pp. 17–19.";
    String example3 = "\"Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry :\"\n"
        + "      + \" A developmental approach. Oxford, England: Oxford University Press. ;; Carr, I., & Kidner,\"\n"
        + "      + \" R. (2003). Statutes and conventions on international trade law (4th ed.). London, \"\n"
        + "      + \"England: Cavendish.\"";
  String bibEntryexample = "@article{mrx05, \n"
      + "auTHor = \"Mr. X\", \n"
      + "Title = {Something Great}, \n"
      + "publisher = \"nob\" # \"ody\"}, \n"
      + "YEAR = 2005, \n"
      + "} ";
  String invalidInput1 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx________________________________";
  String invalidInput2 = "¦@#¦@#¦@#¦@#¦@#¦@#¦@°#¦@¦°¦@°";



    @BeforeAll
    public void setup() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        fileUpdateMonitor = new DummyFileUpdateMonitor();
        grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor, JabRefPreferences.getInstance());
        grobidService = mock(GrobidService.class, Answers.RETURNS_DEEP_STUBS);
        grobidCitationFetcher.setGrobidService(grobidService);
        //when(grobidService.processCitation(example1)).thenReturn();
    }

    /**
     * Tests the base functionality of the parser is working by taking some example
     * strings and parses them with the external parser and checks if the corresponding fields
     * are filled in correctly.
     */
    @Test
    public void singleTextResourceParseTest() {
        try {
            List<BibEntry> s = grobidCitationFetcher
                    .performSearch(example1);
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
    Method parseUsingGrobid = GrobidCitationFetcher.class.getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    String output = (String)parseUsingGrobid.invoke(grobidCitationFetcher, example2);
    assertNotSame(null, output);
  }

  /**
   * Giving an invalid string as a request to the grobid parser should faild and return an
   * empty string.
   */
  @Test
  public void failingGrobidRequestTest() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class.getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    String emptyOutput = (String)parseUsingGrobid.invoke(grobidCitationFetcher, invalidInput1);
    assertEquals("", emptyOutput);
  }


  /**
   * Tests if the Optional<BibEntry> is filled correctly with the extracted fields from bibtex string..
   */
  @Test
  public void grobidParseBibToBibEntryTest() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseBibToBibEntry = GrobidCitationFetcher.class.getDeclaredMethod("parseBibToBibEntry", String.class);
    parseBibToBibEntry.setAccessible(true);
    Optional<BibEntry> abc = (Optional<BibEntry>)parseBibToBibEntry.invoke(grobidCitationFetcher, bibEntryexample);
    assertTrue(abc.isPresent());
  }

  /**
   * Checks if parseBibToBibEntry successfully throws an exception if the parsing fails.
   */
  @Test
  public void grobidParseBibToBibEntryThrowsExceptionWhenFailTest() throws NoSuchMethodException {
    Method parseBibToBibEntry = GrobidCitationFetcher.class.getDeclaredMethod("parseBibToBibEntry", String.class);
    assertThrows(ParseException.class,
        () -> parseBibToBibEntry.invoke(grobidCitationFetcher, invalidInput2));
  }


  /**
   * Tests if the performSearch function correctly splits the plain reference text into the right parts!
   */
  @Test
  public void grobidPerformSearchCorrectlySplitsStringTest()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Method performSearch = GrobidCitationFetcher.class.getDeclaredMethod("performSearch", String.class);
    List<BibEntry> entries = (List<BibEntry>)performSearch.invoke(grobidCitationFetcher, example3);
    assertTrue(entries.get(0).hasField(StandardField.YEAR)); //it must have two entries!
    assertTrue(entries.get(1).hasField(StandardField.YEAR));
  }

  /**
   * Testing with two string examples if the values are parsed correctly into the right field
   * of the specific bibentry.
   */
  @Test
  public void grobidPerformSearchCorrectResultTest() throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    Method performSearch = GrobidCitationFetcher.class.getDeclaredMethod("performSearch", String.class);
    List<BibEntry> entries =(List<BibEntry>)performSearch.invoke(grobidCitationFetcher, example3);
    assertEquals("2007", entries.get(0).getField(StandardField.YEAR).toString());
    assertEquals(entries.get(1).getField(StandardField.YEAR).toString(), "2003");


  }


  /**
   * Tests if failed parsing result are added to the failedEntries ArrayList.
   */
  @Test
  public void grobidPerformSearchWithFailsTest() {

  }



}
