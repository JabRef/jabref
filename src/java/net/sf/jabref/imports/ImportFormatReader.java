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
	StringBuffer sb=new StringBuffer();
	String[] authors = in.split(" and ");
	for(int i=0; i<authors.length; i++){
	    //System.out.println(authors[i]);
	    authors[i]=authors[i].trim();
	    String[] t = authors[i].split(" ");
	    sb.append( t[1].trim() + " " + t[0].trim());
	    if(i==authors.length-1)
		sb.append(".");
	    else
		sb.append(" and ");
	}
	return sb.toString();
    }


    //========================================================
    // rearranges the author names
    // input format string: LN, FN [and LN, FN]*
    // output format string: FN LN [and FN LN]*
    //========================================================

    public static String fixAuthor(String in){
	//Util.pr("firstnamefirst");
	StringBuffer sb=new StringBuffer();
	//System.out.println("FIX AUTHOR: in= " + in);

	String[] authors = in.split(" and ");
	for(int i=0; i<authors.length; i++){
	    String[] t = authors[i].split(",");
	    if(t.length < 2)
		return in; // something went wrong or there is no ","
	    sb.append( t[1].trim() + " " + t[0].trim());
	    if(i != authors.length-1 ) // put back the " and "
		sb.append(" and ");
	}
	return sb.toString();
    }

    //========================================================
    // rearranges the author names
    // input format string: LN, FN [and LN, FN]*
    // output format string: LN, FN [and LN, FN]*
    //========================================================
    public static String fixAuthor_lastnameFirst(String in){
	//Util.pr("lastnamefirst: in");
	StringBuffer sb=new StringBuffer();

	String[] authors = in.split(" and ");
	for(int i=0; i<authors.length; i++){
	    String[] t = authors[i].split(",");
	    if(t.length < 2) {
		// The name is without a comma, so it must be rearranged.
		t = authors[i].split(" ");
		if (t.length > 1) {
		    sb.append(t[t.length - 1]+ ","); // Last name
		    for (int j=0; j<t.length-1; j++)
			sb.append(" "+t[j]);
		} else sb.append(t[0]);
	    }
	    else {
		// The name is written with last name first, so it's ok.
		sb.append(authors[i]);
	    }

	    if(i !=authors.length-1)
		sb.append(" and ");

	}
	//Util.pr(in+" -> "+sb.toString());
	return sb.toString();
    }


    public static ArrayList readSixpack(String filename) {
      final String SEPARATOR = new String(new char[] {0, 48});
      ArrayList bibitems=new ArrayList();
      File f = new File(filename);

      if(!f.exists() && !f.canRead() && !f.isFile()){
        System.err.println("Error " + filename + " is not a valid file and|or is not readable.");
        return null;
      }

      try{
              BufferedReader in = new BufferedReader(new FileReader( filename));
              Util.pr(in.readLine());
              Util.pr(in.readLine());
              String s = null;
              BibtexEntry entry = null;
              while ((s = in.readLine()) != null) {
                try {
                  s = s.replaceAll("<par>", ""); // What is <par> ????
                  String[] fields = s.split(SEPARATOR);
                  // Check type and create entry:
                  if (fields[1].equals("Article"))
                    entry = new BibtexEntry(Util.createNeutralId(),
                                            BibtexEntryType.getType("article"));

                    // Set fields:
                  entry.setField("author", fields[2].replaceAll(" and ", ", ").replaceAll(", ", " and "));
                  // Regarding authors, it appears Sixpack saves with first name first, and normal grammar.
                  entry.setField("title", fields[3]);
                  entry.setField("journal", fields[4]);
                  entry.setField("volume", fields[5]);
                  entry.setField("number", fields[6]);
                  entry.setField("pages", fields[7].replaceAll("-", "--"));
                  entry.setField("month", fields[8]);
                  entry.setField("year", fields[9]);
                  entry.setField("abstract", fields[12]);

                  bibitems.add(entry);
                //         Util.pr(fields[1] + "   (" + fields.length + ")");
                } catch (NullPointerException ex) {
                  Globals.logger("Error parsing Sixpack file, ignoring entry.");
                }
              }
              //for (int i=0; i<s.length; i++)
              //  Util.pr(":"+s[i]);
              /*Util.pr(in.readLine());
              Util.pr(in.readLine());
              Util.pr(in.readLine());

              int str, i=0;
              while ((i < 100) && (str = in.read()) >= 0) {
                      Util.pr(""+(char)str+"  "+str);
                      i++;
              }*/
              in.close();
      }
      catch(IOException e){return null;}


      return bibitems;
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
			for(int j=0; j<fields.length; j++){
				String tmp[]=fields[j].split(":");
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
		    hm.put( "author", fixAuthor( fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," and ") ));
		else if(beg.equals("TI"))
		    hm.put("title", fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("SO")){ // journal name
		    hm.put("journal",fields[j].substring(2,fields[j].length()).trim());
		}
		else if(beg.equals("ID"))
		    hm.put( "keywords",fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("AB"))
		    hm.put("abstract", fields[j].substring(2,fields[j].length()).trim().replaceAll("EOLEOL"," "));
		else if(beg.equals("BP"))
		    //hm.put("pages", fields[j].substring(2,fields[j].length()).trim());
		    pages=fields[j].substring(2,fields[j].length()).trim();
		else if(beg.equals("EP")){
		    pages=pages + "--" + fields[j].substring(2,fields[j].length()).trim();
		}
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
		if(line.length()>0 && line.charAt(0) != ' ')
		    sb.append("__NEWFIELD__");
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
			    if(isComma==false)
				h.put("author",  fixAuthor( author) );
			    else
				h.put("author",  fixAuthor_nocomma( author) );
			}
			else
			    h.put("author",author);
		    }
		    else if(fields[j].indexOf("Title") == 0)
			h.put("title", fields[j].substring(6,fields[j].length()).replaceAll("\\[.+\\]","") );
		    else if(fields[j].indexOf("Source") == 0){
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
              hm.put("author",fixAuthor(Author));
	    if( !Editor.equals(""))
		hm.put("editor",fixAuthor(Editor));
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
		if(fields[j].length() < 2)
		    continue;
		else{
		    String lab = fields[j].substring(0,2);
		    String val = fields[j].substring(6).trim();
		    if(lab.equals("TY")){
			if(val.equals("BOOK")) Type = "book";
			else if (val.equals("JOUR")) Type = "article";
			else Type = "other";
		    }else if(lab.equals("T1"))
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

		}
	    }
	    // fix authors
	    Author = fixAuthor(Author);
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
        parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);

        // Now create a SAXParser object
        ArrayList bibItems=null;
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
		    else if(f3.equals("AU")) h.put("author", fixAuthor(frest.replaceAll(",-",", ").replaceAll(";"," and ")));
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
    //==================================================
    //
    //==================================================


}
