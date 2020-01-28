package org.jabref.logic.importer.fetcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.importer.util.GrobidServiceException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Used to test the whole logic of the external parser anystyle. Therefore any possible outgoing
 * when the user is working with the parser is going to be tested in this class.
 */

public class GrobidCitationFetcherTest {

  static ImportFormatPreferences importFormatPreferences;
  static FileUpdateMonitor fileUpdateMonitor;
  static GrobidCitationFetcher grobidCitationFetcher;
  static GrobidService grobidService;
  static String example1 = "Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.";
  static String example1AsBibtex = "@article{-1,\n" +
      "author\t=\t\"T Derwing and M Rossiter and M Munro\",\n" +
      "title\t=\t\"Teaching native speakers to listen to foreign-accented speech\",\n" +
      "journal\t=\t\"Journal of Multilingual and Multicultural Development\",\n" +
      "year\t=\t\"2002\",\n" +
      "pages\t=\t\"245--259\",\n" +
      "volume\t=\t\"23\",\n" +
      "number\t=\t\"4\"\n" +
      "}";
  static String example2 = "Thomas, H. K. (2004). Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation). University of Colorado, Boulder.";
  static String example2AsBibtex = "@misc{-1,\n" +
      "author\t=\t\"H Thomas\",\n" +
      "title\t=\t\"Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation)\",\n"
      +
      "year\t=\t\"2004\",\n" +
      "address\t=\t\"Boulder\"\n" +
      "}";
  static String example3 = "Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry : A developmental approach. Oxford, England: Oxford University Press.";
  static String example3AsBibtex = "@misc{-1,\n" +
      "author\t=\t\"J Turk and P Graham and F Verhulst\",\n" +
      "title\t=\t\"Child and adolescent psychiatry : A developmental approach\",\n" +
      "publisher\t=\t\"Oxford University Press\",\n" +
      "year\t=\t\"2007\",\n" +
      "address\t=\t\"Oxford, England\"\n" +
      "}";
  static String example4 = "Carr, I., & Kidner, R. (2003). Statutes and conventions on international trade law (4th ed.). London, England: Cavendish.";
  static String example4AsBibtex = "@article{-1,\n" +
      "author\t=\t\"I Carr and R Kidner\",\n" +
      "booktitle\t=\t\"Statutes and conventions on international trade law\",\n" +
      "publisher\t=\t\"Cavendish\",\n" +
      "year\t=\t\"2003\",\n" +
      "address\t=\t\"London, England\"\n" +
      "}";
  static String invalidInput1 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx________________________________";
  static String invalidInput2 = "¦@#¦@#¦@#¦@#¦@#¦@#¦@°#¦@¦°¦@°";
  static String defaultReturnValue = "@misc{-1,\n\n}";

  @BeforeAll
  public static void setup() throws GrobidServiceException {
    importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    fileUpdateMonitor = new DummyFileUpdateMonitor();
    grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences, fileUpdateMonitor,
        grobidService);
    grobidService = mock(GrobidService.class, Answers.RETURNS_DEEP_STUBS);
    when(grobidService.processCitation(example1, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(example1AsBibtex);
    when(grobidService.processCitation(example2, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(example2AsBibtex);
    when(grobidService.processCitation(example3, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(example3AsBibtex);
    when(grobidService.processCitation(example4, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(example4AsBibtex);
    when(grobidService
        .processCitation(invalidInput1, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(defaultReturnValue);
    when(grobidService
        .processCitation(invalidInput2, GrobidService.ConsolidateCitations.WITH_METADATA))
        .thenReturn(defaultReturnValue);
  }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference which should be
   * able to be parsed. The result should either be empty or not empty but not null.
   */
  @Test
  public void passGrobidRequestTestOne()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class
        .getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    assertEquals(example1AsBibtex, parseUsingGrobid.invoke(grobidCitationFetcher, example1));
  }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference which should be
   * able to be parsed. The result should either be empty or not empty but not null.
   */
  @Test
  public void passGrobidRequestTestTwo()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class
        .getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    assertEquals(example2AsBibtex, parseUsingGrobid.invoke(grobidCitationFetcher, example2));
  }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference which should be
   * able to be parsed. The result should either be empty or not empty but not null.
   */
  @Test
  public void passGrobidRequestTestThree()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class
        .getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    assertEquals(example3AsBibtex, parseUsingGrobid.invoke(grobidCitationFetcher, example3));
  }

  /**
   * Checks if the Grobid parser returns a String if the input is a text reference which should be
   * able to be parsed. The result should either be empty or not empty but not null.
   */
  @Test
  public void passGrobidRequestTestFour()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class
        .getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    assertEquals(example4AsBibtex, parseUsingGrobid.invoke(grobidCitationFetcher, example4));
  }

  /**
   * Giving an invalid string as a request to the grobid parser, should fail and return an empty
   * bibtex String.
   */
  @Test
  public void failingGrobidRequestTest()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseUsingGrobid = GrobidCitationFetcher.class
        .getDeclaredMethod("parseUsingGrobid", String.class);
    parseUsingGrobid.setAccessible(true);
    assertEquals(defaultReturnValue, parseUsingGrobid.invoke(grobidCitationFetcher, invalidInput1));
    assertEquals(defaultReturnValue, parseUsingGrobid.invoke(grobidCitationFetcher, invalidInput2));
  }

  /**
   * Tests if the Optional<BibEntry> is filled correctly with the extracted fields from bibtex
   * string..
   */
  @Test
  public void grobidParseBibToBibEntryTest()
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method parseBibToBibEntry = GrobidCitationFetcher.class
        .getDeclaredMethod("parseBibToBibEntry", String.class);
    parseBibToBibEntry.setAccessible(true);
    Optional<BibEntry> bibEntry = (Optional<BibEntry>) parseBibToBibEntry
        .invoke(grobidCitationFetcher, example1AsBibtex);
    assertTrue(bibEntry.isPresent());
    assertEquals(bibEntry.get().getField(StandardField.AUTHOR).get(),
        "T Derwing and M Rossiter and M Munro");
    assertEquals(bibEntry.get().getField(StandardField.TITLE).get(),
        "Teaching native speakers to listen to foreign-accented speech");
    assertEquals(bibEntry.get().getField(StandardField.JOURNAL).get(),
        "Journal of Multilingual and Multicultural Development");
    assertEquals(bibEntry.get().getField(StandardField.YEAR).get(), "2002");
  }

  /**
   * Checks if parseBibToBibEntry creates a empty BibEntry on empty response
   */
  @Test
  public void grobidParseBibToBibEntryReturnsEmptyOptionalWhenFailedTest()
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    Method parseBibToBibEntry = GrobidCitationFetcher.class
        .getDeclaredMethod("parseBibToBibEntry", String.class);
    parseBibToBibEntry.setAccessible(true);
    Optional<BibEntry> bibEntry = (Optional<BibEntry>) parseBibToBibEntry
        .invoke(grobidCitationFetcher, defaultReturnValue);
    assertEquals(Optional.empty(), bibEntry);
    assertEquals(Optional.empty(), bibEntry.get().getField(StandardField.AUTHOR));
    assertEquals(Optional.empty(), bibEntry.get().getField(StandardField.TITLE));
    assertEquals(Optional.empty(), bibEntry.get().getField(StandardField.YEAR));
    assertEquals(Optional.empty(), bibEntry.get().getField(StandardField.JOURNAL));
  }

  /**
   * When the user runs the program without passing anything, no BibEntry is generated (The Optional
   * should be empty)
   */
  @Test
  public void grobidParseBibToBibEntryReturnsNoBibEntryWhenPassingInvalidInputTest()
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    Method parseBibToBibEntry = GrobidCitationFetcher.class
        .getDeclaredMethod("parseBibToBibEntry", String.class);
    parseBibToBibEntry.setAccessible(true);
    Optional<BibEntry> bibEntry = (Optional<BibEntry>) parseBibToBibEntry
        .invoke(grobidCitationFetcher, "");
    assertFalse(bibEntry.isPresent());
  }


  /**
   * Tests if the performSearch function correctly splits the plain reference text into the right
   * parts.
   */
  @Test
  public void grobidPerformSearchCorrectlySplitsStringTest() throws FetcherException {
    List<BibEntry> entries = grobidCitationFetcher.performSearch(example3 + ";;" + example4);
    assertEquals(List.of(example3, example4), entries);
  }

  /**
   * Testing with two string examples if the values are parsed correctly into the right field of the
   * specific bibentry.
   */
  @Test
  public void grobidPerformSearchCorrectResultTest() throws FetcherException {
    List<BibEntry> entries = grobidCitationFetcher.performSearch(example2 + ";;" + example3);
    assertTrue(entries.get(0).getField(StandardField.TITLE).get()
        .equals("Child and adolescent psychiatry : A developmental approach")
        || entries.get(1).getField(StandardField.TITLE).get()
        .equals("Child and adolescent psychiatry : A developmental approach"));
    assertTrue(entries.get(0).getField(StandardField.YEAR).get().equals("2004")
        || entries.get(1).getField(StandardField.YEAR).get().equals("2004"));
  }


  /**
   * Tests if empty Strings throw FetcherException
   */
  @Test
  public void grobidPerformSearchWithEmptyStringsTest() {
    Assertions.assertThrows(FetcherException.class, () -> {
      grobidCitationFetcher.performSearch("   ;;   ");
    });
  }

  /**
   * Tests if failed parsing result are added to the failedEntries ArrayList.
   */
  @Test
  public void grobidPerformSearchWithFailsTest() throws FetcherException {
    List<BibEntry> entries = grobidCitationFetcher
        .performSearch(invalidInput1 + ";;" + invalidInput2);
    assertEquals(entries.get(0).toString(), defaultReturnValue);
    assertEquals(entries.get(1).toString(), defaultReturnValue);
  }

}
