package net.sf.jabref.imports;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class SixpackImporter extends ImportFormat {

    final String SEPARATOR = new String(new char[] { 0, 48 });

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "Sixpack";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "sixpack";
    }
    
    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
	    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        int i=0;
        while (((str = in.readLine()) != null) && (i < 50)) {

			if (str.indexOf(SEPARATOR) >= 0)
				return true;

            i++;
        }

		return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {


	HashMap<String, String> fI = new HashMap<String, String>();
	fI.put("id", "bibtexkey");
	fI.put("au", "author");
	fI.put("ti", "title");
	fI.put("jo", "journal");
	fI.put("vo", "volume");
	fI.put("nu", "number");
	fI.put("pa", "pages");
	fI.put("mo", "month");
	fI.put("yr", "year");
	fI.put("kw", "keywords");
	fI.put("ab", "abstract");
	fI.put("no", "note");
	fI.put("ed", "editor");
	fI.put("pu", "publisher");
	fI.put("se", "series");
	fI.put("ad", "address");
	fI.put("en", "edition");
	fI.put("ch", "chapter");
	fI.put("hp", "howpublished");
	fI.put("tb", "booktitle");
	fI.put("or", "organization");
	fI.put("sc", "school");
	fI.put("in", "institution");
	fI.put("ty", "type");
	fI.put("url", "url");
	fI.put("cr", "crossref");
    fI.put("fi", "file");

	ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	in.readLine();
    String ln = in.readLine();
    if (ln == null)
        return null;
    String[] fieldDef = ln.split(",");

    String s = null;
	BibtexEntry entry = null;
	lines: while ((s = in.readLine()) != null){
	    try{
		s = s.replaceAll("<par>", ""); // What is <par> ????
		String[] fields = s.split(SEPARATOR);
		// Check type and create entry:
		if (fields.length < 2)
		    continue lines; // Avoid ArrayIndexOutOfBoundsException
		BibtexEntryType typ = BibtexEntryType
		    .getType(fields[1].toLowerCase());
		if (typ == null){
		    String type = "";
		    if (fields[1].equals("Masterthesis")) type = "mastersthesis";
		    if (fields[1].equals("PhD-Thesis")) type = "phdthesis";
		    if (fields[1].equals("miscellaneous")) type = "misc";
		    if (fields[1].equals("Conference")) type = "proceedings";
		    typ = BibtexEntryType.getType(type.toLowerCase());
		}
		entry = new BibtexEntry(Util.createNeutralId(), typ);
		String fld;
		for (int i = 0; i < Math.min(fieldDef.length, fields.length); i++){
		    fld = fI.get(fieldDef[i]);
		    if (fld != null){
			if (fld.equals("author") || fld.equals("editor")) ImportFormatReader.setIfNecessary(entry,
												      fld, fields[i].replaceAll(" and ", ", ").replaceAll(", ",
																			  " and "));
			else if (fld.equals("pages")) ImportFormatReader.setIfNecessary(entry, fld, fields[i]
							       .replaceAll("-", "--"));
            else if (fld.equals("file")) {
                String fieldName = "pdf"; // We set pdf as default.
                if (fields[i].endsWith("ps") || fields[i].endsWith("ps.gz"))
                    fieldName = "ps";
                else if (fields[i].endsWith("html"))
                    fieldName = "url";
                ImportFormatReader.setIfNecessary(entry, fieldName, fields[i]);
            }
			else ImportFormatReader.setIfNecessary(entry, fld, fields[i]);
		    }
		}
		bibitems.add(entry);
	    }catch (NullPointerException ex){
		Globals.logger("Problem parsing Sixpack entry, ignoring entry.");
	    }
	}

	return bibitems;
    }
}


