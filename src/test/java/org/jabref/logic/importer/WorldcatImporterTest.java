package org.jabref.logic.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jabref.logic.importer.fileformat.WorldcatImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WorldcatImporterTest {
	private final String XML_WITH_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:diag=\"http://www.loc.gov/zing/srw/diagnostic/\" xmlns:oclcterms=\"http://purl.org/oclc/terms/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\"><title>OCLC Worldcat Search: srw.ti all \"The very best of Glenn\"</title><id>http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=1&amp;count=10&amp;format=atom&amp;wskey={built-in-api-key}</id><updated>2020-02-25T06:16:25-05:00</updated><subtitle>Search results for srw.ti all \"The very best of Glenn\" at http://worldcat.org/webservices/catalog</subtitle><opensearch:totalResults>89</opensearch:totalResults><opensearch:startIndex>1</opensearch:startIndex><opensearch:itemsPerPage>10</opensearch:itemsPerPage><opensearch:Query role=\"request\" searchTerms=\"srw.ti all &quot;The very best of Glenn&quot;\" startPage=\"1\"/><link rel=\"alternate\" href=\"http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=1&amp;count=10&amp;wskey={built-in-api-key}\" type=\"text/html\"/><link rel=\"self\" href=\"http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=1&amp;count=10&amp;format=atom&amp;wskey={built-in-api-key}\" type=\"application/atom+xml\"/><link rel=\"first\" href=\"http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=1&amp;count=10&amp;format=atom&amp;wskey={built-in-api-key}\" type=\"application/atom+xml\"/><link rel=\"next\" href=\"http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=11&amp;count=10&amp;format=atom&amp;wskey={built-in-api-key}\" type=\"application/atom+xml\"/><link rel=\"last\" href=\"http://worldcat.org/webservices/catalog/search/worldcat/opensearch?q=srw.ti+all+%22The+very+best+of+Glenn%22&amp;start=89&amp;count=10&amp;format=atom&amp;wskey={built-in-api-key}\" type=\"application/atom+xml\"/><link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"http://worldcat.org/webservices/catalog/opensearch.description.xml\"/><entry><author><name>Miller, Glenn.</name></author><title>The very best of Glenn Miller</title><link href=\"http://worldcat.org/oclc/754508587\"/><id>http://worldcat.org/oclc/754508587</id><updated>2017-06-29T19:46:28Z</updated><summary>Remastered versions of Glenn Miller's original recordings which bring a freshness to his beloved classics. The album also features a version of 'In the mood' with Jodie Prenger, winner of the BBC's 'I'll do anything'.</summary><oclcterms:recordIdentifier>754508587</oclcterms:recordIdentifier></entry><entry><author><name>Brennan, Walter, 1894-1974.</name></author><title>The very best of Walter Brennan.</title><link href=\"http://worldcat.org/oclc/17009787\"/><id>http://worldcat.org/oclc/17009787</id><updated>2018-07-02T10:57:48Z</updated><dc:identifier>urn:LCCN:95789751</dc:identifier><oclcterms:recordIdentifier>17009787</oclcterms:recordIdentifier></entry><entry><author><name>Frey, Glenn, composer, performer.</name></author><title>Above the clouds : the very best of Glenn Frey</title><link href=\"http://worldcat.org/oclc/1040990604\"/><id>http://worldcat.org/oclc/1040990604</id><updated>2019-05-06T02:09:58Z</updated><oclcterms:recordIdentifier>1040990604</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn, 1954- performer.</name></author><title>The very best of Glenn Miller.</title><link href=\"http://worldcat.org/oclc/1140416403\"/><id>http://worldcat.org/oclc/1140416403</id><updated>2020-02-13T17:01:04Z</updated><oclcterms:recordIdentifier>1140416403</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn.</name></author><title>Very Best of Glenn Miller.</title><link href=\"http://worldcat.org/oclc/630547816\"/><id>http://worldcat.org/oclc/630547816</id><updated>2018-11-28T18:44:17Z</updated><oclcterms:recordIdentifier>630547816</oclcterms:recordIdentifier></entry><entry><author><name>Yarbrough, Glenn.</name></author><title>Glenn yarbrough - his very best</title><link href=\"http://worldcat.org/oclc/1098394025\"/><id>http://worldcat.org/oclc/1098394025</id><updated>2019-04-25T11:56:52Z</updated><oclcterms:recordIdentifier>1098394025</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn.</name></author><title>The very best of Glenn Miller : hits &amp; rarities</title><link href=\"http://worldcat.org/oclc/809462816\"/><id>http://worldcat.org/oclc/809462816</id><updated>2020-01-23T10:02:43Z</updated><oclcterms:recordIdentifier>809462816</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn, 1904-1944, performer.</name></author><title>The very best of Glenn Miller &amp; his orchestra.</title><link href=\"http://worldcat.org/oclc/1123215070\"/><id>http://worldcat.org/oclc/1123215070</id><updated>2019-11-07T01:19:22Z</updated><oclcterms:recordIdentifier>1123215070</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn, 1904-1944.</name></author><title>The very best of Glenn Miller : in the mood.</title><link href=\"http://worldcat.org/oclc/55688833\"/><id>http://worldcat.org/oclc/55688833</id><updated>2016-11-17T00:02:35Z</updated><oclcterms:recordIdentifier>55688833</oclcterms:recordIdentifier></entry><entry><author><name>Miller, Glenn.</name></author><title>Glenn Miller story : the very best of</title><link href=\"http://worldcat.org/oclc/421718747\"/><id>http://worldcat.org/oclc/421718747</id><updated>2018-10-08T09:50:51Z</updated><oclcterms:recordIdentifier>421718747</oclcterms:recordIdentifier></entry></feed>";
	private final String XML_WITHOUT_RESULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:diag=\"http://www.loc.gov/zing/srw/diagnostic/\" xmlns:oclcterms=\"http://purl.org/oclc/terms/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opensearch=\"http://a9.com/-/spec/opensearch/1.1/\"/>";
	
	WorldcatImporter importer;

	@BeforeEach
	public void setUp(){
		importer = new WorldcatImporter();
	}
	
	@Test
	public void withResultIsRecognizedFormat() throws IOException{
		boolean isReq = importer.isRecognizedFormat(XML_WITH_RESULT);
		assertTrue(isReq);
	}

	@Test
	public void withoutResultIsRecognizedFormat() throws IOException{
		boolean isReq = importer.isRecognizedFormat(XML_WITHOUT_RESULT);
		assertTrue(isReq);
	}

	@Test
	public void badXMLIsNotRecognizedFormat() throws IOException{
		boolean isReq = importer.isRecognizedFormat("Nah bruh");
		assertFalse(isReq);
	}

	@Disabled("Will not work without API key")
	@Test
	public void withResultReturnsNonEmptyResult() throws IOException{
		ParserResult res = importer.importDatabase(XML_WITH_RESULT);
		assertTrue(res.getDatabase().getEntries().size() > 0);
	}

	@Test
	public void withoutResultReturnsEmptyResult() throws IOException{
		ParserResult res = importer.importDatabase(XML_WITHOUT_RESULT);
		assertEquals(0, res.getDatabase().getEntries().size());
	}

}