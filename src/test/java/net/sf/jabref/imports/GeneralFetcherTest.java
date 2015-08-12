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
package net.sf.jabref.imports;

import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefMain;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.testutils.GuiTestUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;

import java.util.ArrayList;

/**
 * Tests GeneralFetcher
 *
 * @author Dennis Hartrampf, Ines Moosdorf
 */
public class GeneralFetcherTest {

    static JabRefFrame jrf;
    static SidePaneManager spm;
    static GeneralFetcher gf;
    static ACMPortalFetcher acmpf;


    /**
     * Tests the reset-button. Types a text into tf, pushs reset and check tf's
     * text
     *
     * @throws Exception
     */
    @Test @Ignore
    public void testResetButton() throws Exception {
        String testString = "test string";
        JTextField tf = (JTextField) GuiTestUtils.getChildNamed(GeneralFetcherTest.gf, "tf");
        Assert.assertNotNull(tf); // tf found?
        tf.setText(testString);
        tf.postActionEvent(); // send message
        Assert.assertEquals(testString, tf.getText());
        JButton reset = (JButton) GuiTestUtils.getChildNamed(GeneralFetcherTest.gf, "reset");
        Assert.assertNotNull(reset); // reset found?
        reset.doClick(); // "click" reset
        Assert.assertEquals("", tf.getText());
    }

    /**
     * Get an instance of JabRef via its singleton and get a GeneralFetcher and an ACMPortalFetcher
     */
    @Before
    public void setUp() {
        JabRefMain.main(new String[0]);
        GeneralFetcherTest.jrf = JabRef.jrf;
        GeneralFetcherTest.spm = GeneralFetcherTest.jrf.sidePaneManager;
        GeneralFetcherTest.acmpf = new ACMPortalFetcher();
        ArrayList<EntryFetcher> al = new ArrayList<EntryFetcher>();
        al.add(GeneralFetcherTest.acmpf);
        GeneralFetcherTest.gf = new GeneralFetcher(GeneralFetcherTest.spm, GeneralFetcherTest.jrf);
    }

    @After
    public void tearDown() {
        GeneralFetcherTest.gf = null;
        GeneralFetcherTest.acmpf = null;
        GeneralFetcherTest.spm = null;
        GeneralFetcherTest.jrf = null;
    }

}
