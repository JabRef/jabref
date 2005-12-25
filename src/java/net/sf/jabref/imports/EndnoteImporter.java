package net.sf.jabref.imports;

import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.AuthorList;

/**
 * Importer for the Refer/Endnote format.
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class EndnoteImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
    return "Refer/Endnote";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "refer";
    }
    
    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

    // Our strategy is to look for the "%A *" line.
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    Pattern pat1 = Pattern
        .compile("%A .*");
    String str;
    while ((str = in.readLine()) != null){
        if (pat1.matcher(str).find())
        return true;
    }
    return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List importEntries(InputStream stream) throws IOException {
    ArrayList bibitems = new ArrayList();
    StringBuffer sb = new StringBuffer();
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    String ENDOFRECORD = "__EOREOR__";

    String str;
    boolean first = true;
    while ((str = in.readLine()) != null){
        str = str.trim();
        // if(str.equals("")) continue;
        if (str.indexOf("%0") == 0){
        if (first){
            first = false;
        }else{
            sb.append(ENDOFRECORD);
        }
        sb.append(str);
        }else sb.append(str);
        sb.append("\n");
    }

    String[] entries = sb.toString().split(ENDOFRECORD);
    HashMap hm = new HashMap();
    String Author = "", Type = "", Editor = "";
    for (int i = 0; i < entries.length; i++){
        hm.clear();
        Author = "";
        Type = "";
        Editor = "";
	boolean IsEditedBook = false;
        String[] fields = entries[i].substring(1).split("\n%");
        //String lastPrefix = "";
        for (int j = 0; j < fields.length; j++){
        if (fields[j].length() < 3) continue;

        /*
           * Details of Refer format for Journal Article and Book:
           *
           * Generic Ref Journal Article Book Code Author %A Author Author Year %D
           * Year Year Title %T Title Title Secondary Author %E Series Editor
           * Secondary Title %B Journal Series Title Place Published %C City
           * Publisher %I Publisher Volume %V Volume Volume Number of Volumes %6
           * Number of Volumes Number %N Issue Pages %P Pages Number of Pages
           * Edition %7 Edition Subsidiary Author %? Translator Alternate Title %J
           * Alternate Journal Label %F Label Label Keywords %K Keywords Keywords
           * Abstract %X Abstract Abstract Notes %O Notes Notes
           */

        String prefix = fields[j].substring(0, 1);
        String val = fields[j].substring(2);
        if (prefix.equals("A")){
            if (Author.equals("")) Author = val;
            else Author += " and " + val;
        }else if (prefix.equals("E")){
            if (Editor.equals("")) Editor = val;
            else Editor += " and " + val;
        }else if (prefix.equals("T")) hm.put("title", Util
                             .putBracesAroundCapitals(val));
        else if (prefix.equals("0")){
            if (val.indexOf("Journal") == 0) Type = "article";
            else if ((val.indexOf("Book Section") == 0)) Type = "incollection";
            else if ((val.indexOf("Book") == 0)) Type = "book";
            else if (val.indexOf("Edited Book") == 0) {
                Type = "book";
                IsEditedBook = true;
            }else if (val.indexOf("Conference") == 0) // Proceedings
            Type = "inproceedings";
            else if (val.indexOf("Report") == 0) // Techreport
            Type = "techreport";
            else if (val.indexOf("Review") == 0)
                Type = "article";
            else if (val.indexOf("Thesis") == 0)
                Type = "phdthesis";
            else Type = "misc"; //
        }else if (prefix.equals("7")) hm.put("edition", val);
        else if (prefix.equals("C")) hm.put("address", val);
        else if (prefix.equals("D")) hm.put("year", val);
        else if (prefix.equals("8")) hm.put("date", val);
        else if (prefix.equals("J")){
            // "Alternate journal. Let's set it only if no journal
            // has been set with %B.
            if (hm.get("journal") == null) hm.put("journal", val);
        }else if (prefix.equals("B")){
            // This prefix stands for "journal" in a journal entry, and
            // "series" in a book entry.
            if (Type.equals("article")) hm.put("journal", val);
            else if (Type.equals("book") || Type.equals("inbook")) hm.put(
                                          "series", val);
            else /* if (Type.equals("inproceedings")) */
            hm.put("booktitle", val);
        }else if (prefix.equals("I")) {
            if (Type.equals("phdthesis"))
                hm.put("school", val);
            else
                 hm.put("publisher", val);
        }
            // replace single dash page ranges (23-45) with double dashes (23--45):
        else if (prefix.equals("P")) hm.put("pages", val.replaceAll("([0-9]) *- *([0-9])","$1--$2"));
        else if (prefix.equals("V")) hm.put("volume", val);
        else if (prefix.equals("N")) hm.put("number", val);
        else if (prefix.equals("U")) hm.put("url", val);
        else if (prefix.equals("O")) hm.put("note", val);
        else if (prefix.equals("K")) hm.put("keywords", val);
        else if (prefix.equals("X")) hm.put("abstract", val);
        else if (prefix.equals("9")){
            //Util.pr(val);
            if (val.indexOf("Ph.D.") == 0) Type = "phdthesis";
            if (val.indexOf("Masters") == 0) Type = "mastersthesis";
        }else if (prefix.equals("F")) hm.put(Globals.KEY_FIELD, Util
                             .checkLegalKey(val));
        }

        // For Edited Book, EndNote puts the editors in the author field.
        // We want them in the editor field so that bibtex knows it's an edited book
        if (IsEditedBook && Editor.equals("")) {
           Editor = Author;
           Author = "";
        }

        //fixauthorscomma
        if (!Author.equals("")) hm.put("author", AuthorList.fixAuthor_lastNameFirst(Author));
        if (!Editor.equals("")) hm.put("editor", AuthorList.fixAuthor_lastNameFirst(Editor));
        BibtexEntry b = new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals
                        .getEntryType(Type)); // id assumes an existing database so don't
        // create one here
        b.setField(hm);
        //if (hm.isEmpty())
        if (b.getAllFields().length > 0) bibitems.add(b);

    }
    return bibitems;

    }

}
