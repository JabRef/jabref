/*
Copyright (C) 2003  Morten O. Alver and Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref.imports;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;
import org.xml.sax.*; // for medline
import org.xml.sax.helpers.*;  //for medline
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import net.sf.jabref.*;

/*
  // int jabrefframe
BibtexDatabase database=new BibtexDatabase();
String filename=Globals.getNewFile();
ArrayList bibitems=readISI(filename);
// is there a getFileName();
Iterator it = bibitems.iterator();
while(it.hasNext()){
BibtexEntry entry = (BibtexEntry)it.next();
entry.setId(Util.createId(entry.getType(), database);
try {
database.insertEntry(entry);
} catch (KeyCollisionException ex) {

}
}
*/
public class ImportFormatReader
{

    /**
     * Describe <code>fixAuthor</code> method here.
     *
     * @param in a <code>String</code> value
     * @return a <code>String</code> value
     // input format string: LN FN [and LN, FN]*
     // output format string: FN LN [and FN LN]*
     */
    public static String fixAuthor_nocomma(String in){

	return fixAuthor(in);
	/*
      // Check if we have cached this particular name string before:
      Object old = Globals.nameCache.get(in);
      if (old != null)
        return (String)old;

      StringBuffer sb=new StringBuffer();
      String[] authors = in.split(" and ");
      for(int i=0; i<authors.length; i++){
        //System.out.println(authors[i]);
        authors[i]=authors[i].trim();
        String[] t = authors[i].split(" ");
	if (t.length > 1) {
	    sb.append(t[t.length-1].trim());
	    for (int cnt=0; cnt<=t.length-2; cnt++)
		sb.append(" " + t[cnt].trim());
	} else
	    sb.append(t[0].trim());
	if(i==authors.length-1)
	    sb.append(".");
	else
	    sb.append(" and ");
	
      }

      String fixed = sb.toString();

      // Add the fixed name string to the cache.
      Globals.nameCache.put(in, fixed);

      return fixed;*/
    }

    //========================================================
    // rearranges the author names
    // input format string: LN, FN [and LN, FN]*
    // output format string: FN LN [, FN LN]+ [and FN LN]
    //========================================================    
    public static String fixAuthor_commas(String in) {
        return(fixAuthor(in, false));        
    }

    //========================================================
    // rearranges the author names
    // input format string: LN, FN [and LN, FN]*
    
    // output format string: FN LN [and FN LN]*
    //========================================================

    public static String fixAuthor(String in) {
        return(fixAuthor(in, true));
    }
    
    public static String fixAuthor(String in, boolean includeAnds){

      // Check if we have cached this particular name string before:
      if (includeAnds) {
          Object old = Globals.nameCache.get(in);
          if (old != null)
              return (String)old;
      } else {
          Object old = Globals.nameCache_commas.get(in);
          if (old != null)
              return (String)old;          
      }

	//Util.pr("firstnamefirst");
	StringBuffer sb=new StringBuffer();
	//System.out.println("FIX AUTHOR: in= " + in);

	String[] authors = in.split(" and ");
	for(int i=0; i<authors.length; i++){
	    authors[i]=authors[i].trim();
	    String[] t = authors[i].split(",");
	    if(t.length < 2)
	        // there is no comma, assume we have FN LN order
	     sb.append(authors[i].trim());
	    else
	        sb.append( t[1].trim() + " " + t[0].trim());
	    if (includeAnds) {
	        if(i != authors.length-1 ) // put back the " and "
	            sb.append(" and ");
	    } else {
	        if (i == authors.length - 2)
	            sb.append(" and ");
	        else if (i != (authors.length - 1))
	            sb.append(", ");
	    }
	}

        String fixed = sb.toString();

        // Add the fixed name string to the cache.
        Globals.nameCache.put(in, fixed);

	return fixed;
    }

    //========================================================
    // rearranges the author names
    // input format string: LN, FN [and LN, FN]*
    // output format string: LN, FN [and LN, FN]*
    //========================================================
    public static String fixAuthor_lastnameFirst(String in){

      // Check if we have cached this particular name string before:
      Object old = Globals.nameCache_lastFirst.get(in);
      if (old != null)
        return (String)old;

      //Util.pr("lastnamefirst: in");
      StringBuffer sb=new StringBuffer();

      String[] authors = in.split(" and ");
      for(int i=0; i<authors.length; i++){
	  authors[i]=authors[i].trim();
	  int comma = authors[i].indexOf(',');
	  test:if (comma >= 0) {
	      // There is a comma, so we assume it's ok.
	      sb.append(authors[i]);
        }
        else {
          // The name is without a comma, so it must be rearranged.
          int pos = authors[i].lastIndexOf(' ');
          if (pos == -1) {
            // No spaces. Give up and just add the name.
            sb.append(authors[i]);
            break test;
          }
          String surname = authors[i].substring(pos+1);
          if (surname.equalsIgnoreCase("jr.")) {
            pos = authors[i].lastIndexOf(' ', pos - 1);
            if (pos == -1) {
              // Only last name and jr?
              sb.append(authors[i]);
              break test;
            }
            else
              surname = authors[i].substring(pos+1);
          }
          // Ok, we've isolated the last name. Put together the rearranged name:
          sb.append(surname + ", ");
          sb.append(authors[i].substring(0, pos));

        }
        if (i != authors.length - 1)
          sb.append(" and ");
      }
      /*String[] t = authors[i].split(",");
            if(t.length < 2) {
         // The name is without a comma, so it must be rearranged.
         t = authors[i].split(" ");
         if (t.length > 1) {
             sb.append(t[t.length - 1]+ ","); // Last name
             for (int j=0; j<t.length-1; j++)
          sb.append(" "+t[j]);
         } else if (t.length > 0)
                  sb.append(t[0]);
            }
            else {
         // The name is written with last name first, so it's ok.
         sb.append(authors[i]);
            }

            if(i !=authors.length-1)
         sb.append(" and ");

        }*/
      //Util.pr(in+" -> "+sb.toString());
      String fixed = sb.toString();

      // Add the fixed name string to the cache.
      Globals.nameCache_lastFirst.put(in, fixed);

      return fixed;
    }


    public static ArrayList readSixpack(String filename) {
      final String SEPARATOR = new String(new char[] {0, 48});
      HashMap fI = new HashMap();
      fI.put("id" , "bibtexkey");
      fI.put("au" , "author");
      fI.put("ti" , "title");
      fI.put("jo" , "journal");
      fI.put("vo" , "volume");
      fI.put("nu" , "number");
      fI.put("pa" , "pages");
      fI.put("mo" , "month");
      fI.put("yr" , "year");
      fI.put("kw" , "keywords");
      fI.put("ab" , "abstract");
      fI.put("no" , "note");
      fI.put("ed" , "editor");
      fI.put("pu" , "publisher");
      fI.put("se" , "series");
      fI.put("ad" , "address");
      fI.put("en" , "edition");
      fI.put("ch" , "chapter");
      fI.put("hp" , "howpublished");
      fI.put("tb" , "booktitle");
      fI.put("or" , "organization");
      fI.put("sc" , "school");
      fI.put("in" , "institution");
      fI.put("ty" , "type");
      fI.put("url" , "url");
      fI.put("cr" , "crossref");

      ArrayList bibitems=new ArrayList();
      File f = new File(filename);

      if(!f.exists() && !f.canRead() && !f.isFile()){
        System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
        return null;
      }

      try{
        BufferedReader in = new BufferedReader(new FileReader( filename));
        in.readLine();
        String[] fieldDef = in.readLine().split(",");
        String s = null;
        BibtexEntry entry = null;
        while ((s = in.readLine()) != null) {
          try {
            s = s.replaceAll("<par>", ""); // What is <par> ????
            String[] fields = s.split(SEPARATOR);
            // Check type and create entry:
            BibtexEntryType typ = BibtexEntryType.getType(fields[1].toLowerCase());
            if (typ == null) {
              String type = "";
              if (fields[1].equals("Masterthesis")) type = "mastersthesis";
              if (fields[1].equals("PhD-Thesis")) type = "phdthesis";
              if (fields[1].equals("miscellaneous")) type = "misc";
              if (fields[1].equals("Conference")) type = "proceedings";
              typ = BibtexEntryType.getType(type.toLowerCase());
            }
            entry = new BibtexEntry(Util.createNeutralId(), typ);
            String fld;
            for (int i=0; i<Math.min(fieldDef.length, fields.length); i++) {
              fld = (String)fI.get(fieldDef[i]);
              if (fld != null) {
                if (fld.equals("author") || fld.equals("editor"))
                  setField(entry, fld, fields[i].replaceAll(" and ", ", ").replaceAll(", ", " and "));
                else if (fld.equals("pages"))
                  setField(entry, fld, fields[i].replaceAll("-", "--"));
                else
                  setField(entry, fld, fields[i]);
              }
            }
            bibitems.add(entry);
          } catch (NullPointerException ex) {
            Globals.logger("Problem parsing Sixpack entry, ignoring entry.");
          }
        }
        in.close();
      }
      catch(IOException e){return null;}

      return bibitems;
    }

    /**
     * Just a little wrapper for BibtexEntry's setField, to prevent the field
     * from getting set when the content is an empty string.
     */
    private static void setField(BibtexEntry be, String field, String content) {
      if (!content.equals(""))
        be.setField(field, content);
    }

    //============================================================
    // given a filename, parses the file (assuming scifinder)
    // returns null if unable to find any entries or if the
    // file is not in scifinder format
    //============================================================
    public static ArrayList readScifinder( String filename)
    {
		ArrayList bibitems=new ArrayList();
		File f = new File(filename);

		if(!f.exists() && !f.canRead() && !f.isFile()){
			System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
			return null;
		}
		StringBuffer sb=new StringBuffer();
		try{
			BufferedReader in = new BufferedReader(new FileReader( filename));

			String str;
			while ((str = in.readLine()) != null) {
				sb.append(str);
			}
			in.close();

		}
		catch(IOException e){return null;}
		String [] entries=sb.toString().split("START_RECORD");
		HashMap hm=new HashMap();
		for(int i=1; i<entries.length; i++){
			String[] fields = entries[i].split("FIELD ");
			String Type="";
			hm.clear(); // reset
			for(int j=0; j<fields.length; j++) if (fields[j].indexOf(":") >= 0) {
				String tmp[]= new String[2];
                          tmp[0] = fields[j].substring(0, fields[j].indexOf(":"));
                          tmp[1] = fields[j].substring(fields[j].indexOf(":")+1);
				if(tmp.length > 1){//==2
					if(tmp[0].equals("Author"))
						hm.put( "author", tmp[1].replaceAll(";"," and ") );
					else if(tmp[0].equals("Title"))
						hm.put("title",tmp[1]);

					else if(tmp[0].equals("Journal Title"))
						hm.put("journal",tmp[1]);

					else if(tmp[0].equals("Volume"))
					hm.put("volume",tmp[1]);
				else if(tmp[0].equals("Page"))
					hm.put("pages",tmp[1]);
				else if(tmp[0].equals("Publication Year"))
					hm.put("year",tmp[1]);
				else if(tmp[0].equals("Abstract"))
					hm.put("abstract",tmp[1]);
				else if(tmp[0].equals("Supplementary Terms"))
					hm.put("keywords",tmp[1]);
				else if(tmp[0].equals("Document Type"))
					Type=tmp[1].replaceAll("Journal","article");
			}
	    }

	    BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
									  Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
	    b.setField( hm);
	    bibitems.add( b  );

	}
	return bibitems;
     }
    //==================================================
    //
    //==================================================

    public static ArrayList readISI( String filename) //jbm for new Bibitem
    {
	ArrayList bibitems=new ArrayList();
	File f = new File(filename);

	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}
	StringBuffer sb=new StringBuffer();
	try {
	    BufferedReader in = new BufferedReader(new FileReader( filename));
	    //Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
	    String str;
	    while ((str = in.readLine()) != null) {
		if(str.length() <3)
		    continue;
		// begining of a new item
		if( str.substring(0,3).equals("PT ")){
		    sb.append("::"+str);
		}
		else{
		    String beg = str.substring(0,3).trim();
		    // I could have used the fieldPattern regular expression instead however this seems to be
		    // quick and dirty and it works!
		    if(beg.length()==2) {
			sb.append(" ## ");// mark the begining of each field
			sb.append(str);

		    }else{
			sb.append("EOLEOL");// mark the end of each line
			sb.append(str.substring(2,str.length()));//remove the initial " "

		    }

		}
	    }
	    in.close();
	} catch (IOException e) {
	    //JOptionPane.showMessageDialog(null, "Error: reading " + filename );
	    System.err.println("Error reading file: " + filename);
	    return null;
	}

	String[] entries = sb.toString().split("::");
	// skip the first entry as it is either empty or has document header

	HashMap hm=new HashMap();
	for(int i=1; i<entries.length; i++){
	    String[] fields = entries[i].split(" ## ");
	    String Type="",PT="",pages="";
	    hm.clear();

	    for(int j=0; j<fields.length; j++){
		String beg=fields[j].substring(0,2);
		if(beg.equals("PT")){
		    PT = fields[j].substring(2,fields[j].length()).trim().replaceAll("Journal","article");
		    Type = "article"; //make all of them PT?
 		}
		else if(beg.equals("AU"))
		    hm.put( "author", fixAuthor_lastnameFirst( fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," and ") ));
		else if(beg.equals("TI"))
		    hm.put("title", fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("SO")){ // journal name
		    hm.put("journal",fields[j].substring(2,fields[j].length()).trim());
		}
		else if(beg.equals("ID"))
		    hm.put( "keywords",fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("AB"))
		    hm.put("abstract", fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("BP") || beg.equals("BR"))
		    //hm.put("pages", fields[j].substring(2,fields[j].length()).trim());
		    pages=fields[j].substring(2,fields[j].length()).trim();
		else if(beg.equals("EP")){
		    pages=pages + "--" + fields[j].substring(2,fields[j].length()).trim();
		}
		else if(beg.equals("AR"))
		    pages = fields[j].substring(2,fields[j].length()).trim();
		else if(beg.equals("IS"))
			hm.put( "number",fields[j].substring(2,fields[j].length()).trim());
		else if(beg.equals("PY"))
		    hm.put("year", fields[j].substring(2,fields[j].length()).trim());
		else if(beg.equals("VL"))
		    hm.put( "volume",fields[j].substring(2,fields[j].length()).trim());
		else if(beg.equals("DT")){
 		    Type = fields[j].substring(2,fields[j].length()).trim();
 		    if(!Type.equals("Article") && !PT.equals("Journal"))//Article"))
 			Type="misc";
 		    else
 			Type="article";
		}//ignore
		else if(beg.equals("CR")) //cited references
		    hm.put("CitedReferences",fields[j].substring(2,fields[j].length()).replaceAll("EOLEOL"," ; ").trim());
	    }

	    hm.put("pages",pages);
	    BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
									  Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
	    b.setField( hm);

	    bibitems.add( b  );
	}

	return bibitems;
    }


    //==================================================
    //
    //==================================================
    public static Pattern ovid_src_pat= Pattern.compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+)\\(([\\w\\-]+)\\):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");
    public static Pattern ovid_src_pat_no_issue= Pattern.compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");
 //   public static Pattern ovid_pat_inspec= Pattern.compile("Source ([ \\w&\\-]+)");

    public static ArrayList readOvid( String filename){
	ArrayList bibitems = new ArrayList();
	File f=new File(filename);
	int rowNum=0;
	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error: " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}
	try{
	    BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line;
	    StringBuffer sb=new StringBuffer();
	    while((line=in.readLine()) != null){
		if(line.length()>0 && line.charAt(0) != ' ') {
                  sb.append("__NEWFIELD__");
                }
		sb.append(line);
	    }
	    in.close();
	    String items[]=sb.toString().split("<[0-9]+>");

	    for(int i =1; i<items.length; i++){
		HashMap h=new HashMap();
		String[] fields=items[i].split("__NEWFIELD__");
		for(int j=0; j<fields.length; j++){
		    fields[j]=fields[j].trim();
		    if(fields[j].indexOf("Author") == 0 && fields[j].indexOf("Author Keywords") ==-1
		       && fields[j].indexOf("Author e-mail") ==-1){
			String author;
			boolean isComma=false;
			if( fields[j].indexOf(";") > 0){ //LN FN; [LN FN;]*
			    author  = fields[j].substring(7,fields[j].length()).replaceAll("[^\\.A-Za-z,;\\- ]","").replaceAll(";"," and ");
			}
			else{// LN FN. [LN FN.]*
			    isComma=true;
			    author  = fields[j].substring(7,fields[j].length()).replaceAll("\\."," and").replaceAll(" and$","");

			}
			if(author.split(" and ").length > 1){ // single author or no ";"
                          h.put("author",  fixAuthor_lastnameFirst( author) );
			   /* if(isComma==false)

			    else
				h.put("author",  fixAuthor_nocomma( author) );*/
			}
			else
			    h.put("author",author);
		    }
		    else if(fields[j].indexOf("Title") == 0)
			h.put("title", fields[j].substring(6,fields[j].length()).replaceAll("\\[.+\\]","") );
		    else if(fields[j].indexOf("Source") == 0){
                      System.out.println(fields[j]);
			String s=fields[j];
			Matcher matcher = ovid_src_pat.matcher(s);
			boolean matchfound = matcher.find();
			if(matchfound){
			    h.put("journal", matcher.group(1));
			    h.put("volume", matcher.group(2));
			    h.put("issue", matcher.group(3));
			    h.put("pages", matcher.group(4));
			    h.put("year", matcher.group(5));
			}else{// may be missing the issue
			    matcher = ovid_src_pat_no_issue.matcher(s);
			    matchfound = matcher.find();
			    if(matchfound){
				h.put("journal", matcher.group(1));
				h.put("volume", matcher.group(2));
				h.put("pages", matcher.group(3));
				h.put("year", matcher.group(4));
			    }
			}

		    }
		    else if(fields[j].indexOf("Abstract")==0)
			h.put("abstract",fields[j].substring(9,fields[j].length()));
		    //else if(fields[j].indexOf("References")==0)
		    //	h.put("references", fields[j].substring( 11,fields[j].length()));
		}
		BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
									  Globals.getEntryType("article")); // id assumes an existing database so don't create one here
		b.setField( h);

		bibitems.add( b  );

	    }

	}
	catch(IOException ex){
	    return null;

	}
	return bibitems;
    }

    static File checkAndCreateFile(String filename){
	File f = new File(filename);
	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    Globals.logger( "Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}else
	    return f;

    }
    // check here for details on the format
    // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
    public static ArrayList readEndnote(String filename){
	String ENDOFRECORD="__EOREOR__";
	ArrayList bibitems = new ArrayList();
	File f = checkAndCreateFile( filename );// will return null if file is not readable
	if(f==null)  return null;
	StringBuffer sb = new StringBuffer();
	try{
	    BufferedReader in = new BufferedReader(new FileReader( filename));

	    String str;
	    boolean first = true;
	    while ((str = in.readLine()) != null) {
		str = str.trim();
		// if(str.equals("")) continue;
		if(str.indexOf("%0")==0){
		    if (first) {
			first = false;
		    }
		    else {
			sb.append(ENDOFRECORD);
		    }
		    sb.append(str);
		}else
		    sb.append(str);
		sb.append("\n");
	    }
	    in.close();
	}

	catch(IOException e){return null;}

	String [] entries=sb.toString().split(ENDOFRECORD);
	HashMap hm=new HashMap();
	String Author="",Type="",Editor="";
	for(int i=0; i<entries.length; i++){
	    hm.clear();
	    Author=""; Type="";Editor="";
	    String[] fields = entries[i].substring(1).split("\n%");
            //String lastPrefix = "";
	    for(int j=0; j <fields.length; j++){
		if(fields[j].length() < 3)
		    continue;

		/* Details of Refer format for Journal Article and Book:

		  Generic            Ref     Journal Article   Book
		  Code
		  Author             %A      Author            Author
		  Year               %D      Year              Year
		  Title              %T      Title             Title
		  Secondary Author   %E                        Series Editor
		  Secondary Title    %B      Journal           Series Title
		  Place Published    %C                        City
		  Publisher          %I                        Publisher
		  Volume             %V      Volume            Volume
		  Number of Volumes  %6                        Number of Volumes
		  Number             %N      Issue
		  Pages              %P      Pages             Number of Pages
		  Edition            %7                        Edition
		  Subsidiary Author  %?                        Translator
		  Alternate Title    %J      Alternate Journal
		  Label              %F      Label             Label
		  Keywords           %K      Keywords          Keywords
		  Abstract           %X      Abstract          Abstract
		  Notes              %O      Notes             Notes
		*/

		String prefix=fields[j].substring(0,1);
		String val = fields[j].substring(2);
		if( prefix.equals("A")){
		    if( Author.equals(""))
			Author=val;
		    else
			Author += " and " + val;
		}
		else if(prefix.equals("Y")){
		    if( Editor.equals("")) Editor=val;
		    else Editor += " and " + val;
		}
		else if(prefix.equals("T")) hm.put("title", Globals.putBracesAroundCapitals(val));
		else if(prefix.equals("0")){
		    if(val.indexOf("Journal")==0)
			Type="article";
		    else if((val.indexOf("Book")==0)
			    || (val.indexOf("Edited Book")==0))
			Type="book";
		    else if( val.indexOf("Conference")==0)// Proceedings
			Type="inproceedings";
		    else if( val.indexOf("Report")==0) // Techreport
			Type="techreport";
		    else
			Type = "misc"; //
		}
		else if(prefix.equals("7")) hm.put("edition",val);
		else if(prefix.equals("C")) hm.put("address",val);
		else if(prefix.equals("D")) hm.put("year",val);
                else if(prefix.equals("8")) hm.put("date",val);
		else if(prefix.equals("J")) {
		    // "Alternate journal. Let's set it only if no journal
		    // has been set with %B.
		    if (hm.get("journal") == null)
			hm.put("journal", val);
		}
                else if (prefix.equals("B")) {
		    // This prefix stands for "journal" in a journal entry, and
		    // "series" in a book entry.
		    if (Type.equals("article")) hm.put("journal", val);
		    else if (Type.equals("book") || Type.equals("inbook"))
			hm.put("series", val);
		    else /* if (Type.equals("inproceedings"))*/
			hm.put("booktitle", val);
                }
		else if(prefix.equals("I")) hm.put("publisher",val);
		else if(prefix.equals("P")) hm.put("pages",val);
		else if(prefix.equals("V")) hm.put("volume",val);
		else if(prefix.equals("N")) hm.put("number",val);
		else if(prefix.equals("U")) hm.put("url",val);
                else if(prefix.equals("O")) hm.put("note",val);
                else if(prefix.equals("K")) hm.put("keywords", val);
		else if(prefix.equals("X")) hm.put("abstract",val);
		else if(prefix.equals("9")) {
		    //Util.pr(val);
		    if (val.indexOf("Ph.D.")==0)
			Type = "phdthesis";
		}
		else if(prefix.equals("F")) hm.put
		    (Globals.KEY_FIELD,Util.checkLegalKey(val));
	    }
	    //fixauthorscomma
            if (!Author.equals(""))
              hm.put("author",fixAuthor_lastnameFirst(Author));
	    if( !Editor.equals(""))
		hm.put("editor",fixAuthor_lastnameFirst(Editor));
	    BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
					  Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
	    b.setField( hm);
            //if (hm.isEmpty())
            if (b.getAllFields().length > 0)
              bibitems.add(b);

	}
	return bibitems;
    }
    //========================================================
    //
    //========================================================
    public static ArrayList readReferenceManager10(String filename)
    {
	ArrayList bibitems=new ArrayList();
	File f = new File(filename);

	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}
	StringBuffer sb=new StringBuffer();
	try{
	    BufferedReader in = new BufferedReader(new FileReader( filename));

	    String str;
	    while ((str = in.readLine()) != null) {
		sb.append(str);
		sb.append("\n");
	    }
	    in.close();

	}

	catch(IOException e){return null;}

	String [] entries=sb.toString().split("ER  -");

	HashMap hm=new HashMap();
	for(int i=0; i<entries.length-1; i++){
	    String Type="",Author="",StartPage="",EndPage="";
	    hm.clear();

	    String[] fields = entries[i].split("\n");
	    for(int j=0; j <fields.length; j++){
		if(fields[j].length() < 6)
		    continue;
		else{
		    String lab = fields[j].substring(0,2);
		    String val = fields[j].substring(6).trim();
		    if(lab.equals("TY")){
			if(val.equals("BOOK")) Type = "book";
			else if (val.equals("JOUR")) Type = "article";
			else Type = "other";
		    }else if(lab.equals("T1") || lab.equals("TI"))
			hm.put("title",val);//Title = val;

		    else if(lab.equals("A1") ||lab.equals("AU")){

			if( Author.equals("")) // don't add " and " for the first author
			    Author=val;
			else
			    Author += " and " + val;
		    }else if( lab.equals("JA") || lab.equals("JF") || lab.equals("JO"))
			hm.put("journal",val);
		    else if(lab.equals("SP"))
			StartPage=val;

		    else if(lab.equals("EP"))
			EndPage=val;

		    else if(lab.equals("VL")) hm.put("volume",val);
		    else if(lab.equals("IS")) hm.put("number",val);
		    else if(lab.equals("N2") || lab.equals("AB")) hm.put("abstract",val);
		    else if(lab.equals("UR")) hm.put("url",val);
                    else if((lab.equals("Y1")||lab.equals("PY"))&& val.length()>=4)
                      hm.put("year",val.substring(0,4));
                    else if(lab.equals("KW")) {
                      if (!hm.containsKey("keywords"))
                        hm.put("keywords", val);
                      else {
                        String kw = (String)hm.get("keywords");
                        hm.put("keywords", kw+", "+val);
                      }
                    }
		}
	    }
	    // fix authors
	    Author = fixAuthor_lastnameFirst(Author);
	    if(Author.endsWith("."))
		hm.put("author", Author.substring(0,Author.length()-1));
	    else
		hm.put("author", Author);

	    hm.put("pages",StartPage+"--"+EndPage);
	    BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
									  Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
	    b.setField( hm);

	    bibitems.add( b  );

	}

	return bibitems;
    }

    //==================================================
    //
    //==================================================
    public static ArrayList readMedline(String filename)
    {
	File f = new File(filename);

	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}

	// Obtain a factory object for creating SAX parsers
	SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	// Configure the factory object to specify attributes of the parsers it creates
	parserFactory.setValidating(true);
	parserFactory.setNamespaceAware(true);

	// Now create a SAXParser object
	ArrayList bibItems=null;
	try{
	    SAXParser parser = parserFactory.newSAXParser();   //May throw exceptions
	    MedlineHandler handler = new MedlineHandler();
	    // Start the parser. It reads the file and calls methods of the handler.
	    parser.parse(new File(filename), handler);

	    // When you're done, report the results stored by your handler object
	    bibItems = handler.getItems();
	}
	catch(javax.xml.parsers.ParserConfigurationException e1){}
	catch(org.xml.sax.SAXException e2){}
	catch(java.io.IOException e3){}
	return bibItems;
    }

    public static ArrayList readBibTeXML(String filename)
    {
        File f = new File(filename);
        if(!f.exists() && !f.canRead() && !f.isFile()){
            System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
            return null;
        }

        // Obtain a factory object for creating SAX parsers
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        // Configure the factory object to specify attributes of the parsers it creates
        // parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);

        // Now create a SAXParser object
        ArrayList bibItems= new ArrayList();
        try{
            SAXParser parser = parserFactory.newSAXParser();   //May throw exceptions
            BibTeXMLHandler handler = new BibTeXMLHandler();
            // Start the parser. It reads the file and calls methods of the handler.
            parser.parse(new File(filename), handler);
            // When you're done, report the results stored by your handler object
            bibItems = handler.getItems();

        }
        catch(javax.xml.parsers.ParserConfigurationException e1){}
        catch(org.xml.sax.SAXException e2){}
        catch(java.io.IOException e3){}
        return bibItems;
    }

    //==================================================
    //
    //==================================================
    public static ArrayList fetchMedline(String id)
    {

	ArrayList bibItems=null;
	try {

	    String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" + id;

	    URL url = new URL( baseUrl );
	    HttpURLConnection data = (HttpURLConnection)url.openConnection();

	    // Obtain a factory object for creating SAX parsers
	    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	    // Configure the factory object to specify attributes of the parsers it creates
	    parserFactory.setValidating(true);
	    parserFactory.setNamespaceAware(true);

	    // Now create a SAXParser object
	    SAXParser parser = parserFactory.newSAXParser();   //May throw exceptions
	    MedlineHandler handler = new MedlineHandler();
	    // Start the parser. It reads the file and calls methods of the handler.
	    parser.parse( data.getInputStream(), handler);
	    // When you're done, report the results stored by your handler object
	    bibItems = handler.getItems();

	}
	catch(javax.xml.parsers.ParserConfigurationException e1){}
	catch(org.xml.sax.SAXException e2){}
	catch(java.io.IOException e3){}
	return bibItems;
    }

    //========================================================
    //
    //========================================================
    public static ArrayList readINSPEC( String filename)
    {
	ArrayList bibitems = new ArrayList();
	File f = new File(filename);

	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}
	StringBuffer sb=new StringBuffer();
	try {
	    BufferedReader in = new BufferedReader(new FileReader( filename));
	    String str;
	    while((str=in.readLine())!=null){
		if(str.length() < 2) continue;
		if(str.indexOf("Record")==0)
		    sb.append("__::__"+str);
		else
		    sb.append("__NEWFIELD__"+str);
	    }
	    in.close();
	    String[] entries = sb.toString().split("__::__");
	    String Type="";
	    HashMap h=new HashMap();
	    for(int i=0; i<entries.length; i++){
		if(entries[i].indexOf("Record") != 0)
		    continue;
		h.clear();

		String[] fields = entries[i].split("__NEWFIELD__");
		for(int j=0; j<fields.length; j++){
		    //System.out.println(fields[j]);
		    String s = fields[j];
		    String f3 = s.substring(0,2);
		    String frest = s.substring(5);
		    if(f3.equals("TI")) h.put("title", frest);
		    else if(f3.equals("PY")) h.put("year", frest);
		    else if(f3.equals("AU")) h.put("author", fixAuthor_lastnameFirst(frest.replaceAll(",-",", ").replaceAll(";"," and ")));
		    else if(f3.equals("AB")) h.put("abstract", frest);
		    else if(f3.equals("ID")) h.put("keywords", frest);
		    else if(f3.equals("SO")){
			int m = frest.indexOf(".");
			if(m >= 0){
			    String jr = frest.substring(0,m);
			    h.put("journal",jr.replaceAll("-"," "));
			    frest = frest.substring(m);
			    m = frest.indexOf(";");
			    if(m>=5){
				String yr = frest.substring(m-5,m);
				h.put("year",yr);
				frest = frest.substring(m);
				m = frest.indexOf(":");
				if(m>=0){
				    String pg = frest.substring(m+1).trim();
				    h.put("pages",pg);
				    h.put("volume",frest.substring(1,m));
				}
			    }
			}

		    }
		    else if(f3.equals("RT")){
			frest=frest.trim();
			if(frest.equals("Journal-Paper"))
			    Type="article";
			else if(frest.equals("Conference-Paper") || frest.equals("Conference-Paper; Journal-Paper"))
			    Type="inproceedings";
			else
			    Type=frest.replaceAll(" ","");
		    }
		}
		BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
									  Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
		b.setField( h);

		bibitems.add( b  );

	    }
	}
	catch(IOException e){ return null;}

	return bibitems;
    }

    //========================================================
    //
    //========================================================
    public static ArrayList readSilverPlatter( String filename)
    {
	ArrayList bibitems = new ArrayList();
	File f = new File(filename);

	if(!f.exists() && !f.canRead() && !f.isFile()){
	    System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
	    return null;
	}
	StringBuffer sb=new StringBuffer();
	try {
	    BufferedReader in = new BufferedReader(new FileReader( filename));
	    String str;
	    while((str=in.readLine())!=null){
		if(str.length() < 2) 
		    sb.append("__::__"+str);
		else
		    sb.append("__NEWFIELD__"+str);
	    }
	    in.close();
	    String[] entries = sb.toString().split("__::__");
	    String Type="";
	    HashMap h=new HashMap();
	    entryLoop: for(int i=0; i<entries.length; i++){
		if (entries[i].trim().length() < 6)
		    continue entryLoop;
		//System.out.println("'"+entries[i]+"'");
		h.clear();
		String[] fields = entries[i].split("__NEWFIELD__");
		fieldLoop: for(int j=0; j<fields.length; j++){
		    if (fields[j].length() < 6)
			continue fieldLoop;
		    //System.out.println(">"+fields[j]+"<");
		    String s = fields[j];
		    String f3 = s.substring(0,2);
		    String frest = s.substring(5);
		    if(f3.equals("TI")) h.put("title", frest);
		    //else if(f3.equals("PY")) h.put("year", frest);
		    else if(f3.equals("AU")) {
			if (frest.trim().endsWith("(ed)")) {
			    String ed = frest.trim();
			    ed = ed.substring(0, ed.length()-4);
			    h.put("editor", fixAuthor_lastnameFirst(ed.replaceAll(",-",", ").replaceAll(";"," and ")));
			} else			
			    h.put("author", fixAuthor_lastnameFirst(frest.replaceAll(",-",", ").replaceAll(";"," and ")));
		    }
		    else if(f3.equals("AB")) h.put("abstract", frest);
		    else if(f3.equals("DE")) {
			String kw = frest.replaceAll("-;", ",").toLowerCase();
			h.put("keywords", kw.substring(0, kw.length()-1));
		    }
		    else if(f3.equals("SO")){
			int m = frest.indexOf(".");
			if(m >= 0){
			    String jr = frest.substring(0,m);
			    h.put("journal",jr.replaceAll("-"," "));
			    frest = frest.substring(m);
			    m = frest.indexOf(";");
			    if(m>=5){
				String yr = frest.substring(m-5,m).trim();
				h.put("year",yr);
				frest = frest.substring(m);
				m = frest.indexOf(":");
				if(m>=0){
				    String pg = frest.substring(m+1).trim();
				    h.put("pages",pg);
				    h.put("volume",frest.substring(1,m));
				}
			    }
			}
		    }
		    else if(f3.equals("PB")){
			int m = frest.indexOf(":");
			if(m >= 0){
			    String jr = frest.substring(0,m);
			    h.put("publisher",jr.replaceAll("-"," ").trim());
			    frest = frest.substring(m);
			    m = frest.indexOf(", ");
			    if(m+2<frest.length()){
				String yr = frest.substring(m+2).trim();
				h.put("year",yr);
			    }
			}
		    }
		    else if(f3.equals("DT")){
			frest=frest.trim();
			if(frest.equals("Monograph"))
			    Type="book";
			else if(frest.equals("Journal-Article"))
			    Type="article";
			else if(frest.equals("Contribution")) {
			    Type="incollection";
			    // This entry type contains page numbers and booktitle in the title field.
			    // We assume the title field has been set already.
			    String title = ((String)h.get("title")).trim();
			    if (title != null) {
				int inPos = title.indexOf("\" in ");
				int pgPos = title.lastIndexOf(" ");
				if (inPos > 1)
				    h.put("title", title.substring(1, inPos));
				if (pgPos > inPos)
				    h.put("pages", title.substring(pgPos).replaceAll("-", "--"));
				
			    }
			}

			else
			    Type=frest.replaceAll(" ","");
		    }
		}
		BibtexEntry b=new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID,
					      Globals.getEntryType(Type)); // id assumes an existing database so don't create one here
		b.setField( h);

		bibitems.add( b  );

	    }
	}
	catch(IOException e){ return null;}

	return bibitems;
    }

    //==================================================
    // Set a field, unless the string to set is empty.
    //==================================================
    private static void setIfNecessary(BibtexEntry be, String field, String content) {
      if (!content.equals(""))
        be.setField(field, content);
    }

    //==================================================
    // Import a file in the JStor format
    //==================================================
    public static ArrayList readJStorFile(String filename) {
      ArrayList bibitems = new ArrayList();
      File f = new File(filename);
      if (!f.exists() || !f.canRead() || !f.isFile()) {
        System.err.println("Error: " + filename + " is not a valid file and|or is not readable.");
        return null;
      }

      String s = "";
      try {
        BufferedReader in = new BufferedReader(new FileReader(filename));

        while (!s.startsWith("Item Type"))
          s = in.readLine();

        mainloop: while ((s = in.readLine()) != null) {
          if (s.equals(""))
            continue;
          if (s.startsWith("-----------------------------"))
            break mainloop;
          String[] fields = s.split("\t");
          BibtexEntry be = new BibtexEntry(Util.createNeutralId());
          try {
            if (fields[0].equals("FLA"))
              be.setType(BibtexEntryType.getType("article"));
            setIfNecessary(be, "title", fields[2]);
            setIfNecessary(be, "author", fields[4].replaceAll("; ", " and "));
            setIfNecessary(be, "journal", fields[7]);
            setIfNecessary(be, "volume", fields[9]);
            setIfNecessary(be, "number", fields[10]);
            String[] datefield = fields[12].split(" ");
            setIfNecessary(be, "year", datefield[datefield.length-1]);
            //for (int i=0; i<fields.length; i++)
            //  Util.pr(i+": "+fields[i]);
            setIfNecessary(be, "pages", fields[13].replaceAll("-", "--"));
            setIfNecessary(be, "url", fields[14]);
            setIfNecessary(be, "issn", fields[15]);
            setIfNecessary(be, "abstract", fields[16]);
            setIfNecessary(be, "keywords", fields[17]);
            setIfNecessary(be, "copyright", fields[21]);
          } catch (ArrayIndexOutOfBoundsException ex) {}
          bibitems.add(be);
        }

      } catch (IOException ex) {
        Util.pr("Err: "+s);
      }
      return bibitems;
    }

    /**
     * Imports a Biblioscape Tag File. The format is described on
     * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm
     * Several Biblioscape field types are ignored. Others are only included in the
     * BibTeX field "comment".
    */
   public static ArrayList readBiblioscapeTagFile(String filename) {
     ArrayList bibitems = new ArrayList();
     File f = new File(filename);
     if (!f.exists() || !f.canRead() || !f.isFile()) {
       System.err.println("Error: " + filename + " is not a valid file and|or is not readable.");
       return null;
     }

     try {
       BufferedReader in = new BufferedReader(new FileReader(filename));
       String line;
       HashMap hm = new HashMap();
       HashMap lines = new HashMap();
       StringBuffer previousLine = null;
       while ((line = in.readLine()) != null) {
         // entry delimiter -> item complete
         if (line.equals("------")) {
           String[] type = new String[2];
           String[] pages = new String[2];
           String country = null;
           String address = null;
           Vector comments = new Vector();
           // add item
           Object[] l = lines.entrySet().toArray();
           for (int i = 0; i < l.length; ++i) {
             Map.Entry entry = (Map.Entry)l[i];
             if (entry.getKey().equals("AU")) hm.put("author",entry.getValue().toString());
             else if (entry.getKey().equals("TI")) hm.put("title",entry.getValue().toString());
             else if (entry.getKey().equals("ST")) hm.put("booktitle",entry.getValue().toString());
             else if (entry.getKey().equals("YP")) hm.put("year",entry.getValue().toString());
             else if (entry.getKey().equals("VL")) hm.put("volume",entry.getValue().toString());
             else if (entry.getKey().equals("NB")) hm.put("number",entry.getValue().toString());
             else if (entry.getKey().equals("PS")) pages[0] = entry.getValue().toString();
             else if (entry.getKey().equals("PE")) pages[1] = entry.getValue().toString();
             else if (entry.getKey().equals("KW")) hm.put("keywords",entry.getValue().toString());
             //else if (entry.getKey().equals("RM")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("RU")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("RT")) type[0] = entry.getValue().toString();
             else if (entry.getKey().equals("SB")) comments.add("Subject: " + entry.getValue().toString());
             else if (entry.getKey().equals("SA")) comments.add("Secondary Authors: " + entry.getValue().toString());
             else if (entry.getKey().equals("NT")) hm.put("note",entry.getValue().toString());
             //else if (entry.getKey().equals("PP")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("PB")) hm.put("publisher",entry.getValue().toString());
             else if (entry.getKey().equals("TA")) comments.add("Tertiary Authors: " + entry.getValue().toString());
             else if (entry.getKey().equals("TT")) comments.add("Tertiary Title: " + entry.getValue().toString());
             else if (entry.getKey().equals("ED")) hm.put("edition",entry.getValue().toString());
             //else if (entry.getKey().equals("DP")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("TW")) type[1] = entry.getValue().toString();
             else if (entry.getKey().equals("QA")) comments.add("Quaternary Authors: " + entry.getValue().toString());
             else if (entry.getKey().equals("QT")) comments.add("Quaternary Title: " + entry.getValue().toString());
             else if (entry.getKey().equals("IS")) hm.put("isbn",entry.getValue().toString());
             //else if (entry.getKey().equals("LA")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("AB")) hm.put("abstract",entry.getValue().toString());
             //else if (entry.getKey().equals("DI")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("DM")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("AV")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("PR")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("LO")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("AD")) address = entry.getValue().toString();
             else if (entry.getKey().equals("LG")) hm.put("language",entry.getValue().toString());
             else if (entry.getKey().equals("CO")) country = entry.getValue().toString();
             else if (entry.getKey().equals("UR") || entry.getKey().equals("AT")) {
               String s = entry.getValue().toString().trim();
               hm.put(s.startsWith("http://") || s.startsWith("ftp://") ? "url" : "pdf",
                      entry.getValue().toString());
             }
             else if (entry.getKey().equals("C1")) comments.add("Custom1: " + entry.getValue().toString());
             else if (entry.getKey().equals("C2")) comments.add("Custom2: " + entry.getValue().toString());
             else if (entry.getKey().equals("C3")) comments.add("Custom3: " + entry.getValue().toString());
             else if (entry.getKey().equals("C4")) comments.add("Custom4: " + entry.getValue().toString());
             //else if (entry.getKey().equals("RD")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("MB")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("C5")) comments.add("Custom5: " + entry.getValue().toString());
             else if (entry.getKey().equals("C6")) comments.add("Custom6: " + entry.getValue().toString());
             //else if (entry.getKey().equals("FA")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("CN")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("DE")) hm.put("annote",entry.getValue().toString());
             //else if (entry.getKey().equals("RP")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("DF")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("RS")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("CA")) comments.add("Categories: " + entry.getValue().toString());
             //else if (entry.getKey().equals("WP")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("TH")) comments.add("Short Title: " + entry.getValue().toString());
             //else if (entry.getKey().equals("WR")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("EW")) hm.put("",entry.getValue().toString());
             else if (entry.getKey().equals("SE")) hm.put("chapter",entry.getValue().toString());
             //else if (entry.getKey().equals("AC")) hm.put("",entry.getValue().toString());
             //else if (entry.getKey().equals("LP")) hm.put("",entry.getValue().toString());
           }

           String bibtexType = "misc";
           if (type[1] != null) { // first check TW
             type[1] = type[1].toLowerCase();
             if (type[1].indexOf("article") >= 0) bibtexType = "article";
             else if (type[1].indexOf("book") >= 0) bibtexType = "book";
             else if (type[1].indexOf("conference") >= 0) bibtexType = "inproceedings";
             else if (type[1].indexOf("proceedings") >= 0) bibtexType = "inproceedings";
             else if (type[1].indexOf("report") >= 0) bibtexType = "techreport";
             else if (type[1].indexOf("thesis") >= 0
                      && type[1].indexOf("master") >= 0) bibtexType = "mastersthesis";
             else if (type[1].indexOf("thesis") >= 0) bibtexType = "phdthesis";
           } else if (type[0] != null) { // check RT
             type[0] = type[0].toLowerCase();
             if (type[0].indexOf("article") >= 0) bibtexType = "article";
             else if (type[0].indexOf("book") >= 0) bibtexType = "book";
             else if (type[0].indexOf("conference") >= 0) bibtexType = "inproceedings";
             else if (type[0].indexOf("proceedings") >= 0) bibtexType = "inproceedings";
             else if (type[0].indexOf("report") >= 0) bibtexType = "techreport";
             else if (type[0].indexOf("thesis") >= 0
                      && type[0].indexOf("master") >= 0) bibtexType = "mastersthesis";
             else if (type[0].indexOf("thesis") >= 0) bibtexType = "phdthesis";
           }

           // concatenate pages
           if (pages[0] != null || pages[1] != null)
             hm.put("pages",(pages[0] != null ? pages[0] : "")
                    + (pages[1] != null ? "--" + pages[1] : ""));

             // concatenate address and country
           if (address != null)
             hm.put("address",address + (country != null ? ", " + country : ""));

           if (comments.size() > 0) { // set comment if present
             StringBuffer s = new StringBuffer();
             for (int i = 0; i < comments.size(); ++i)
               s.append((i > 0 ? "; " : "")+ comments.elementAt(i).toString());
             hm.put("comment",s.toString());
           }
           BibtexEntry b = new BibtexEntry(
      Globals.DEFAULT_BIBTEXENTRY_ID,
   Globals.getEntryType(bibtexType));
     b.setField(hm);
     bibitems.add(b);

     hm.clear();
     lines.clear();
     previousLine = null;

     continue;
   }
   // new key
   if (line.startsWith("--") && line.length() >= 7 && line.substring(4,7).equals("-- ")) {
     lines.put(line.substring(2,4),previousLine = new StringBuffer(line.substring(7)));
     continue;
   }
   // continuation (folding) of previous line
   if (previousLine == null) // sanity check; should never happen
     return null;
   previousLine.append(line.trim());
 }
  } catch (IOException e) {
    return null;
  }

  return bibitems;
}

public static ParserResult loadDatabase(File fileToOpen, String encoding) throws IOException {
  // Temporary (old method):
  //FileLoader fl = new FileLoader();
  //BibtexDatabase db = fl.load(fileToOpen.getPath());

  Reader reader = getReader(fileToOpen, encoding);
  String suppliedEncoding = null;
  try {
    boolean keepon = true;
    int piv = 0, c;
    while (keepon) {
      c = reader.read();
      if ( (piv == 0 && Character.isWhitespace( (char) c)) ||
          c == GUIGlobals.SIGNATURE.charAt(piv))
        piv++;
      else
        keepon = false;
      found: if (piv == GUIGlobals.SIGNATURE.length()) {
        keepon = false;
        // Found the signature. The rest of the line is unknown, so we skip it:
        while (reader.read() != '\n');
        // Then we must skip the "Encoding: "
        for (int i=0; i<GUIGlobals.encPrefix.length(); i++) {
          if (reader.read() != GUIGlobals.encPrefix.charAt(i))
            break found; // No, it doesn't seem to match.
        }
        // If ok, then read the rest of the line, which should contain the name
        // of the encoding:
        StringBuffer sb = new StringBuffer();
        while ((c = reader.read()) != '\n')
          sb.append((char)c);
        suppliedEncoding = sb.toString();
      }

    }
  } catch (IOException ex) {}

  if ((suppliedEncoding != null) && (!suppliedEncoding.equalsIgnoreCase(encoding))) {
    Reader oldReader = reader;
    try {
      // Ok, the supplied encoding is different from our default, so we must make a new
      // reader. Then close the old one.
      reader = getReader(fileToOpen, suppliedEncoding);
      oldReader.close();
      //System.out.println("Using encoding: "+suppliedEncoding);
    } catch (IOException ex) {
      reader = oldReader; // The supplied encoding didn't work out, so we keep our
      // existing reader.

      //System.out.println("Error, using default encoding.");
    }
  } else {
    // We couldn't find a supplied encoding. Since we don't know far into the file we read,
    // we start a new reader.
    reader.close();
    reader = getReader(fileToOpen, encoding);
    //System.out.println("No encoding supplied, or supplied encoding equals default. Using default encoding.");
  }


    //return null;

  BibtexParser bp = new BibtexParser(reader);

  ParserResult pr = bp.parse();
  pr.setEncoding(encoding);

  return pr;
}

public static Reader getReader(File f, String encoding) throws IOException{
  InputStreamReader reader;
  reader = new InputStreamReader(new FileInputStream(f), encoding);
  return reader;
}

public static BibtexDatabase importFile(String format, String filename) throws IOException {
  BibtexDatabase database = null;
  ArrayList bibentries = null;
  File f = new File(filename);
  if (!f.exists())
    throw new IOException(Globals.lang("File not found")+": "+filename);

  if (format.equals("endnote"))
    bibentries = readEndnote(filename);
  else if (format.equals("medline"))
    bibentries = fetchMedline(filename);
  else if (format.equals("biblioscape"))
    bibentries = readBiblioscapeTagFile(filename);
  else if (format.equals("sixpack"))
    bibentries = readSixpack(filename);
  else if (format.equals("bibtexml"))
    bibentries = readBibTeXML(filename);
  else if (format.equals("inspec"))
    bibentries = readINSPEC(filename);
  else if (format.equals("isi"))
    bibentries = readISI(filename);
  else if (format.equals("ovid"))
    bibentries = readOvid(filename);
  else if (format.equals("ris"))
    bibentries = readReferenceManager10(filename);
  else if (format.equals("scifinder"))
    bibentries = readScifinder(filename);
  else if (format.equals("jstor"))
    bibentries = readJStorFile(filename);
  else if (format.equals("silverplatter"))
    bibentries = readSilverPlatter(filename);
 else
    throw new IOException(Globals.lang("Could not resolve import format")+" '"+format+"'");

  if (bibentries == null)
    throw new IOException(Globals.lang("Import failed"));

    // Add entries to database.
    database = new BibtexDatabase();
  Iterator it = bibentries.iterator();
  while (it.hasNext()) {
    BibtexEntry entry = (BibtexEntry) it.next();
    try {
      entry.setId(Util.createNeutralId());
      database.insertEntry(entry);
    }
    catch (KeyCollisionException ex) {
      //ignore
      System.err.println("KeyCollisionException [ addBibEntries(...) ]");
    }
  }


  return database;
}



}
