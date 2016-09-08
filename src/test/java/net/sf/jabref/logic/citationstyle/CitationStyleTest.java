package net.sf.jabref.logic.citationstyle;

import org.junit.Assert;
import org.junit.Test;


public class CitationStyleTest {

    @Test
    public void getDefault() throws Exception {
        Assert.assertNotNull(CitationStyle.getDefault());
    }

}
