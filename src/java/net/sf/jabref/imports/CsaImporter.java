package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.sf.jabref.BibtexFields;


/**
 * Importer for records downloaded from CSA: Cambridge Scientific Abstracts
 * in full text format.  Although the same basic format is used by all CSA
 * databases, this importer has been tailored and tested to handle
 * ASFA: Aquatic Sciences and Fisheries records.
 *
 * @author John Relph
 */
public class CsaImporter extends ImportFormat {

    // local fields
    private int line;

    // pre-compiled patterns
    private final static Pattern FIELD_PATTERN =
        Pattern.compile("^([A-Z][A-Z]): ([A-Z].*)$");
    private final static Pattern VOLNOPP_PATTERN =
        Pattern.compile("[;,\\.]\\s+(\\d+[A-Za-z]?)\\((\\d+(?:-\\d+)?)\\)(?:,\\s+|:)(\\d+-\\d+)");
    private final static Pattern PAGES_PATTERN =
        Pattern.compile("[;,\\.]\\s+(?:(\\[?[vn]\\.?p\\.?\\]?)|(?:pp?\\.?\\s+)(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)|(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)(?:\\s+pp?))");
    private final static Pattern VOLUME_PATTERN =
        Pattern.compile("[;,\\.]?\\s+[vV][oO][lL]\\.?\\s+(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)");
    private final static Pattern NUMBER_PATTERN =
        Pattern.compile("[;,\\.]\\s+(?:No|no|Part|part|NUMB)\\.?\\s+([A-Z]?\\d+(?:[/-]\\d+)?)");
    private final static Pattern DATE_PATTERN =
        Pattern.compile("[;,\\.]\\s+(?:(\\d+)\\s)?(?:([A-Z][a-z][a-z])[\\.,]*\\s)?\\(?(\\d\\d\\d\\d)\\)?(?:\\s([A-Z][a-z][a-z]))?(?:\\s+(\\d+))?");
    private final static Pattern LT_PATTERN =
        Pattern.compile("\\[Lt\\]");

    // other constants
    private static final String MONS =
        "jan feb mar apr may jun jul aug sep oct nov dec";
    private static final String[] MONTHS =
        { "January", "February", "March", "April", "May", "June",
          "July", "August", "September", "October", "November", "December" };

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
        return "CSA";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "csa";
    }

    // read a line
    private String readLine(BufferedReader file) throws IOException {
        String str = file.readLine();
        if (str != null)
            line++;
        return str;
    }

    // append to the "note" field
    private void addNote(HashMap<String, String> hm, String note) {

        StringBuffer notebuf = new StringBuffer();
        if (hm.get("note") != null) {
            notebuf.append(hm.get("note"));
            notebuf.append("\n");
        }
        notebuf.append(note);
        hm.put("note", notebuf.toString());
    }

    // parse the date from the Source field
    private String parseDate(HashMap<String, String> hm, String fstr) {

        // find LAST matching date in string
        int match = -1;
        Matcher pm = DATE_PATTERN.matcher(fstr);
        while (pm.find()) {
            match = pm.start();
//	    System.out.println("MATCH: " + match + ": " + pm.group(0));
        }

        if (match == -1) {
//	    System.out.println("NO MATCH: \"" + fstr + "\"");
            return fstr;
        }

        if (!pm.find(match)) {
//	    System.out.println("MATCH FAILED: \"" + fstr + "\"");
            return fstr;
        }

        StringBuffer date = new StringBuffer();

        String day = pm.group(1);
        if (day == null)
            day = pm.group(5);
        else if (pm.group(5) != null)
            return fstr;	// possible day found in two places

        if (day != null && !day.equals("0")) {
            date.append(day);
            date.append(" ");
        } else
            day = null;

        String mon = pm.group(2);
        if (mon == null)
            mon = pm.group(4);
        else if (pm.group(4) != null)
            return fstr;	// possible month found in two places

        int idx = -1;
        if (mon != null) {
            String lmon = mon.toLowerCase();
            idx = MONS.indexOf(lmon);
            if (idx == -1)  // not legal month, error
                return fstr;
            date.append(mon);
            date.append(" ");
            idx = idx / 4;
            hm.put("month", MONTHS[idx]);

        } else if (day != null) // day found but not month, error
            return fstr;

        String year = pm.group(3);
        date.append(year);

        StringBuffer note = new StringBuffer();
        if (day != null && !day.equals("0")) {
            note.append("Source Date: ");
            note.append(date);
            note.append(".");
            addNote(hm, note.toString());
        }

        // check if journal year matches PY field
        if (hm.get("year") != null) {
            String oyear = hm.get("year");
            if (!year.equals(oyear)) {
                note.setLength(0);
                note.append("Source Year: ");
                note.append(year);
                note.append(".");
                addNote(hm, note.toString());
//		System.out.println(year + " != " + oyear);
            }
        } else
            hm.put("year", year);

        int len = fstr.length();
        StringBuffer newf = new StringBuffer();
        if (pm.start() > 0)
            newf.append(fstr.substring(0, pm.start()));
        if (pm.end() < len)
            newf.append(fstr.substring(pm.end(), len));
        return newf.toString();
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        // CSA records start with "DN: Database Name"
        BufferedReader in =
            new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        while ((str = in.readLine()) != null) {
            if (str.equals("DN: Database Name"))
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
        HashMap<String, String> hm = new HashMap<String, String>();

        BufferedReader in =
            new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        String Type = null;
        String str;
        boolean first = true;
        line = 1;
        str = readLine(in);
        while (true) {
            if (str == null || str.length() == 0) {	// end of record
                if (!hm.isEmpty()) { // have a record
                    if (Type == null) {
                        addNote(hm, "Publication Type: [NOT SPECIFIED]");
                        addNote(hm, "[PERHAPS NOT FULL FORMAT]");
                        Type = "article";
                    }

                    // post-process Journal article
                    if (Type.equals("article") &&
                        hm.get("booktitle") != null) {
                        String booktitle = hm.get("booktitle");
                        hm.remove("booktitle");
                        hm.put("journal", booktitle);
                    }

                    BibtexEntry b =
                        new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID,
                                        Globals.getEntryType(Type));

                    // create one here
                    b.setField(hm);

                    bibitems.add(b);
                }
                hm.clear();	// ready for next record
                first = true;
                if (str == null)
                    break;	// end of file
                str = readLine(in);
                continue;
            }

            int fline = line;	// save this before reading field contents
            Matcher fm = FIELD_PATTERN.matcher(str);
            if (fm.find()) {

                // save the field name (long and short)
                String fabbr = fm.group(1);
                String fname = fm.group(2);

                // read the contents of the field
                sb.setLength(0); // clear the buffer
                while ((str = readLine(in)) != null) {
                    if (! str.startsWith("    ")) // field contents?
                        break;	// nope
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(str.substring(4)); // skip spaces
                }
                String fstr = sb.toString();
                if (fstr == null || fstr.length() == 0) {
                    int line1 = line - 1;
                    throw new IOException("illegal empty field at line " +
                                          line1);
                }
                // replace [Lt] with <
                fm = LT_PATTERN.matcher(fstr);
                if (fm.find())
                    fstr = fm.replaceAll("<");

                // check for start of new record
                if (fabbr.equals("DN") &&
                    fname.equalsIgnoreCase("Database Name")) {
                    if (first == false) {
                        throw new IOException("format error at line " + fline +
                                              ": DN out of order");
                    }
                    first = false;
                } else if (first == true) {
                    throw new IOException("format error at line " + fline +
                                              ": missing DN");
                }

                if (fabbr.equals("PT")) {
                    Type = null;
                    String flow = fstr.toLowerCase();
                    String[] types = flow.split("; ");
                    for (int ii = 0; ii < types.length; ++ii) {
                        if ((types[ii].indexOf("article")>=0) ||
                            (types[ii].indexOf("journal article")>=0)) {
                            Type = "article";
                            break;
                        } else if (types[ii].equals("dissertation")) {
                            Type = "phdthesis";
                            break;
                        } else if (types[ii].equals("conference")) {
                            Type = "inproceedings";
                            break;
                        } else if (types[ii].equals("book monograph") &&
                                   Type == null) {
                            Type = "book";
                            break;
                        } else if (types[ii].equals("report") &&
                                   Type == null) {
                            Type = "techreport";
                            break;
                        }
                    }
                    if (Type == null) {
                        Type = "misc";
                    }

                }

                String ftype = null;
                if (fabbr.equals("AB"))
                    ftype = "abstract";
                else if (fabbr.equals("AF"))
                    ftype = "affiliation";
                else if (fabbr.equals("AU")) {
                    ftype = "author";
                    if (fstr.indexOf(";") >= 0)
                        fstr = fstr.replaceAll("; ", " and ");
                }
                else if (fabbr.equals("CA"))
                    ftype = "organization";
                else if (fabbr.equals("DE"))
                    ftype = "keywords";
                else if (fabbr.equals("DO"))
                    ftype = "doi";
                else if (fabbr.equals("ED"))
                    ftype = "editor";
                else if (fabbr.equals("IB"))
                    ftype = "ISBN";
                else if (fabbr.equals("IS"))
                    ftype = "ISSN";
                else if (fabbr.equals("JN"))
                    ftype = "journal";
                else if (fabbr.equals("LA"))
                    ftype = "language";
                else if (fabbr.equals("PB"))
                    ftype = "publisher";
                else if (fabbr.equals("PY")) {
                    ftype = "year";
                    if (hm.get("year") != null) {
                        String oyear = hm.get("year");
                        if (!fstr.equals(oyear)) {
                            StringBuffer note = new StringBuffer();
                            note.append("Source Year: ");
                            note.append(oyear);
                            note.append(".");
                            addNote(hm, note.toString());
//			    System.out.println(fstr + " != " + oyear);
                        }
                    }
                } else if (fabbr.equals("RL")) {
                    ftype = "url";
                    String[] lines = fstr.split(" ");
                    StringBuffer urls = new StringBuffer();
                    for (int ii = 0; ii < lines.length; ++ii) {
                        if (lines[ii].startsWith("[URL:"))
                            urls.append(lines[ii].substring(5));
                        else if (lines[ii].endsWith("]")) {
                            int len = lines[ii].length();
                            urls.append(lines[ii].substring(0, len - 1));
                            if (ii < lines.length - 1)
                                urls.append("\n");
                        } else
                            urls.append(lines[ii]);
                    }
                    fstr = urls.toString();
                } else if (fabbr.equals("SO")) {
                    ftype = "booktitle";

                    // see if we can extract journal information

                    // compact vol(no):page-page:
                    Matcher pm = VOLNOPP_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("volume", pm.group(1));
                        hm.put("number", pm.group(2));
                        hm.put("pages", pm.group(3));
                        fstr = pm.replaceFirst("");
                    }

                    // pages
                    pm = PAGES_PATTERN.matcher(fstr);
                    StringBuffer pages = new StringBuffer();
                    while (pm.find()) {
                        if (pages.length() > 0)
                            pages.append(",");
                        String pp = pm.group(1);
                        if (pp == null)
                            pp = pm.group(2);
                        if (pp == null)
                            pp = pm.group(3);
                        pages.append(pp);
                        fstr = pm.replaceFirst("");
                        pm = PAGES_PATTERN.matcher(fstr);
                    }
                    if (pages.length() > 0)
                        hm.put("pages", pages.toString());

                    // volume:
                    pm = VOLUME_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("volume", pm.group(1));
                        fstr = pm.replaceFirst("");
                    }

                    // number:
                    pm = NUMBER_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("number", pm.group(1));
                        fstr = pm.replaceFirst("");
                    }

                    // journal date:
                    fstr = parseDate(hm, fstr);

                    // strip trailing whitespace
                    Pattern pp = Pattern.compile(",?\\s*$");
                    pm = pp.matcher(fstr);
                    if (pm.find())
                        fstr = pm.replaceFirst("");

                    if (fstr.equals(""))
                        continue;
//		    System.out.println("SOURCE: \"" + fstr + "\"");
                } else if (fabbr.equals("TI"))
                    ftype = "title";
                else if (fabbr.equals("RE"))
                    continue;	// throw away References

                if (ftype != null) {
                    hm.put(ftype, fstr);
                } else {
                    StringBuffer val = new StringBuffer();
                    val.append(fname);
                    val.append(": ");
                    val.append(fstr);
                    val.append(".");
                    addNote(hm, val.toString());
                }
            } else
                str = readLine(in);
        }

        return bibitems;
    }
}
