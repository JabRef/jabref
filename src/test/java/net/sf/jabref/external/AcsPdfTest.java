package net.sf.jabref.external;

import java.net.URL;

import junit.framework.TestCase;

public class AcsPdfTest extends TestCase {

    public void testSupportsSite() throws Exception {
    	FullTextFinder acs = new ACSPdfDownload();
    	assertTrue(acs.supportsSite(new URL("http://pubs.acs.org/doi/abs/10.1021/bk-2006-STYG.ch014")));
    	assertFalse(acs.supportsSite(new URL("http://pubs.rsc.org/en/Content/ArticleLanding/2014/SC/c4sc00823e")));
    }

    public void testFindFullTextURL() throws Exception {
    	FullTextFinder acs = new ACSPdfDownload();
    	assertEquals(new URL("http://pubs.acs.org/doi/pdf/10.1021/bk-2006-STYG.ch014"), 
    			acs.findFullTextURL(new URL("http://pubs.acs.org/doi/abs/10.1021/bk-2006-STYG.ch014")));
    }
}
