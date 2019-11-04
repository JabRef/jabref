package org.jabref.logic.plaintextparser;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * This class is used to help making new entries faster by parsing a string.
 * An external parser called anystyle is being used for that.
 */
public class ParserPipeline {

  private BibEntry bibEntry;
  private BibDatabase bibDatabase;
  private static String referenceText;
  private static String parserResults;
  private static StandardField[] fields; // contains the fields which the parser filtered.



  /**
   * Takes a whole String and filters the specific fields for the entry which is done
   * by an external parser.
   * @param refText Reference text to be parsed.
   */
  public static void parseRefText(String refText) {
    /*
    TODO: IMPLEMENT PARSER METHOD // IMPORT ANYSTYLE PARSER
     */
  }

  /**
   * Creates a new entry and fills the fields with the results of the parser, but only
   * there where the parser has found something. If the entry already exists then
   * nothing the function will immediately return.
   * @param entry New entry.
   */
  public static void createNewEntry(BibEntry entry) {
    /*
    if(bibDatabase.containsEntryWithId(entry.getId())) {
    return;
    }
    else{
    for (StandardField field : fields) {
      entry.setField(field, parserResults);
    }
    bibDatabase.insertEntry(entry);
    }
     */
    //TODO: Finish it!

  }





}
