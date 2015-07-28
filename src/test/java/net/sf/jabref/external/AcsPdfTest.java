package net.sf.jabref.external;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

public class AcsPdfTest {

    @Test
    public void testSupportsSite() throws Exception {
        FullTextFinder acs = new ACSPdfDownload();
        Assert.assertTrue(acs.supportsSite(new URL("http://pubs.acs.org/doi/abs/10.1021/bk-2006-STYG.ch014")));
        Assert.assertFalse(acs.supportsSite(new URL("http://pubs.rsc.org/en/Content/ArticleLanding/2014/SC/c4sc00823e")));
    }

    @Test
    public void testFindFullTextURL() throws Exception {
        FullTextFinder acs = new ACSPdfDownload();
        Assert.assertEquals(new URL("http://pubs.acs.org/doi/pdf/10.1021/bk-2006-STYG.ch014"),
                acs.findFullTextURL(new URL("http://pubs.acs.org/doi/abs/10.1021/bk-2006-STYG.ch014")));
    }
}
