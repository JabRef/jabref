package net.sf.jabref.util;

import org.junit.Assert;
import org.junit.Test;

public class GoogleUrlCleanerTest {

    @Test
    public void testCleanUrl() throws Exception {
        Assert.assertEquals("http://dl.acm.org/citation.cfm?id=321811", GoogleUrlCleaner.cleanUrl("https://www.google.hr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&ved=0CC0QFjAA&url=http%3A%2F%2Fdl.acm.org%2Fcitation.cfm%3Fid%3D321811&ei=gHDRUa-IKobotQbMy4GAAg&usg=AFQjCNEBJPUimu-bAns6lSLe-kszz4AiGA&sig2=DotF0pIZD8OhjDcSHPlBbQ"));
        Assert.assertEquals("http://dl.acm.org/citation.cfm?id=321811", GoogleUrlCleaner.cleanUrl("http://dl.acm.org/citation.cfm?id=321811"));
        Assert.assertEquals("test text", GoogleUrlCleaner.cleanUrl("test text"));
        Assert.assertEquals(" ", GoogleUrlCleaner.cleanUrl(" "));
        Assert.assertEquals("", GoogleUrlCleaner.cleanUrl(""));
        Assert.assertEquals(null, GoogleUrlCleaner.cleanUrl(null));
    }
}