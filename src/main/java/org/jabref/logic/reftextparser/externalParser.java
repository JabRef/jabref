package org.jabref.logic.reftextparser;

import org.jabref.model.entry.BibEntry;

/**
 * This class is used to help making new entries faster by parsing a string.
 * An external parser called anystyle is being used for that.
 */
public class externalParser {

  private BibEntry bibEntry;
  private String referenceText;


  /**
   * Takes a whole String and filters the specific fields for the entry which is done
   * by an external parser. If the entry is already available then no changes are made.
   * @param refText Reference text to be parsed.
   */
  public static void parseRefText(String refText) {
    /*
    TODO: IMPLEMENT PARSER METHOD // IMPORT ANYSTYLE PARSER
     */
  }

  public static void createNewEntry(BibEntry entry) {

  }





}
