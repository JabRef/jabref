package net.sf.jabref.imports;

import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexFields;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
    return "RIS";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "ris";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

    // Our strategy is to look for the "AU  - *" line.
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    Pattern pat1 = Pattern.compile("AU  - .*"),
        pat2 = Pattern.compile("A1  - .*"),
        pat3 = Pattern.compile("A2  - .*");


    String str;
    while ((str = in.readLine()) != null){
        if (pat1.matcher(str).find() || pat2.matcher(str).find() || pat3.matcher(str).find())
            return true;
    }

    return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {
    ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
    StringBuffer sb = new StringBuffer();
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    String str;
    while ((str = in.readLine()) != null){
        sb.append(str);
        sb.append("\n");
    }
    String[] entries = sb.toString().split("ER  -");

    for (int i = 0; i < entries.length; i++){

        if (entries[i].trim().length() == 0)
            continue;
        
            String type = "", author = "", editor = "", startPage = "", endPage = "",
                comment = "";
            HashMap<String, String> hm = new HashMap<String, String>();


        String[] fields = entries[i].split("\n");

        for (int j = 0; j < fields.length; j++){
                StringBuffer current = new StringBuffer(fields[j]);
                boolean done = false;
                while (!done && (j < fields.length-1)) {
                    if ((fields[j+1].length() >= 6) && !fields[j+1].substring(2, 6).equals("  - ")) {
                        if ((current.length() > 0)
                                && !Character.isWhitespace(current.charAt(current.length()-1))
                                && !Character.isWhitespace(fields[j+1].charAt(0)))
                            current.append(' ');
                        current.append(fields[j+1]);
                        j++;
                    } else
                        done = true;
                }
                String entry = current.toString();
        if (entry.length() < 6) continue;
        else{
            String lab = entry.substring(0, 2);
            String val = entry.substring(6).trim();
            if (lab.equals("TY")){
            if (val.equals("BOOK")) type = "book";
            else if (val.equals("JOUR") || val.equals("MGZN")) type = "article";
                        else if (val.equals("THES")) type = "phdthesis";
                        else if (val.equals("UNPB")) type = "unpublished";
                        else if (val.equals("RPRT")) type = "techreport";
                        else if (val.equals("CONF")) type = "inproceedings";
                        else if (val.equals("CHAP")) type = "incollection";//"inbook";

            else type = "other";
            }else if (lab.equals("T1") || lab.equals("TI")) hm.put("title", val);//Title
            // =
            // val;
            else if (lab.equals("T2") || lab.equals("T3") || lab.equals("BT")) {
                hm.put("booktitle", val);
            }
            else if (lab.equals("AU") || lab.equals("A1")) {
                if (author.equals("")) // don't add " and " for the first author
                    author = val;
                else author += " and " + val;
            }
	        else if (lab.equals("A2")){
                if (editor.equals("")) // don't add " and " for the first editor
                    editor = val;
                else editor += " and " + val;
            }
		   	else if (lab.equals("JA") || lab.equals("JF") || lab.equals("JO")) {
                if (type.equals("inproceedings"))
                    hm.put("booktitle", val);
                else
                    hm.put("journal", val);
            }

            else if (lab.equals("SP")) startPage = val;
            else if (lab.equals("PB")) {
                if (type.equals("phdthesis"))
                    hm.put("school", val);
                else
                    hm.put("publisher", val);
            }
            else if (lab.equals("AD") || lab.equals("CY"))
                hm.put("address", val);
            else if (lab.equals("EP")) endPage = val;
                    else if (lab.equals("SN"))
                        hm.put("issn", val);
            else if (lab.equals("VL")) hm.put("volume", val);
            else if (lab.equals("IS")) hm.put("number", val);
            else if (lab.equals("N2") || lab.equals("AB")) {
                String oldAb = hm.get("abstract");
                if (oldAb == null)
                    hm.put("abstract", val);
                else
                    hm.put("abstract", oldAb+"\n"+val);
            }

            else if (lab.equals("UR")) hm.put("url", val);
            else if ((lab.equals("Y1") || lab.equals("PY")) && val.length() >= 4) {
                        String[] parts = val.split("/");
                        hm.put("year", parts[0]);
                        if ((parts.length > 1) && (parts[1].length() > 0)) {
                            try {
                                int month = Integer.parseInt(parts[1]);
                                if ((month > 0) && (month <= 12)) {
                                    //System.out.println(Globals.MONTHS[month-1]);
                                    hm.put("month", "#"+Globals.MONTHS[month-1]+"#");
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                        }
                    }

            else if (lab.equals("KW")){
            if (!hm.containsKey("keywords")) hm.put("keywords", val);
            else{
                String kw = hm.get("keywords");
                hm.put("keywords", kw + ", " + val);
            }
            }
            else if (lab.equals("U1") || lab.equals("U2") || lab.equals("N1")) {
                if (comment.length() > 0)
                    comment = comment+"\n";
                comment = comment+val;
            }
            // Added ID import 2005.12.01, Morten Alver:
            else if (lab.equals("ID"))
                hm.put("refid", val);
        }
        }
        // fix authors
        if (author.length() > 0) {
            author = AuthorList.fixAuthor_lastNameFirst(author);
            hm.put("author", author);
        }
        if (editor.length() > 0) {
            editor = AuthorList.fixAuthor_lastNameFirst(editor);
            hm.put("editor", editor);
        }
        if (comment.length() > 0) {
            hm.put("comment", comment);
        }

        hm.put("pages", startPage + "--" + endPage);
        BibtexEntry b = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, Globals
                        .getEntryType(type)); // id assumes an existing database so don't

        // Remove empty fields:
        ArrayList<Object> toRemove = new ArrayList<Object>();
        for (Iterator<String> it = hm.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            String content = hm.get(key);
            if ((content == null) || (content.trim().length() == 0))
                toRemove.add(key);
        }
        for (Iterator<Object> iterator = toRemove.iterator(); iterator.hasNext();) {
            hm.remove(iterator.next());

        }
        // create one here
        b.setField(hm);

        bibitems.add(b);

    }

    return bibitems;
    }
}


