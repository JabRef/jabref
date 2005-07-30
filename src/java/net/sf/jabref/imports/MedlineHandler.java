package net.sf.jabref.imports;
import java.util.regex.*;
import javax.xml.parsers.*;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import net.sf.jabref.*;


/*
  Copyright (C) 2002-2003 Morten O. Alver & Nizar N. Batada
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

public class MedlineHandler extends DefaultHandler
{
    ArrayList bibitems= new ArrayList();
    boolean inTitle=false,			inYear = false,
		inJournal = false,			inMonth = false,
		inVolume = false,			inAuthorList = false,
		inAuthor =false,			inLastName = false,
		inInitials = false,			inMedlinePgn = false,
		inMedlineID = false,		inURL=false,
		inIssue = false,			inPubDate = false,
		inUrl=false, inForename=false, inAbstractText=false, inMedlineDate=false,
		inPubMedID=false, inDescriptorName=false,inDoi=false,inPii=false;
    String title="", journal="", keyword="",author="",
		lastName="",year="",forename="", abstractText="";
    String month="",volume="",lastname="",initials="",number="",page="",medlineID="",url="",MedlineDate="";
    String series="",editor="",booktitle="",type="article",key="",address="",
		pubmedid="", descriptorName="",doi="",pii="";
    ArrayList authors=new ArrayList();
    int rowNum=0;
    public ArrayList getItems(){ return bibitems;}

    public MedlineHandler(){
		super();

    }
    public void startElement(String uri, String localName, String qName,  Attributes atts)
    {
		//		public void startElement(String localName, Attributes atts) {
		// Get the number of attribute
		if(localName.equals("PubmedArticle")){}
		else if(localName.equals("ArticleTitle")){ inTitle=true; title="";}
		else if(localName.equals("PubDate")){inPubDate=true;}
		else if(localName.equals("Year") && inPubDate==true){inYear=true;}
		else if( localName.equals("MedlineDate") && inPubDate==true){inMedlineDate=true;} // medline date does not have 4 digit dates instead it has multiyear etc
		else if(localName.equals("MedlineTA")){inJournal=true;journal="";} //journal name
		else if(localName.equals("Month") && inPubDate==true){inMonth=true;}
		else if(localName.equals("Volume")){inVolume=true;}
		else if(localName.equals("AuthorList")){
			inAuthorList=true;
			authors.clear();}
		else if(localName.equals("DescriptorName")){
			//keyword="";
			inDescriptorName=true;
			//descriptorName="";
		}
                else if(localName.equals("Author")){inAuthor=true;author="";}
                else if(localName.equals("CollectiveName")){inForename=true;forename="";} // Morten A. 20040513.
		else if(localName.equals("PMID")){inPubMedID=true;pubmedid="";}
		else if(localName.equals("LastName")){inLastName=true; lastName="";}
		else if(localName.equals("ForeName") || localName.equals("FirstName")) {
			inForename=true; forename="";
		}
		else if(localName.equals("Issue")){inIssue=true;}
		else if(localName.equals("MedlinePgn")){inMedlinePgn=true;
		}//pagenumber
		else if(localName.equals("URL")){inUrl=true;}
		else if(localName.equals("Initials")){inInitials=true;}
		else if(localName.equals("AbstractText")){ inAbstractText=true;}
		else if(localName.equals("ArticleId")){
			for (int i = 0; i < atts.getLength(); i++) {
				String name = atts.getQName(i);
				String type = atts.getType(i);
				String value = atts.getValue(i);
				//System.out.println("name:" + name + " type: " + type + " value: " + value);
				if(value.equals("doi"))
					inDoi=true;
				else if(value.equals("pii"))
					inPii=true;

			}
		}



		return;
    }
    String join(Object[] sa,String delim){
		StringBuffer sb=new StringBuffer();
		sb.append( sa[0].toString() );
		for(int i=1; i<sa.length; i++)
	    {
			sb.append( delim );
			sb.append( sa[i].toString() );
	    }
		return sb.toString();
    }
    String makeBibtexString(){
		String out  = "";
                // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added call to fixPageRange
		out= "article{,\n" + " author = { " + author + " },\n title = { " + title + "},\n journal ={ " + journal + "},\n year = " + year +
			"},\n volume = { " + volume + "},\n number = { "+ number + "},\n pages = { " + fixPageRange(page) + "},\n abstract = { " + abstractText + "},\n}";
		return out;
    }
    public void endElement( String uri, String localName, String qName ) {
		if(localName.equals("PubmedArticle")){
			//bibitems.add( new Bibitem(null, makeBibtexString(), Globals.nextKey(),"-1" )	 );
			// check if year ="" then give medline date instead
			if(year.equals("")){
				if(!MedlineDate.equals("")) {
					// multi-year date format
					//System.out.println(MedlineDate);
					year = MedlineDate.substring(0,4);
					//Matcher m = Pattern.compile("\\b[0-9]{4}\\b").matcher(MedlineDate);
					//if(m.matches())
					//year = m.group();
				}
			}
			//################################## 09/23/03  put {} around capitals

			title=Util.putBracesAroundCapitals(title);
			//##############################
			// Sort keywords and remove duplicates. Add pubmedid as keyword (user request)
            StringBuffer sb = new StringBuffer(Util.sortWordsAndRemoveDuplicates(descriptorName));
            if (sb.length()>0)
                sb.append(", ");
            sb.append(pubmedid);
            keyword = sb.toString();
            
			BibtexEntry b=new BibtexEntry(Util.createNeutralId(),//Globals.DEFAULT_BIBTEXENTRY_ID,
										  Globals.getEntryType("article")); // id assumes an existing database so don't create one here
			if (!author.equals("")) { 
			    b.setField("author",ImportFormatReader.expandAuthorInitials(author));
			    author = "";
			}
			if (!title.equals("")) b.setField("title",title);
			if (!journal.equals("")) b.setField("journal",journal);
			if (!year.equals("")) b.setField("year",year);
                        // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added call to fixPageRange
			if (!page.equals("")) b.setField("pages",fixPageRange(page));
			if (!volume.equals("")) b.setField("volume",volume);
			if (!abstractText.equals("")) b.setField("abstract",abstractText.replaceAll("%","\\\\%"));
			if (!keyword.equals("")) b.setField("keywords",keyword);
			if (!month.equals("")) b.setField("month",month);
			//if (!url.equals("")) b.setField("url",url);
			if (!number.equals("")) b.setField("number",number);

			if(!doi.equals("")){
			    b.setField("doi",doi);
			    b.setField("url","http://dx.doi.org/"+doi);
			}
			if(!pii.equals(""))
			    b.setField("pii",pii);

                        // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added "pmid" bibtex field
                        // Older references do not have doi entries, but every
                        // medline entry has a unique pubmed ID (aka primary ID).
                        // Add a bibtex field for the pubmed ID for future use.
                        if (!pubmedid.equals(""))
                            b.setField("pmid",pubmedid);
                        
			bibitems.add( b  );

			abstractText = "";
			author = "";
			title="";
			journal="";
			keyword="";
			doi=""; pii="";
			year="";
			forename="";
			lastName="";
			abstractText="";
			pubmedid="";
			month="";volume="";lastname="";initials="";number="";page="";medlineID="";url="";
			MedlineDate="";
		}

		else if(localName.equals("ArticleTitle")){inTitle=false;}
		else if(localName.equals("PubDate")){inPubDate=false;}
		else if(localName.equals("Year")){inYear=false;}
		else if(localName.equals("PMID")){inPubMedID=false;}
		else if(localName.equals("MedlineDate")){inMedlineDate=false;}
		else if(localName.equals("MedlineTA")){inJournal=false;} //journal name
		else if(localName.equals("Month")){inMonth=false;}
		else if(localName.equals("Volume")){inVolume=false;}
		else if(localName.equals("AuthorList")){
			author = join( authors.toArray(), " and " );
			inAuthorList = false;
		}
		else if(localName.equals("Author")){
			// forename sometimes has initials with " " in middle: is pattern [A-Z] [A-Z]
			// when above is the case replace it with initials
			if(forename.length()==3 && forename.charAt(1)==' '){
				forename=initials;
			}
			author = forename + " " + lastname;
			//author = initials + " " + lastname;
			authors.add(author);
			inAuthor=false;
			forename = "";
			initials = "";
			lastname = "";
		}
		else if(localName.equals("DescriptorName")) inDescriptorName=false;
		else if(localName.equals("LastName")){inLastName=false;}
		else if(localName.equals("ForeName")||localName.equals("FirstName")){ inForename=false;}
		else if(localName.equals("Issue")){ inIssue = false;}
		else if(localName.equals("MedlinePgn")){inMedlinePgn=false;}//pagenumber
		else if(localName.equals("URL")){ inUrl=false;}
		else if(localName.equals("Initials")){
			//initials= '.' + initials + '.';
			inInitials=false;
		}
		else if(localName.equals("AbstractText")){ inAbstractText=false;}
		else if(localName.equals("ArticleId")){
			if(inDoi)
				inDoi=false;
			else if(inPii)
				inPii=false;}
    }

    public void characters( char[] data, int start, int length ) {

		// if stack is not ready, data is not content of recognized element
		if( inTitle ){ title += new String( data, start, length);}
		else if(inYear){ year+=new String(data,start,length);}
		else if(inJournal){journal += new String(data,start,length);}
		else if(inMonth){month += new String(data,start,length);}
		else if(inVolume){volume += new String(data,start,length);}
		else if(inLastName){lastname += new String(data,start,length);}
		else if(inInitials){initials += new String(data,start,length);}
		else if(inIssue){number += new String(data,start,length);}
		else if(inMedlinePgn){ page += new String(data,start,length);}
		else if(inMedlineID){medlineID += new String(data,start,length);}
		else if(inURL){url += new String(data,start,length);}
		else if(inPubMedID){pubmedid = new String(data,start,length);}
		else if(inDescriptorName) descriptorName += new String(data,start,length) + ", ";
		else if(inForename){
			forename += new String(data,start,length);
			//System.out.println("IN FORENAME: " + forename);
		}
		else if(inAbstractText){ abstractText += new String(data,start,length);}
		else if(inMedlineDate){ MedlineDate += new String(data,start,length);}
		else if(inDoi){ doi=new String(data,start,length);}
		else if(inPii){ pii=new String(data,start,length);}
    }

    // PENDING jeffrey.kuhn@yale.edu 2005-05-27 : added fixPageRange method
    //   Convert medline page ranges from short form to full form.
    //   Medline reports page ranges in a shorthand format. 
    //   The last page is reported using only the digits which
    //   differ from the first page. 
    //      i.e. 12345-51 refers to the actual range 12345-12351
    public String fixPageRange(String pageRange) {
        int minusPos = pageRange.indexOf('-');
        if (minusPos < 0) {
            return pageRange;
        }
        String first = pageRange.substring(0, minusPos).trim();
        String last = pageRange.substring(minusPos+1).trim();
        int llast = last.length(), lfirst = first.length();
        if (llast < lfirst) {
            last = first.substring(0, lfirst-llast) + last;
        }
        return first + "--" + last;
    }
}
