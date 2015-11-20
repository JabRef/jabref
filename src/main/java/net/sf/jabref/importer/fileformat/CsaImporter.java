/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fileformat;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibtexEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sf.jabref.bibtex.EntryTypes;

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
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("^([A-Z][A-Z]): ([A-Z].*)$");
    private static final Pattern VOLNOPP_PATTERN =
            Pattern.compile("[;,\\.]\\s+(\\d+[A-Za-z]?)\\((\\d+(?:-\\d+)?)\\)(?:,\\s+|:)(\\d+-\\d+)");
    private static final Pattern PAGES_PATTERN =
            Pattern.compile("[;,\\.]\\s+(?:(\\[?[vn]\\.?p\\.?\\]?)|(?:pp?\\.?\\s+)(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)|(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)(?:\\s+pp?))");
    private static final Pattern VOLUME_PATTERN =
            Pattern.compile("[;,\\.]?\\s+[vV][oO][lL]\\.?\\s+(\\d+[A-Z]?(?:-\\d+[A-Z]?)?)");
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("[;,\\.]\\s+(?:No|no|Part|part|NUMB)\\.?\\s+([A-Z]?\\d+(?:[/-]\\d+)?)");
    private static final Pattern DATE_PATTERN =
            Pattern.compile("[;,\\.]\\s+(?:(\\d+)\\s)?(?:([A-Z][a-z][a-z])[\\.,]*\\s)?\\(?(\\d\\d\\d\\d)\\)?(?:\\s([A-Z][a-z][a-z]))?(?:\\s+(\\d+))?");
    private static final Pattern LT_PATTERN =
            Pattern.compile("\\[Lt\\]");

    // other constants
    private static final String MONS =
            "jan feb mar apr may jun jul aug sep oct nov dec";
    private static final String[] MONTHS =
        {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "CSA";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "csa";
    }

    // read a line
    private String readLine(BufferedReader file) throws IOException {
        String str = file.readLine();
        if (str != null) {
            line++;
        }
        return str;
    }

    // append to the "note" field
    private static void addNote(HashMap<String, String> hm, String note) {

        StringBuilder notebuf = new StringBuilder();
        if (hm.get("note") != null) {
            notebuf.append(hm.get("note"));
            notebuf.append("\n");
        }
        notebuf.append(note);
        hm.put("note", notebuf.toString());
    }

    // parse the date from the Source field
    private static String parseDate(HashMap<String, String> hm, String fstr) {

        // find LAST matching date in string
        int match = -1;
        Matcher pm = CsaImporter.DATE_PATTERN.matcher(fstr);
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

        StringBuilder date = new StringBuilder();

        String day = pm.group(1);
        if (day == null) {
            day = pm.group(5);
        } else if (pm.group(5) != null)
        {
            return fstr; // possible day found in two places
        }

        if ((day != null) && !day.equals("0")) {
            date.append(day);
            date.append(" ");
        } else {
            day = null;
        }

        String mon = pm.group(2);
        if (mon == null) {
            mon = pm.group(4);
        } else if (pm.group(4) != null)
        {
            return fstr; // possible month found in two places
        }

        int idx;
        if (mon != null) {
            String lmon = mon.toLowerCase();
            idx = CsaImporter.MONS.indexOf(lmon);
            if (idx == -1) {
                return fstr;
            }
            date.append(mon);
            date.append(" ");
            idx = idx / 4;
            hm.put("month", CsaImporter.MONTHS[idx]);

        } else if (day != null) {
            return fstr;
        }

        String year = pm.group(3);
        date.append(year);

        StringBuilder note = new StringBuilder();
        if ((day != null) && !day.equals("0")) {
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
        } else {
            hm.put("year", year);
        }

        int len = fstr.length();
        StringBuilder newf = new StringBuilder();
        if (pm.start() > 0) {
            newf.append(fstr.substring(0, pm.start()));
        }
        if (pm.end() < len) {
            newf.append(fstr.substring(pm.end(), len));
        }
        return newf.toString();
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        // CSA records start with "DN: Database Name"
        BufferedReader in =
                new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        while ((str = in.readLine()) != null) {
            if (str.equals("DN: Database Name")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    @Override
    public List<BibtexEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        ArrayList<BibtexEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        HashMap<String, String> hm = new HashMap<>();

        BufferedReader in =
                new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        String Type = null;
        String str;
        boolean first = true;
        line = 1;
        str = readLine(in);
        while (true) {
            if ((str == null) || str.isEmpty()) { // end of record
                if (!hm.isEmpty()) { // have a record
                    if (Type == null) {
                        addNote(hm, "Publication Type: [NOT SPECIFIED]");
                        addNote(hm, "[PERHAPS NOT FULL FORMAT]");
                        Type = "article";
                    }

                    // post-process Journal article
                    if (Type.equals("article") &&
                            (hm.get("booktitle") != null)) {
                        String booktitle = hm.get("booktitle");
                        hm.remove("booktitle");
                        hm.put("journal", booktitle);
                    }

                    BibtexEntry b =
                            new BibtexEntry(DEFAULT_BIBTEXENTRY_ID,
                                    EntryTypes.getBibtexEntryType(Type));

                    // create one here
                    b.setField(hm);

                    bibitems.add(b);
                }
                hm.clear(); // ready for next record
                first = true;
                if (str == null)
                {
                    break; // end of file
                }
                str = readLine(in);
                continue;
            }

            int fline = line; // save this before reading field contents
            Matcher fm = CsaImporter.FIELD_PATTERN.matcher(str);
            if (fm.find()) {

                // save the field name (long and short)
                String fabbr = fm.group(1);
                String fname = fm.group(2);

                // read the contents of the field
                sb.setLength(0); // clear the buffer
                while ((str = readLine(in)) != null) {
                    if (!str.startsWith("    "))
                    {
                        break; // nope
                    }
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(str.substring(4)); // skip spaces
                }
                String fstr = sb.toString();
                if (fstr.isEmpty()) {
                    int line1 = line - 1;
                    throw new IOException("illegal empty field at line " +
                            line1);
                }
                // replace [Lt] with <
                fm = CsaImporter.LT_PATTERN.matcher(fstr);
                if (fm.find()) {
                    fstr = fm.replaceAll("<");
                }

                // check for start of new record
                if (fabbr.equals("DN") &&
                        fname.equalsIgnoreCase("Database Name")) {
                    if (!first) {
                        throw new IOException("format error at line " + fline +
                                ": DN out of order");
                    }
                    first = false;
                } else if (first) {
                    throw new IOException("format error at line " + fline +
                            ": missing DN");
                }

                if (fabbr.equals("PT")) {
                    Type = null;
                    String flow = fstr.toLowerCase();
                    String[] types = flow.split("; ");
                    for (String type : types) {
                        if (type.contains("article") ||
                                type.contains("journal article")) {
                            Type = "article";
                            break;
                        } else if (type.equals("dissertation")) {
                            Type = "phdthesis";
                            break;
                        } else if (type.equals("conference")) {
                            Type = "inproceedings";
                            break;
                        } else if (type.equals("book monograph") &&
                                (Type == null)) {
                            Type = "book";
                            break;
                        } else if (type.equals("report") &&
                                (Type == null)) {
                            Type = "techreport";
                            break;
                        }
                    }
                    if (Type == null) {
                        Type = "misc";
                    }

                }

                String ftype = null;
                if (fabbr.equals("AB")) {
                    ftype = "abstract";
                } else if (fabbr.equals("AF")) {
                    ftype = "affiliation";
                } else if (fabbr.equals("AU")) {
                    ftype = "author";
                    if (fstr.contains(";")) {
                        fstr = fstr.replaceAll("; ", " and ");
                    }
                }
                else if (fabbr.equals("CA")) {
                    ftype = "organization";
                } else if (fabbr.equals("DE")) {
                    ftype = "keywords";
                } else if (fabbr.equals("DO")) {
                    ftype = "doi";
                } else if (fabbr.equals("ED")) {
                    ftype = "editor";
                } else if (fabbr.equals("IB")) {
                    ftype = "ISBN";
                } else if (fabbr.equals("IS")) {
                    ftype = "ISSN";
                } else if (fabbr.equals("JN")) {
                    ftype = "journal";
                } else if (fabbr.equals("LA")) {
                    ftype = "language";
                } else if (fabbr.equals("PB")) {
                    ftype = "publisher";
                } else if (fabbr.equals("PY")) {
                    ftype = "year";
                    if (hm.get("year") != null) {
                        String oyear = hm.get("year");
                        if (!fstr.equals(oyear)) {
                            addNote(hm, "Source Year: " + oyear + ".");
                            //			    System.out.println(fstr + " != " + oyear);
                        }
                    }
                } else if (fabbr.equals("RL")) {
                    ftype = "url";
                    String[] lines = fstr.split(" ");
                    StringBuilder urls = new StringBuilder();
                    for (int ii = 0; ii < lines.length; ++ii) {
                        if (lines[ii].startsWith("[URL:")) {
                            urls.append(lines[ii].substring(5));
                        } else if (lines[ii].endsWith("]")) {
                            int len = lines[ii].length();
                            urls.append(lines[ii].substring(0, len - 1));
                            if (ii < (lines.length - 1)) {
                                urls.append("\n");
                            }
                        } else {
                            urls.append(lines[ii]);
                        }
                    }
                    fstr = urls.toString();
                } else if (fabbr.equals("SO")) {
                    ftype = "booktitle";

                    // see if we can extract journal information

                    // compact vol(no):page-page:
                    Matcher pm = CsaImporter.VOLNOPP_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("volume", pm.group(1));
                        hm.put("number", pm.group(2));
                        hm.put("pages", pm.group(3));
                        fstr = pm.replaceFirst("");
                    }

                    // pages
                    pm = CsaImporter.PAGES_PATTERN.matcher(fstr);
                    StringBuilder pages = new StringBuilder();
                    while (pm.find()) {
                        if (pages.length() > 0) {
                            pages.append(",");
                        }
                        String pp = pm.group(1);
                        if (pp == null) {
                            pp = pm.group(2);
                        }
                        if (pp == null) {
                            pp = pm.group(3);
                        }
                        pages.append(pp);
                        fstr = pm.replaceFirst("");
                        pm = CsaImporter.PAGES_PATTERN.matcher(fstr);
                    }
                    if (pages.length() > 0) {
                        hm.put("pages", pages.toString());
                    }

                    // volume:
                    pm = CsaImporter.VOLUME_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("volume", pm.group(1));
                        fstr = pm.replaceFirst("");
                    }

                    // number:
                    pm = CsaImporter.NUMBER_PATTERN.matcher(fstr);
                    if (pm.find()) {
                        hm.put("number", pm.group(1));
                        fstr = pm.replaceFirst("");
                    }

                    // journal date:
                    fstr = parseDate(hm, fstr);

                    // strip trailing whitespace
                    Pattern pp = Pattern.compile(",?\\s*$");
                    pm = pp.matcher(fstr);
                    if (pm.find()) {
                        fstr = pm.replaceFirst("");
                    }

                    if (fstr.equals(""))
                    {
                        continue;
                        //		    System.out.println("SOURCE: \"" + fstr + "\"");
                    }
                } else if (fabbr.equals("TI")) {
                    ftype = "title";
                } else if (fabbr.equals("RE"))
                {
                    continue; // throw away References
                }

                if (ftype != null) {
                    hm.put(ftype, fstr);
                } else {
                    addNote(hm, fname + ": " + fstr + ".");
                }
            } else {
                str = readLine(in);
            }
        }

        return bibitems;
    }
}
