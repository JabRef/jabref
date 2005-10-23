package net.sf.jabref.imports;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class OvidImporter implements ImportFormat {

    public static Pattern ovid_src_pat = Pattern
    .compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+)\\(([\\w\\-]+)\\):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");

    public static Pattern ovid_src_pat_no_issue = Pattern
    .compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");

    //   public static Pattern ovid_pat_inspec= Pattern.compile("Source ([
    // \\w&\\-]+)");


    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
    return "Ovid";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream in) throws IOException {
    return true;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List importEntries(InputStream stream) throws IOException {
    ArrayList bibitems = new ArrayList();
    StringBuffer sb = new StringBuffer();
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    String line;
    while ((line = in.readLine()) != null){
        if (line.length() > 0 && line.charAt(0) != ' '){
        sb.append("__NEWFIELD__");
        }
        sb.append(line);
        sb.append('\n');
    }

    String items[] = sb.toString().split("<[0-9]+>");

    for (int i = 1; i < items.length; i++){
        HashMap h = new HashMap();
        String[] fields = items[i].split("__NEWFIELD__");
        for (int j = 0; j < fields.length; j++){
            int linebreak = fields[j].indexOf('\n');
            String fieldName = fields[j].substring(0, linebreak).trim();
            String content = fields[j].substring(linebreak).trim();
            //fields[j] = fields[j].trim();
            if (fieldName.indexOf("Author") == 0
                && fieldName.indexOf("Author Keywords") == -1
                && fieldName.indexOf("Author e-mail") == -1){
                String author;
                String[] names;
                if (content.indexOf(";") > 0){ //LN FN; [LN FN;]*
                    names = content.replaceAll("[^\\.A-Za-z,;\\- ]", "").split(";");
                }else{// LN FN. [LN FN.]*
                    //author = content.replaceAll("\\.", " and").replaceAll(" and$", "");
                    names = content.split("  ");
                }

                StringBuffer buf = new StringBuffer();
                for (int ii=0; ii<names.length; ii++) {
                    names[ii] = names[ii].trim();
                    int space = names[ii].indexOf(' ');
                    if (space >= 0) {
                        buf.append(names[ii].substring(0, space));
                        buf.append(',');
                        buf.append(names[ii].substring(space));
                    } else {
                        buf.append(names[ii]);
                    }
                    if (ii < names.length-1)
                        buf.append(" and ");
                }
                h.put("author", buf.toString());

                //    author = content.replaceAll("  ", " and ").replaceAll(" and $", "");


            //h.put("author", ImportFormatReader.fixAuthor_lastNameFirst(author));

        }else if (fieldName.indexOf("Title") == 0) h.put("title",
                       content.replaceAll("\\[.+\\]", ""));
        else if (fieldName.indexOf("Source") == 0){
            //System.out.println(fields[j]);
            String s = content;
            Matcher matcher = ovid_src_pat.matcher(s);
            boolean matchfound = matcher.find();
            if (matchfound){
            h.put("journal", matcher.group(1));
            h.put("volume", matcher.group(2));
            h.put("issue", matcher.group(3));
            h.put("pages", matcher.group(4));
            h.put("year", matcher.group(5));
            }else{// may be missing the issue
            matcher = ovid_src_pat_no_issue.matcher(s);
            matchfound = matcher.find();
            if (matchfound){
                h.put("journal", matcher.group(1));
                h.put("volume", matcher.group(2));
                h.put("pages", matcher.group(3));
                h.put("year", matcher.group(4));
            }
            }

        }else
            if (fieldName.equals("Abstract")) {
                System.out.println("'"+content+"'");
                h.put("abstract", content);
            }
        }
        BibtexEntry b = new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals
                        .getEntryType("article")); // id assumes an existing database so
        // don't create one here
        b.setField(h);

        bibitems.add(b);

    }

    return bibitems;
    }
}


