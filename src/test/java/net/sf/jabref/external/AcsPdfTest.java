/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.external;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

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
