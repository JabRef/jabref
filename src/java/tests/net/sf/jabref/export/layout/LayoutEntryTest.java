package tests.net.sf.jabref.export.layout;

import java.io.StringReader;
import java.util.ArrayList;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutEntry;
import net.sf.jabref.export.layout.LayoutHelper;
import junit.framework.TestCase;

/**
 * The test class LayoutEntryTest test the net.sf.jabref.export.layout.LayoutEntry.
 * Indirectly the net.sf.jabref.export.layout.Layout is testet too.
 * 
 * The LayoutEntry creates a human readable String assinged with html formaters.
 * To test the Highlighting Feature, an instance of LayoutEntry will be instatiated via Layout and LayoutHelper.
 * With these instance the doLayout() Method is called several times for each test case. 
 * To simulate a search, a BibtexEntry will be created, wich will be used by LayoutEntry.
 * The definiton of the search is set by
 * 
 * LayoutEntry.setWordsToHighlight(words); and 
 * Globals.prefs.putBoolean("caseSensitiveSearch", false);
 * 
 * There are five test cases:
 * - The shown result text has no words which should be highlighted.
 * - There is one word which will be highlighted ignoring case sensitivity.  
 * - There are two words which will be highlighted ignoring case sensitivity.
 * - There is one word which will be highlighted case sensitivity.
 * - There are more words which will be highlighted case sensitivity.
 * 
 * @author Arne
 *
 */

public class LayoutEntryTest extends TestCase
{
	private BibtexEntry mBTE;
	
	/**
	 * Initialize Preferences.
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		if (Globals.prefs == null)
		{
			Globals.prefs = JabRefPreferences.getInstance();
			Globals.prefs.putBoolean("highLightWords", Boolean.TRUE);
		}
		
		// create Bibtext Entry
		
		mBTE = new BibtexEntry("testid");
		mBTE.setField("abstract", "In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android google Open-Source Devices");
		//  Specifically, we present a core typed language to describe Android applications, and to reason about their data-flow security properties. Our operational semantics and type system provide some necessary foundations to help both users and developers of Android applications deal with their security concerns.
		mBTE.setField("keywords", "android, mobile devices, security");
		mBTE.setField("posted-at", "2010-08-11 15:00:49");
		mBTE.setField("location", "Dublin, Ireland");
		mBTE.setField("bibtexkey", "chaudhuri-plas09");
		mBTE.setField("pages", "1--7");
		mBTE.setField("booktitle", "PLAS '09: Proceedings of the ACM SIGPLAN Fourth Workshop on Programming Languages and Analysis for Security");
		mBTE.setField("citeulike-article-id", "7615801");
		mBTE.setField("citeulike-linkout-1", "http://dx.doi.org/10.1145/1554339.1554341");
		mBTE.setField("url", "http://dx.doi.org/10.1145/1554339.1554341");
		mBTE.setField("publisher", "ACM");
		mBTE.setField("timestamp", "2010.11.11");
		mBTE.setField("author", "Chaudhuri, Avik");
		mBTE.setField("title", "Language-based security on Android");
		mBTE.setField("address", "New York, NY, USA");
		mBTE.setField("priority", "2");
		mBTE.setField("isbn", "978-1-60558-645-8");
		mBTE.setField("owner", "Arne");
		mBTE.setField("year", "2009");
		mBTE.setField("citeulike-linkout-0", "http://portal.acm.org/citation.cfm?id=1554339.1554341");
		mBTE.setField("doi", "10.1145/1554339.1554341");
	}
	
	// helper Methods
	
	public String layout(String layoutFile, BibtexEntry entry, ArrayList<String> wordsToHighlight) throws Exception
	{
		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
		Layout layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
		StringBuffer sb = new StringBuffer();
		sb.append(layout.doLayout(entry, null, wordsToHighlight));

		return sb.toString();
	}
	
	/*************************/
	/****** tests Cases ******/
	/*************************/
	
	/**
	 * @throws Exception
	 */
	public void testNoHighlighting() throws Exception
	{
		// say that this bibtex object was found
		mBTE.setSearchHit(true);
		
		// define the highlighting settings
		Globals.prefs.putBoolean("caseSensitiveSearch", false);
		
		String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, new ArrayList<String>());
		String expecting = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android google Open-Source Devices</font>";
		
		assertEquals(expecting, result);
	}
	
	/**
	 * @throws Exception
	 */
	public void testHighlightingOneWordCaseInsesitive() throws Exception
	{
		// say that this bibtex object was found
		mBTE.setSearchHit(true);
		
		// define the serach words
		ArrayList<String> words = new ArrayList<String>();
		words.add("google");
		
		// define the highlighting settings
		Globals.prefs.putBoolean("caseSensitiveSearch", false);
		
		String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, words);
		String containing = "<span style=\"background-color:#FF4500;\">Google</span>"; 
		
		// check
		assertTrue(result.contains(containing));
	}
	
	/**
	 * @throws Exception
	 */
	public void testHighlightingTwoWordsCaseInsesitive() throws Exception
	{
		// say that this bibtex object was found
		mBTE.setSearchHit(true);
		
		// define the serach words
		ArrayList<String> words = new ArrayList<String>();
		words.add("Android");
		words.add("study");
		
		// define the highlighting settings
		Globals.prefs.putBoolean("caseSensitiveSearch", false);
		
		String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, words);
		
		String containing = "<span style=\"background-color:#FF4500;\">Android</span>"; 
		String containing2 = "<span style=\"background-color:#FF4500;\">study</span>";
		
		// check
		assertTrue(result.contains(containing));
		assertTrue(result.contains(containing2));
	}

	
	/**
	 * @throws Exception
	 */
	public void testHighlightingOneWordCaseSesitive() throws Exception
	{
		// say that this bibtex object was found
		mBTE.setSearchHit(true);
		
		// define the serach words
		ArrayList<String> words = new ArrayList<String>();
		words.add("google");
		
		// define the highlighting settings
		Globals.prefs.putBoolean("caseSensitiveSearch", true);
		
		String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, words);
		String expected = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android <span style=\"background-color:#FF4500;\">google</span> Open-Source Devices</font>";
		
		// check
		assertEquals(expected, result);
	}
	
	/**
	 * @throws Exception
	 */
	public void testHighlightingMoreWordsCaseSesitive() throws Exception
	{
		// say that this bibtex object was found
		mBTE.setSearchHit(true);
		
		// define the serach words
		ArrayList<String> words = new ArrayList<String>();
		words.add("Android");
		words.add("study");
		words.add("Open");
		
		// define the highlighting settings
		Globals.prefs.putBoolean("caseSensitiveSearch", false);
		
		String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, words);
		String expected = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal <span style=\"background-color:#FF4500;\">study</span> of security on <span style=\"background-color:#FF4500;\">Android</span>: Google's new <span style=\"background-color:#FF4500;\">open</span>-source platform for mobile devices. Tags: Paper <span style=\"background-color:#FF4500;\">android</span> google <span style=\"background-color:#FF4500;\">Open</span>-Source Devices</font>";
		
		// check
		assertEquals(expected, result);
	}
}
