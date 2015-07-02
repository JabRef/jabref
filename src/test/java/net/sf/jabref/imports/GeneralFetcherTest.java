package net.sf.jabref.imports;

import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefMain;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.TestUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        JTextField tf = (JTextField) TestUtils.getChildNamed(GeneralFetcherTest.gf, "tf");
        Assert.assertNotNull(tf); // tf found?
        tf.setText(testString);
        tf.postActionEvent(); // send message
        Assert.assertEquals(testString, tf.getText());
        JButton reset = (JButton) TestUtils.getChildNamed(GeneralFetcherTest.gf, "reset");
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
        GeneralFetcherTest.gf = new GeneralFetcher(GeneralFetcherTest.spm, GeneralFetcherTest.jrf, al);
    }

    @After
    public void tearDown() {
        GeneralFetcherTest.gf = null;
        GeneralFetcherTest.acmpf = null;
        GeneralFetcherTest.spm = null;
        GeneralFetcherTest.jrf = null;
    }

}
