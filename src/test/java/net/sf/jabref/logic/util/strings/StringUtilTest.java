package net.sf.jabref.logic.util.strings;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {
    @Test
    public void testUnifyLineBreaks() throws Exception {
        // Mac < v9
        Assert.assertEquals("\n", StringUtil.unifyLineBreaks("\r"));
        // Windows
        Assert.assertEquals("\n", StringUtil.unifyLineBreaks("\r\n"));
        // Unix
        Assert.assertEquals("\n", StringUtil.unifyLineBreaks("\n"));
    }
}