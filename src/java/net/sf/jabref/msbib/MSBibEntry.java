/*
 * Created on April 01, 2007
 * Updated on May 03, 2007
 * */
package net.sf.jabref.msbib;
import net.sf.jabref.*;
import net.sf.jabref.export.layout.format.*;
import net.sf.jabref.export.layout.*;
import net.sf.jabref.mods.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.util.regex.*;

/**
 * @author S M Mahbub Murshed
 * @email udvranto@yahoo.com
 *
 * @version 1.0.0
 * @see http://mahbub.wordpress.com/2007/03/24/details-of-microsoft-office-2007-bibliographic-format-compared-to-bibtex/
 * @see http://mahbub.wordpress.com/2007/03/22/deciphering-microsoft-office-2007-bibliography-format/
 * 
 * Date: May 03, 2007
 * 
 */
public class MSBibEntry {
	protected String sourceType = "Misc";
	protected String bibTexEntry = null;

	protected String tag = null;
	protected String GUID = null;
	protected int LCID;
	
	protected List authors = null;
	protected List bookAuthors = null;
	protected List editors = null;
	protected List translators = null;
	protected List producerNames = null;
	protected List composers = null;
	protected List conductors = null;
	protected List performers = null;
	protected List writers = null;
	protected List directors = null;
	protected List compilers = null;
	protected List interviewers = null;
	protected List interviewees = null;
	protected List inventors = null;
	protected List counsels = null;

	protected String title = null;
	protected String year = null;
	protected String month = null;
	protected String day = null;
	
	protected String shortTitle = null;
	protected String comments = null;
	
	protected PageNumbers pages = null;
	protected String volume = null;
	protected int numberOfVolumes;
	protected String edition = null;
	protected String standardNumber = null;	
	protected String publisher = null;
	
	protected String address = null;
	protected String bookTitle = null;
	protected int chapterNumber;
	protected String journalName = null;
	protected int issue;
	protected String periodicalTitle = null;
	protected String conferenceName = null;
	protected String department = null;
	protected String institution = null;
	protected String thesisType = null;
	protected String internetSiteTitle = null;
	protected String dateAccessed = null;
	protected String url = null;
	protected String productionCompany = null;
	protected String publicationTitle = null;
	protected String medium = null;
	protected String albumTitle = null;
	protected String recordingNumber = null;
	protected String theater = null;
	protected String distributor = null;
	protected String broadcastTitle = null;
	protected String broadcaster = null;
	protected String station = null;
	protected String type = null;
	protected String patentNumber = null;
	protected String court = null;
	protected String reporter = null;
	protected String caseNumber = null;
	protected String abbreviatedCaseNumber = null;
	protected String bibTex_Series = null;
	protected String bibTex_Abstract = null; 	 
	protected String bibTex_KeyWords = null; 	 
	protected String bibTex_CrossRef = null;
	protected String bibTex_HowPublished = null; 	 
	protected String bibTex_Affiliation = null;
	protected String bibTex_Contents = null;
	protected String bibTex_Copyright = null;	 
	protected String bibTex_Price = null; 	 
	protected String bibTex_Size = null;

	public final String BIBTEX = "BIBTEX_";

	private final String bcol = "b:";
	
	private final boolean FORMATXML = false;
	
	public MSBibEntry() {
	}
	
	public MSBibEntry(BibtexEntry bibtex) {
		this();
		populateFromBibtex(bibtex);
	}
	
	protected void populateFromBibtex(BibtexEntry bibtex) {
		// date = getDate(bibtex);	
		sourceType = getMSBibSourceType(bibtex);

		if (bibtex.getField("bibtexkey") != null)
			tag = bibtex.getField("bibtexkey").toString();

		if (bibtex.getField("language") != null)
			LCID = getLCID(bibtex.getField("language").toString());

		if (bibtex.getField("title") != null)
			title = bibtex.getField("title").toString();
		if (bibtex.getField("year") != null)
			year = bibtex.getField("year").toString();
		if (bibtex.getField("month") != null)
			month = bibtex.getField("month").toString();
		if (bibtex.getField("msbib-day") != null)
			day = bibtex.getField("msbib-day").toString();

		if (bibtex.getField("msbib-shorttitle") != null)
			shortTitle = bibtex.getField("msbib-shorttitle").toString();
		if (bibtex.getField("note") != null)
			comments = bibtex.getField("note").toString();

		if (bibtex.getField("pages") != null)
			pages = new PageNumbers(bibtex.getField("pages").toString());

		if (bibtex.getField("volume") != null)
			volume = bibtex.getField("volume").toString();

		if (bibtex.getField("msbib-numberofvolume") != null)
			numberOfVolumes = Integer.parseInt(bibtex.getField("msbib-numberofvolume").toString());

		if (bibtex.getField("edition") != null)
			edition = bibtex.getField("edition").toString();
		
		standardNumber = new String();
		if (bibtex.getField("ISBN") != null)
			standardNumber += ":ISBN:" + bibtex.getField("ISBN").toString();
		if (bibtex.getField("ISSN") != null)
			standardNumber += ":ISSN:"+ bibtex.getField("ISSN").toString();
		if (bibtex.getField("LCCN") != null)
			standardNumber += ":LCCN:"+ bibtex.getField("LCCN").toString();
		if (bibtex.getField("mrnumber") != null)
			standardNumber += ":MRN:"+ bibtex.getField("mrnumber").toString();

		if (bibtex.getField("publisher") != null)
			publisher = bibtex.getField("publisher").toString();

		if (bibtex.getField("address") != null)
			address = bibtex.getField("address").toString();

		if (bibtex.getField("booktitle") != null)
			bookTitle = bibtex.getField("booktitle").toString();

		if (bibtex.getField("chapter") != null)
			chapterNumber = Integer.parseInt(bibtex.getField("chapter").toString());

		if (bibtex.getField("journal") != null)
			journalName = bibtex.getField("journal").toString();

		if (bibtex.getField("issue") != null)
			issue = Integer.parseInt(bibtex.getField("issue").toString());

		if (bibtex.getField("msbib-periodical") != null)
			periodicalTitle = bibtex.getField("msbib-periodical").toString();
		
		if (bibtex.getField("organization") != null)
			conferenceName = bibtex.getField("organization").toString();
		if (bibtex.getField("school") != null)
			department = bibtex.getField("school").toString();
		if (bibtex.getField("institution") != null)
			institution = bibtex.getField("institution").toString();

		if (bibtex.getField("type") != null)
			thesisType = bibtex.getField("type").toString();
		if ( (sourceType.equals("InternetSite")==true || sourceType.equals("DocumentFromInternetSite")==true)
				&& bibtex.getField("title") != null)
			internetSiteTitle = bibtex.getField("title").toString();
		if (bibtex.getField("msbib-accessed") != null)
			dateAccessed = bibtex.getField("msbib-accessed").toString();
		if (bibtex.getField("URL") != null)
			url = bibtex.getField("URL").toString();
		if (bibtex.getField("msbib-productioncompany") != null)
			productionCompany = bibtex.getField("msbib-productioncompany").toString();
		
		if ( (sourceType.equals("ElectronicSource")==true 
				|| sourceType.equals("Art")==true
				|| sourceType.equals("Misc")==true)
				&& bibtex.getField("title") != null)
			publicationTitle = bibtex.getField("title").toString();
		if (bibtex.getField("msbib-medium") != null)
			medium = bibtex.getField("msbib-medium").toString();
		if (sourceType.equals("SoundRecording")==true && bibtex.getField("title") != null)
			albumTitle = bibtex.getField("title").toString();
		if (bibtex.getField("msbib-recordingnumber") != null)
			recordingNumber = bibtex.getField("msbib-recordingnumber").toString();
		if (bibtex.getField("msbib-theater") != null)
			theater = bibtex.getField("msbib-theater").toString();
		if (bibtex.getField("msbib-distributor") != null)
			distributor = bibtex.getField("msbib-distributor").toString();
		if (sourceType.equals("Interview")==true && bibtex.getField("title") != null)
			broadcastTitle = bibtex.getField("title").toString();
		if (bibtex.getField("msbib-broadcaster") != null)
			broadcaster = bibtex.getField("msbib-broadcaster").toString();
		if (bibtex.getField("msbib-station") != null)
			station = bibtex.getField("msbib-station").toString();
		if (bibtex.getField("msbib-type") != null)
			type = bibtex.getField("msbib-type").toString();
		if (bibtex.getField("msbib-patentnumber") != null)
			patentNumber = bibtex.getField("msbib-patentnumber").toString();
		if (bibtex.getField("msbib-court") != null)
			court = bibtex.getField("msbib-court").toString();
		if (bibtex.getField("msbib-reporter") != null)
			reporter = bibtex.getField("msbib-reporter").toString();
		if (bibtex.getField("msbib-casenumber") != null)
			caseNumber = bibtex.getField("msbib-casenumber").toString();
		if (bibtex.getField("msbib-abbreviatedcasenumber") != null)
			abbreviatedCaseNumber = bibtex.getField("msbib-abbreviatedcasenumber").toString();
		if (bibtex.getField("series") != null)
			bibTex_Series = bibtex.getField("series").toString();
		if (bibtex.getField("abstract") != null)
			bibTex_Abstract = bibtex.getField("abstract").toString();
		if (bibtex.getField("keywords") != null)
			bibTex_KeyWords = bibtex.getField("keywords").toString();
		if (bibtex.getField("crossref") != null)
			bibTex_CrossRef = bibtex.getField("crossref").toString();
		if (bibtex.getField("howpublished") != null)
			bibTex_HowPublished = bibtex.getField("howpublished").toString();
		if (bibtex.getField("affiliation") != null)
			bibTex_Affiliation = bibtex.getField("affiliation").toString();
		if (bibtex.getField("contents") != null)
			bibTex_Contents = bibtex.getField("contents").toString();
		if (bibtex.getField("copyright") != null)
			bibTex_Copyright = bibtex.getField("copyright").toString();
		if (bibtex.getField("price") != null)
			bibTex_Price = bibtex.getField("price").toString();
		if (bibtex.getField("size") != null)
			bibTex_Size = bibtex.getField("size").toString();
	 

		if (bibtex.getField("author") != null)
			authors = getAuthors(bibtex.getField("author").toString());
		
		
		if(FORMATXML)
		{
			title = format(title);
			shortTitle = format(shortTitle);
			publisher = format(publisher);
			conferenceName = format(conferenceName);
			department = format(department);
			institution = format(institution);
			internetSiteTitle = format(internetSiteTitle);
			publicationTitle = format(publicationTitle);
			albumTitle = format(albumTitle);
			theater = format(theater);
			distributor = format(distributor);
			broadcastTitle = format(broadcastTitle);
			broadcaster = format(broadcaster);
			station = format(station);
			court = format(court);
			reporter = format(reporter);
			bibTex_Series = format(bibTex_Series);
		}
	}

	private String format(String value)
	{
		if(value == null)
			return null;
		String result = null;
		LayoutFormatter chars = new XMLChars();
		result = chars.format(value);
		return result;
	}
	
	// http://www.microsoft.com/globaldev/reference/lcid-all.mspx
	protected int getLCID(String language)
	{
		int iLCID = 0;
		// TODO: add lanaguage to LCID mapping
		
		return iLCID;
	}
	
	
	protected List getAuthors(String authors) {
		List result = new LinkedList();
		LayoutFormatter chars = new XMLChars();
		
		if (authors.indexOf(" and ") == -1)
		{
			if(FORMATXML)
				result.add(new PersonName(chars.format(authors)));
			else
				result.add(new PersonName(authors));
		}
        else
        {
            String[] names = authors.split(" and ");
            for (int i=0; i<names.length; i++)
            {
            	if(FORMATXML)
            		result.add(new PersonName(chars.format(names[i])));
            	else
            		result.add(new PersonName(names[i]));
            }
        }
		return result;
	}
	
	/* construct a MSBib date object */
	protected String getDate(BibtexEntry bibtex) {
		String result = "";
		if (bibtex.getField("year") != null)
			result += (bibtex.getField("year").toString());
		if (bibtex.getField("month") != null)
			result += "-" + bibtex.getField("month").toString();
		
		return result;
	}

	protected String getMSBibSourceType(BibtexEntry bibtex) {
		String bibtexType = bibtex.getType().getName();

		String result = "Misc";
		if (bibtexType.equalsIgnoreCase("book"))
			result = "Book";
		else if(bibtexType.equalsIgnoreCase("inbook"))
			result = "BookSection";
		else if(bibtexType.equalsIgnoreCase("booklet"))
			{ result = "BookSection"; bibTexEntry = "booklet"; } 
		else if(bibtexType.equalsIgnoreCase("incollection"))
			{ result = "BookSection"; bibTexEntry = "incollection"; } 

		else if(bibtexType.equalsIgnoreCase("article"))
			result = "JournalArticle"; 

		else if(bibtexType.equalsIgnoreCase("inproceedings"))
			result = "ConferenceProceedings"; 
		else if(bibtexType.equalsIgnoreCase("conference"))
			{ result = "ConferenceProceedings"; bibTexEntry = "conference"; } 
		else if(bibtexType.equalsIgnoreCase("proceedings"))
			{ result = "ConferenceProceedings"; bibTexEntry = "proceedings"; } 
		else if(bibtexType.equalsIgnoreCase("collection"))
			{ result = "ConferenceProceedings"; bibTexEntry = "collection"; } 

		else if(bibtexType.equalsIgnoreCase("techreport"))
			result = "Report"; 
		else if(bibtexType.equalsIgnoreCase("manual"))
			{ result = "Report"; bibTexEntry = "manual"; } 
		else if(bibtexType.equalsIgnoreCase("mastersthesis"))
			{ result = "Report"; bibTexEntry = "mastersthesis"; } 
		else if(bibtexType.equalsIgnoreCase("phdthesis"))
			{ result = "Report"; bibTexEntry = "phdthesis"; } 
		else if(bibtexType.equalsIgnoreCase("unpublished"))
			{ result = "Report"; bibTexEntry = "unpublished"; } 

		else if(bibtexType.equalsIgnoreCase("patent"))
			result = "Patent"; 

		else if(bibtexType.equalsIgnoreCase("misc"))
			result = "Misc"; 
		
		return result;
	}
	
	public Document getDOMrepresentation() {
		Document result = null;
		try {
			DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
		//	result = getDOMrepresentation(d);
		}
		catch (Exception e) 
		{
			throw new Error(e);
		}
		return result;
	}
	
//	private String healXML(String value)
//	{
//		String healedValue = value;
//
//		// Remove special dash that looks like same but is not		
//		healedValue = healedValue.replace('–','-');
//		healedValue = healedValue.replace('—','-');
//		
//		// String literals
//		healedValue = healedValue.replaceAll("‘","`");
//		healedValue = healedValue.replaceAll("’","'");
//		healedValue = healedValue.replaceAll("“","\"");
//		healedValue = healedValue.replaceAll("”","\"");
//		
//		// HTML subscript value
//		healedValue = healedValue.replaceAll("<sub>","_");
//		healedValue = healedValue.replaceAll("</sub>","");
//
////		restore converted to html-char
////		Pattern p = Pattern.compile("&#(\\d{1,4});");
////		Matcher m = p.matcher(healedValue);
////		while (m.find())
////		{
////			System.out.println(m.group(1));
////			int n = Integer.parseInt(m.group(1));
////			char ch = Character.forDigit(n,10);
////			healedValue = healedValue.replaceAll("&#"+m.group(1)+";",""+ch);
////		}
//		
//		return healedValue;
//	}

	public void addField(Document d,Element parent, String name, String value) {
		if(value == null)
			return;
		Element elem = d.createElement(bcol+name);
 		// elem.appendChild(d.createTextNode(healXML(value)));
		elem.appendChild(d.createTextNode(value));
   		parent.appendChild(elem);
	}

	public void addAuthor(Document d, Element allAuthors, String entryName, List authorsLst) {
		if(authorsLst == null)
			return;
		Element authorTop = d.createElement(bcol+entryName);
		Element nameList = d.createElement(bcol+"NameList");
		for(Iterator iter = authorsLst.iterator(); iter.hasNext();) {
			PersonName name = (PersonName) iter.next();
			Element person = d.createElement(bcol+"Person");
			addField(d, person,"Last",name.getSurname());
			addField(d, person,"Middle",name.getMiddlename());
			addField(d, person,"First",name.getFirstname());
			nameList.appendChild(person);
		}
		authorTop.appendChild(nameList);
		
		allAuthors.appendChild(authorTop);
	}

	public void addAdrress(Document d,Element parent, String address) {
		if(address == null)
			return;

		// US address parser
		// See documentation here http://regexlib.com/REDetails.aspx?regexp_id=472
		// Pattern p = Pattern.compile("^(?n:(((?<address1>(\\d{1,5}(\\ 1\\/[234])?(\\x20[A-Z]([a-z])+)+ )|(P\\.O\\.\\ Box\\ \\d{1,5}))\\s{1,2}(?i:(?<address2>(((APT|B LDG|DEPT|FL|HNGR|LOT|PIER|RM|S(LIP|PC|T(E|OP))|TRLR|UNIT)\\x20\\w{1,5})|(BSMT|FRNT|LBBY|LOWR|OFC|PH|REAR|SIDE|UPPR)\\.?)\\s{1,2})?))?)(?<city>[A-Z]([a-z])+(\\.?)(\\x20[A-Z]([a-z])+){0,2})([,\\x20]+?)(?<state>A[LKSZRAP]|C[AOT]|D[EC]|F[LM]|G[AU]|HI|I[ADL N]|K[SY]|LA|M[ADEHINOPST]|N[CDEHJMVY]|O[HKR]|P[ARW]|RI|S[CD] |T[NX]|UT|V[AIT]|W[AIVY])([,\\x20]+?)(?<zipcode>(?!0{5})\\d{5}(-\\d {4})?)((([,\\x20]+?)(?<country>[A-Z]([a-z])+(\\.?)(\\x20[A-Z]([a-z])+){0,2}))?))$");
		// the pattern above is for C#, may not work with java. Never tested though.
		
		// reduced subset, supports only "CITY , STATE, COUNTRY"
		// \b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w+)\b
		// WORD SPACE , SPACE WORD SPACE , SPACE WORD
		// tested using http://www.javaregex.com/test.html
		Pattern p = Pattern.compile("\\b(\\w+)\\s*[,]?\\s*(\\w+)\\s*[,]?\\s*(\\w+)\\b");
		Matcher m = p.matcher(address);
		if (m.matches() && m.groupCount()>3)
		{
			addField(d, parent,"City",m.group(1));
			addField(d, parent,"StateProvince",m.group(2));
			addField(d, parent,"CountryRegion",m.group(3));
		}
	}

	public void addDate(Document d,Element parent, String date, String extra) {
		if(date == null)
			return;

		// Allows 20.3-2007|||20/3-  2007 etc. 
		// (\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})
		// 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
		// tested using http://www.javaregex.com/test.html
		Pattern p = Pattern.compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");
		Matcher m = p.matcher(date);
		if (m.matches() && m.groupCount()>3)
		{
			addField(d, parent,"Month"+extra,m.group(1));
			addField(d, parent,"Day"+extra,m.group(2));
			addField(d, parent,"Year"+extra,m.group(3));
		}
	}

	public Element getDOMrepresentation(Document d) {
		Node result = null;		
	   	try {
	   		Element msbibEntry = d.createElement(bcol+"Source");

	   		addField(d,msbibEntry,"SourceType",sourceType);
   			addField(d,msbibEntry,BIBTEX+"Entry",bibTexEntry);

   			addField(d,msbibEntry,"Tag",tag);
   			addField(d,msbibEntry,"GUID",GUID);
   			addField(d,msbibEntry,"LCID",Integer.toString(LCID));
   			addField(d,msbibEntry,"Title",title);
   			addField(d,msbibEntry,"Year",year);
   			addField(d,msbibEntry,"ShortTitle",shortTitle);
   			addField(d,msbibEntry,"Comments",comments);

   			Element allAuthors = d.createElement(bcol+"Author");

   			addAuthor(d,allAuthors,"Author",authors);
	   		addAuthor(d,allAuthors,"BookAuthor",bookAuthors);
	   		addAuthor(d,allAuthors,"Editor",editors);
	   		addAuthor(d,allAuthors,"Translator",translators);
	   		addAuthor(d,allAuthors,"ProducerName",producerNames);
	   		addAuthor(d,allAuthors,"Composer",composers);
	   		addAuthor(d,allAuthors,"Conductor",conductors);
	   		addAuthor(d,allAuthors,"Performer",performers);
	   		addAuthor(d,allAuthors,"Writer",writers);
	   		addAuthor(d,allAuthors,"Director",directors);
	   		addAuthor(d,allAuthors,"Compiler",compilers);
	   		addAuthor(d,allAuthors,"Interviewer",interviewers);
	   		addAuthor(d,allAuthors,"Interviewee",interviewees);
	   		addAuthor(d,allAuthors,"Inventor",inventors);
	   		addAuthor(d,allAuthors,"counsels",counsels);

	   		msbibEntry.appendChild(allAuthors);
	   		
	   		if(pages !=null )
	   			addField(d,msbibEntry,"Pages",pages.toString("-"));
	   		addField(d,msbibEntry,"Volume",volume);
	   		addField(d,msbibEntry,"NumberVolumes",Integer.toString(numberOfVolumes));
	   		addField(d,msbibEntry,"Edition",edition);
	   		addField(d,msbibEntry,"StandardNumber",standardNumber);
	   		addField(d,msbibEntry,"Publisher",publisher);
		
	   		addAdrress(d,msbibEntry,address);
	   		
	   		addField(d,msbibEntry,"BookTitle",bookTitle);
	   		addField(d,msbibEntry,"ChapterNumber",Integer.toString(chapterNumber));

	   		addField(d,msbibEntry,"JournalName",journalName);
	   		addField(d,msbibEntry,"Issue",Integer.toString(issue));
	   		addField(d,msbibEntry,"PeriodicalTitle",periodicalTitle);
	   		addField(d,msbibEntry,"ConferenceName",conferenceName);

	   		addField(d,msbibEntry,"Department",department);
	   		addField(d,msbibEntry,"Institution",institution);
	   		addField(d,msbibEntry,"ThesisType",thesisType);
	   		addField(d,msbibEntry,"InternetSiteTitle",internetSiteTitle);
	   		
	   		addDate(d,msbibEntry, dateAccessed, "Accessed");
	   		
	   		addField(d,msbibEntry,"URL",url);
	   		addField(d,msbibEntry,"ProductionCompany",productionCompany);
	   		addField(d,msbibEntry,"PublicationTitle",publicationTitle);
	   		addField(d,msbibEntry,"Medium",medium);
	   		addField(d,msbibEntry,"AlbumTitle",albumTitle);
	   		addField(d,msbibEntry,"RecordingNumber",recordingNumber);	   		
	   		addField(d,msbibEntry,"Theater",theater);
	   		addField(d,msbibEntry,"Distributor",distributor);
	   		addField(d,msbibEntry,"BroadcastTitle",broadcastTitle);
	   		addField(d,msbibEntry,"Broadcaster",broadcaster);
	   		addField(d,msbibEntry,"Station",station);
	   		addField(d,msbibEntry,"Type",type);
	   		addField(d,msbibEntry,"PatentNumber",patentNumber);
	   		addField(d,msbibEntry,"Court",court);
	   		addField(d,msbibEntry,"Reporter",reporter);
	   		addField(d,msbibEntry,"CaseNumber",caseNumber);
	   		addField(d,msbibEntry,"AbbreviatedCaseNumber",abbreviatedCaseNumber);

	   		addField(d,msbibEntry,BIBTEX+"Series",bibTex_Series);
	   		addField(d,msbibEntry,BIBTEX+"Abstract",bibTex_Abstract);
	   		addField(d,msbibEntry,BIBTEX+"KeyWords",bibTex_KeyWords);
	   		addField(d,msbibEntry,BIBTEX+"CrossRef",bibTex_CrossRef);
	   		addField(d,msbibEntry,BIBTEX+"HowPublished",bibTex_HowPublished);
	   		addField(d,msbibEntry,BIBTEX+"Affiliation",bibTex_Affiliation);
	   		addField(d,msbibEntry,BIBTEX+"Contents",bibTex_Contents);
	   		addField(d,msbibEntry,BIBTEX+"Copyright",bibTex_Copyright);
	   		addField(d,msbibEntry,BIBTEX+"Price",bibTex_Price);
	   		addField(d,msbibEntry,BIBTEX+"Size",bibTex_Size);

	   		return msbibEntry;
	   	}
	   	catch (Exception e)
		{
	   		System.out.println("Exception caught..." + e);
	   		e.printStackTrace();
	   		throw new Error(e);
		}
	   	// return null;
	   }
	
	/*
	 * render as XML
	 */
	public String toString() {
		StringWriter sresult = new StringWriter();
	   	try {
	      	 DOMSource source = new DOMSource(getDOMrepresentation());
	      	 StreamResult result = new StreamResult(sresult);
	      	 Transformer trans = TransformerFactory.newInstance().newTransformer();
	      	 trans.setOutputProperty(OutputKeys.INDENT, "yes");
	      	 trans.transform(source, result);
	      	}
	      	catch (Exception e) {
	      		throw new Error(e);
	      	}
	      return sresult.toString();
	}

}
