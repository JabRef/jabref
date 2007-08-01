package net.sf.jabref.imports;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;
import net.sf.jabref.AuthorList;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class JstorImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
    return "JStor (tab delimited)";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "jstor";
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
    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {
    ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
    String s = "";
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    while ((s != null) && !s.startsWith("Item Type"))
        s = in.readLine();

    mainloop: while ((s = in.readLine()) != null){
        if (s.equals("")) continue;
        if (s.startsWith("-----------------------------")) break mainloop;
        String[] fields = s.split("\t");
        BibtexEntry be = new BibtexEntry(Util.createNeutralId());
        try{
        if (fields[0].equals("FLA")) be.setType(BibtexEntryType
                            .getType("article"));
        ImportFormatReader.setIfNecessary(be, "title", fields[2]);
        ImportFormatReader.setIfNecessary(be, "author", AuthorList.fixAuthor_lastNameFirst(fields[4].replaceAll("; ", " and ")));
        ImportFormatReader.setIfNecessary(be, "journal", fields[7]);
        ImportFormatReader.setIfNecessary(be, "volume", fields[9]);
        ImportFormatReader.setIfNecessary(be, "number", fields[10]);
        String[] datefield = fields[12].split(" ");
        ImportFormatReader.setIfNecessary(be, "year", datefield[datefield.length - 1]);
        if (datefield.length > 1) {
            if (datefield[0].endsWith(","))
                datefield[0] = datefield[0].substring(0, datefield[0].length()-1);
            ImportFormatReader.setIfNecessary(be, "month", datefield[0]);
        }
        //for (int i=0; i<fields.length; i++)
        //  Util.pr(i+": "+fields[i]);
        ImportFormatReader.setIfNecessary(be, "pages", fields[13].replaceAll("-", "--"));
        ImportFormatReader.setIfNecessary(be, "url", fields[14]);
        ImportFormatReader.setIfNecessary(be, "issn", fields[15]);
        ImportFormatReader.setIfNecessary(be, "abstract", fields[16]);
        ImportFormatReader.setIfNecessary(be, "keywords", fields[17]);
        ImportFormatReader.setIfNecessary(be, "copyright", fields[21]);
        }catch (ArrayIndexOutOfBoundsException ex){
        }
        bibitems.add(be);
    }

    return bibitems;

    }
}


