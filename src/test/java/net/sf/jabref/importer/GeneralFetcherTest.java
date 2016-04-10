package net.sf.jabref.importer;

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefMain;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.importer.fetcher.ACMPortalFetcher;
import net.sf.jabref.importer.fetcher.EntryFetcher;
import net.sf.jabref.importer.fetcher.GeneralFetcher;
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

    private static JabRefFrame jrf;
    private static SidePaneManager spm;
    private static GeneralFetcher gf;
    private static ACMPortalFetcher acmpf;


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
        GeneralFetcherTest.jrf = JabRef.mainFrame;
        GeneralFetcherTest.spm = GeneralFetcherTest.jrf.getSidePaneManager();
        GeneralFetcherTest.acmpf = new ACMPortalFetcher();
        ArrayList<EntryFetcher> al = new ArrayList<>();
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
