package org.jabref.logic.plaintextparser;


import org.jabref.model.entry.BibEntry;

/**
 * This class is used to help making new entries faster by parsing a string.
 * An external parser called grobid is being used for that.
 */
public class ParserPipeline {

  private static BibEntry bibEntry;

  /**
   * Takes a whole String and filters the specific fields for the entry which is done
   * by an external parser.
   * @param plainText Reference text to be parsed.
   */
  public static void parseRefText(String plainText) {
    GrobidClient gc = new GrobidClient();
    System.out.println(gc.sendReference(plainText));
    /*
    TODO: IMPLEMENT PARSER METHOD // IMPORT PARSER
     */
  }

  /**
   * Creates a new entry and fills the fields with the results of the parser, but only
   * there where the parser has found something. If the entry already exists then
   * nothing the function will immediately return.
   * @param entry New entry.
   */
  public static void createNewEntry(String bibtexString) {
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

  public static BibEntry getBibEntry() {
    return bibEntry;
  }


}
