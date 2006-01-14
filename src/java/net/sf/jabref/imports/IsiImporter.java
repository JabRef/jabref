package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.AuthorList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Importer for the ISI Web of Science format.
 */
public class IsiImporter extends ImportFormat {
  /**
   * Return the name of this import format.
   */
  public String getFormatName() {
    return "ISI";
  }

  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#getCLIId()
   */
  public String getCLIId() {
    return "isi";
  }
    
  /**
   * Check whether the source is in the correct format for this importer.
   */
  public boolean isRecognizedFormat(InputStream stream)
    throws IOException {
    // Our strategy is to look for the "PY <year>" line.
    BufferedReader in =
      new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    Pattern pat1 = Pattern.compile("PY \\d{4}");

    //was PY \\\\d{4}? before
    String str;

    while ((str = in.readLine()) != null) {

      // The following line gives false positives for RIS files, so it should
      // not be uncommented. The hypen is a characteristic of the RIS format.
      //str = str.replace(" - ", "");


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

    BufferedReader in =
      new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

    //Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB
    // |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
    String str;

    while ((str = in.readLine()) != null) {
      if (str.length() < 3)
        continue;

      // begining of a new item
      if (str.substring(0, 3).equals("PT "))
          sb.append("::").append(str);
      else {
        String beg = str.substring(0, 3).trim();

        // I could have used the fieldPattern regular expression instead
        // however this seems to be
        // quick and dirty and it works!
        if (beg.length() == 2) {
          sb.append(" ## "); // mark the begining of each field
          sb.append(str);
        } else {
          sb.append("EOLEOL"); // mark the end of each line
          sb.append(str.substring(2, str.length())); //remove the initial " "
        }
      }
    }

    String[] entries = sb.toString().split("::");

    // skip the first entry as it is either empty or has document header
    HashMap hm = new HashMap();

    for (int i = 0; i < entries.length; i++) {
      String[] fields = entries[i].split(" ## ");

      if (fields.length == 0)
        fields = entries[i].split("\n");

      String Type = "";
      String PT = "";
      String pages = "";
      hm.clear();

      for (int j = 0; j < fields.length; j++) {
        //empty field don't do anything
        if (fields[j].length() <= 2)
          continue;

        // this is Java 1.5.0 code: 
        // fields[j] = fields[j].replace(" - ", "");
        // TODO: switch to 1.5.0 some day; until then, use 1.4.2 code:
        fields[j] = fields[j].replaceAll(" - ", "");

        String beg = fields[j].substring(0, 2);
        String value = fields[j].substring(2);
        value = value.trim();

        if (beg.equals("PT")) {
          PT = value.replaceAll("Journal", "article").replaceAll("J", "article");
          Type = "article"; //make all of them PT?
        } else if (beg.equals("TY")) {
          if ("CONF".equals(value))
            Type = "inproceedings";
        } else if (beg.equals("JO"))
          hm.put("booktitle", value);
        else if (beg.equals("AU")) {
	    String author = isiAuthorConvert(
            AuthorList.fixAuthor_lastNameFirst(value.replaceAll("EOLEOL", " and ")));

          // if there is already someone there then append with "and"
          if (hm.get("author") != null)
            author = (String) hm.get("author") + " and " + author;

          hm.put("author", author);
        } else if (beg.equals("TI"))
          hm.put("title", value.replaceAll("EOLEOL", " "));
        else if (beg.equals("SO"))
          hm.put("journal", value.replaceAll("EOLEOL", " "));
        else if (beg.equals("ID"))
          hm.put("keywords", value.replaceAll("EOLEOL", " "));
        else if (beg.equals("AB"))
          hm.put("abstract", value.replaceAll("EOLEOL", " "));
        else if (beg.equals("BP") || beg.equals("BR") || beg.equals("SP"))
          pages = value;
        else if (beg.equals("EP")) {
          int detpos = value.indexOf(' ');

          // tweak for IEEE Explore
          if (detpos != -1)
            value = value.substring(0, detpos);

          pages = pages + "--" + value;
        } else if (beg.equals("AR"))
          pages = value;
        else if (beg.equals("IS"))
          hm.put("number", value);
        else if (beg.equals("PY"))
          hm.put("year", value);
        else if (beg.equals("VL"))
          hm.put("volume", value);
        else if (beg.equals("PD")) {
            String[] parts = value.split(" ");
            for (int ii=0; ii<parts.length; ii++) {
                if (Globals.MONTH_STRINGS.containsKey(parts[ii].toLowerCase())) {
                    hm.put("month", "#"+parts[ii].toLowerCase()+"#");
                }
            }
        }
        else if (beg.equals("DT")) {
          Type = value;
	  if (Type.equals("Review")) {
	      Type = "article";
	      // set "Review" in Note/Comment?
	  }
          else if (!Type.equals("Article") && !PT.equals("Journal"))
            Type = "misc";
          else
            Type = "article";
        } //ignore
        else if (beg.equals("CR"))
          hm.put("CitedReferences", value.replaceAll("EOLEOL", " ; ").trim());
      }

      if (!"".equals(pages))
        hm.put("pages", pages);

      BibtexEntry b =
        new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals.getEntryType(Type)); // id assumes an existing database so don't

      // create one here
      b.setField(hm);

      bibitems.add(b);
    }

    return bibitems;
  }

    private String isiAuthorConvert(String authors) {
	String[] author = authors.split(" and ");
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<author.length; i++) {
	    int pos = author[i].indexOf(", ");
	    if (pos > 0) {
		sb.append(author[i].substring(0, pos));
		sb.append(", ");

		for (int j=pos+2; j<author[i].length(); j++) {
		    sb.append(author[i].charAt(j));
		    sb.append(".");
		    if (j<author[i].length()-1)
			sb.append(" ");
		}
	    } else
		sb.append(author[i]);
	    if (i<author.length-1)
		sb.append(" and ");
	}
	return sb.toString();
    }

}
