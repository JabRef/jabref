package net.sf.jabref.imports;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.AuthorList;

import java.util.regex.Pattern;
import net.sf.jabref.BibtexFields;

/**
 * INSPEC format importer.
 */
public class InspecImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
        return "INSPEC";
    }

  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#getCLIId()
   */
  public String getCLIId() {
    return "inspec";
  }

  /**
   * Check whether the source is in the correct format for this importer.
   */
  public boolean isRecognizedFormat(InputStream stream)
    throws IOException {
    // Our strategy is to look for the "PY <year>" line.
    BufferedReader in =
      new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    //Pattern pat1 = Pattern.compile("PY:  \\d{4}");
    Pattern pat1 = Pattern.compile("Record.*INSPEC.*");

    //was PY \\\\d{4}? before
    String str;

    while ((str = in.readLine()) != null) {
      //Inspec and IEEE seem to have these strange " - " between key and value
      //str = str.replace(" - ", "");
      //System.out.println(str);

      if (pat1.matcher(str).find())
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
            if (str.length() < 2) continue;
            if (str.indexOf("Record") == 0) sb.append("__::__").append(str);
            else
            sb.append("__NEWFIELD__").append(str);
        }
        in.close();
        String[] entries = sb.toString().split("__::__");
        String Type = "";
        HashMap<String, String> h = new HashMap<String, String>();
        for (int i = 0; i < entries.length; i++){
            if (entries[i].indexOf("Record") != 0) continue;
            h.clear();

            String[] fields = entries[i].split("__NEWFIELD__");
            for (int j = 0; j < fields.length; j++){
                //System.out.println(fields[j]);
                String s = fields[j];
                String f3 = s.substring(0, 2);
                String frest = s.substring(5);
                if (f3.equals("TI")) h.put("title", frest);
                else if (f3.equals("PY")) h.put("year", frest);
                else if (f3.equals("AU")) h.put("author",
                                                AuthorList.fixAuthor_lastNameFirst(frest.replaceAll(",-", ", ").replaceAll(
                                                        ";", " and ")));
                else if (f3.equals("AB")) h.put("abstract", frest);
                else if (f3.equals("ID")) h.put("keywords", frest);
                else if (f3.equals("SO")){
                    int m = frest.indexOf(".");
                    if (m >= 0){
                        String jr = frest.substring(0, m);
                        h.put("journal", jr.replaceAll("-", " "));
                        frest = frest.substring(m);
                        m = frest.indexOf(";");
                        if (m >= 5){
                            String yr = frest.substring(m - 5, m);
                            h.put("year", yr);
                            frest = frest.substring(m);
                            m = frest.indexOf(":");
                            if (m >= 0){
                                String pg = frest.substring(m + 1).trim();
                                h.put("pages", pg);
                                h.put("volume", frest.substring(1, m));
                            }
                        }
                    }

                }else if (f3.equals("RT")){
                    frest = frest.trim();
                    if (frest.equals("Journal-Paper")) Type = "article";
                    else if (frest.equals("Conference-Paper")
                             || frest.equals("Conference-Paper; Journal-Paper")) Type = "inproceedings";
                    else Type = frest.replaceAll(" ", "");
                }
            }
            BibtexEntry b = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, Globals
                                            .getEntryType(Type)); // id assumes an existing database so don't
            // create one here
            b.setField(h);

            bibitems.add(b);

        }

        return bibitems;
    }
}


