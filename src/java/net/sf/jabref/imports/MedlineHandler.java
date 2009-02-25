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
package net.sf.jabref.imports;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class MedlineHandler extends DefaultHandler
{
    ArrayList<BibtexEntry> bibitems= new ArrayList<BibtexEntry>();
    boolean inTitle=false,			inYear = false,
		inJournal = false,			inMonth = false,
		inVolume = false,			inAuthorList = false,
		inAuthor =false,			inLastName = false,
		inInitials = false,			inMedlinePgn = false,
		inMedlineID = false,		inURL=false,
		inIssue = false,			inPubDate = false,
        inUrl=false, inForename=false, inAbstractText=false, inMedlineDate=false,
		inPubMedID=false, inDescriptorName=false,inDoi=false,inPii=false,
        inAffiliation=false, inMeshHeader=false, inQualifierName=false,
        inLanguage=false, inPst=false;
    String title="", journal="", keywords ="",author="",
		lastName="",year="",forename="", abstractText="", affiliation="";
    String month="",volume="",lastname="",initials="",number="",page="",medlineID="",url="",MedlineDate="";
    String series="",editor="",booktitle="",type="article",key="",address="",
		pubmedid="",doi="",pii="", majorTopic = "", minorTopics = "", language = "", pst= "";
    ArrayList<String> authors=new ArrayList<String>();
    TreeSet<String> descriptors = new TreeSet<String>(); // To gather keywords
    int rowNum=0;

    private static final String KEYWORD_SEPARATOR = "; ";

    public ArrayList<BibtexEntry> getItems(){ return bibitems;}

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
        else if(localName.equals("Language")){inLanguage=true;}
        else if(localName.equals("PublicationStatus")){inPst=true;}
		else if(localName.equals("AuthorList")){
			inAuthorList=true;
			authors.clear();}
        else if (localName.equals("MeshHeading")) {
            inMeshHeader = true;
            majorTopic = "";
            minorTopics = "";
        }
        else if(localName.equals("DescriptorName")){
			inDescriptorName=true;
		}
        else if (localName.equals("QualifierName")) {
            inQualifierName=true;
        }
                else if(localName.equals("Author")){inAuthor=true;author="";}
                else if(localName.equals("CollectiveName")){inForename=true;forename="";} // Morten A. 20040513.
		else if(localName.equals("PMID")){
            // Set PMID only once, because there can be <CommentIn> tags later on that
            // contain IDs of different articles.
            if (pubmedid.length() == 0) {
                inPubMedID=true;
                pubmedid="";
            }
        }
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
				String value = atts.getValue(i);
				if(value.equals("doi"))
					inDoi=true;
				else if(value.equals("pii"))
					inPii=true;

			}
		}
        else if(localName.equals("Affiliation")){ inAffiliation=true; }


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
			
			// Build a string from the collected keywords:
            StringBuffer sb = new StringBuffer();
            for (Iterator<String> iterator = descriptors.iterator(); iterator.hasNext();) {
                String s = iterator.next();
                sb.append(s);
                if (iterator.hasNext())
                    sb.append(KEYWORD_SEPARATOR);
            }
            keywords = sb.toString();
            
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
            if (!language.equals("")) b.setField("language",language);
            if (!pst.equals("")) b.setField("medline-pst", pst);
			if (!abstractText.equals("")) b.setField("abstract",abstractText.replaceAll("%","\\\\%"));
			if (!keywords.equals("")) b.setField("keywords",keywords);
			if (!month.equals("")) b.setField("month",month);
			//if (!url.equals("")) b.setField("url",url);
			if (!number.equals("")) b.setField("number",number);

			if(!doi.equals("")){
			    b.setField("doi",doi);
			    b.setField("url","http://dx.doi.org/"+doi);
			}
			if(!pii.equals(""))
			    b.setField("pii",pii);
            if(!affiliation.equals("")) {
                b.setField("institution",affiliation.replaceAll("#", "\\\\#"));
            }

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
			keywords ="";
            doi=""; pii="";
			year="";
			forename="";
			lastName="";
			abstractText="";
            affiliation="";
            pubmedid="";
            majorTopic = "";
            minorTopics = "";
            month="";volume="";language="";pst="";lastname="";initials="";number="";page="";medlineID="";url="";
			MedlineDate="";
            descriptors.clear();
        }

		else if(localName.equals("ArticleTitle")){inTitle=false;}
		else if(localName.equals("PubDate")){inPubDate=false;}
		else if(localName.equals("Year")){inYear=false;}
		else if(localName.equals("PMID")){inPubMedID=false;}
		else if(localName.equals("MedlineDate")){inMedlineDate=false;}
		else if(localName.equals("MedlineTA")){inJournal=false;} //journal name
		else if(localName.equals("Month")){inMonth=false;}
		else if(localName.equals("Volume")){inVolume=false;}
        else if(localName.equals("Language")){inLanguage=false;}
        else if(localName.equals("PublicationStatus")){inPst=false;}
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
        else if(localName.equals("QualifierName")) inQualifierName=false;
        else if(localName.equals("MeshHeading")) {
            inMeshHeader = false;
            if (minorTopics.equals(""))
                descriptors.add(majorTopic);
            else
                descriptors.add(majorTopic+", "+minorTopics);
        }
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
        else if(localName.equals("Affiliation")){ inAffiliation=false; }
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
        else if(inLanguage){language += new String(data,start,length).toLowerCase();}
        else if(inPst){pst += new String(data,start,length);}
		else if(inLastName){lastname += new String(data,start,length);}
		else if(inInitials){initials += new String(data,start,length);}
		else if(inIssue){number += new String(data,start,length);}
		else if(inMedlinePgn){ page += new String(data,start,length);}
		else if(inMedlineID){medlineID += new String(data,start,length);}
		else if(inURL){url += new String(data,start,length);}
		else if(inPubMedID){pubmedid = new String(data,start,length);}
        else if(inQualifierName) {
            if (!minorTopics.equals(""))
                minorTopics = minorTopics+"/";
            minorTopics = minorTopics + new String(data,start,length);
        }
        else if(inDescriptorName) {
            majorTopic = new String(data,start,length);
        }

            //keywords += new String(data,start,length) + ", ";
		else if(inForename){
			forename += new String(data,start,length);
			//System.out.println("IN FORENAME: " + forename);
		}
		else if(inAbstractText){ abstractText += new String(data,start,length);}
		else if(inMedlineDate){ MedlineDate += new String(data,start,length);}
		else if(inDoi){ doi=new String(data,start,length);}
		else if(inPii){ pii=new String(data,start,length);}
        else if(inAffiliation){ affiliation = new String(data,start,length);}
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
