package net.sf.jabref.logic.util.io;

import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.io.URLUtil;
import org.junit.Assert;
import org.junit.Test;

public class URLUtilTest {
    @Test
    public void cleanGoogleSearchURL() throws Exception {
        // empty text
        Assert.assertEquals("", URLUtil.cleanGoogleSearchURL(""));
        Assert.assertEquals(" ", URLUtil.cleanGoogleSearchURL(" "));
        // no URL
        Assert.assertEquals("this is no url!", URLUtil.cleanGoogleSearchURL("this is no url!"));
        // no Google search URL
        Assert.assertEquals("http://dl.acm.org/citation.cfm?id=321811", URLUtil.cleanGoogleSearchURL("http://dl.acm.org/citation.cfm?id=321811"));
        // no queries
        Assert.assertEquals("https://www.google.de/url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url"));
        Assert.assertEquals("https://www.google.de/url?", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?"));
        // no multiple queries
        Assert.assertEquals("https://www.google.de/url?key=value", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key=value"));
        // no key values
        Assert.assertEquals("https://www.google.de/url?key", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key"));
        Assert.assertEquals("https://www.google.de/url?url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url"));
        Assert.assertEquals("https://www.google.de/url?key=", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key="));
        // no url param
        Assert.assertEquals("https://www.google.de/url?key=value&key2=value2", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?key=value&key2=value2"));
        // no url param value
        Assert.assertEquals("https://www.google.de/url?url=", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url="));
        // url param value no URL
        Assert.assertEquals("https://www.google.de/url?url=this+is+no+url", URLUtil.cleanGoogleSearchURL("https://www.google.de/url?url=this+is+no+url"));
        // Http
        Assert.assertEquals(
                "http://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("http://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=http%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
        // Https
        Assert.assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
        // root domain
        Assert.assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://google.de/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
        // foreign domain
        Assert.assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
        // foreign domain co.uk
        Assert.assertEquals(
                "https://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.co.uk/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=https%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
        // accept ftp results
        Assert.assertEquals(
                "ftp://moz.com/ugc/the-ultimate-guide-to-the-google-search-parameters",
                URLUtil.cleanGoogleSearchURL("https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&uact=8&ved=0CCEQFjAAahUKEwjJurHd2sfHAhWBsxQKHSrEAaM&url=ftp%3A%2F%2Fmoz.com%2Fugc%2Fthe-ultimate-guide-to-the-google-search-parameters&ei=0THeVYmOJIHnUqqIh5gK&usg=AFQjCNHnid_r_d2LP8_MqvI7lQnTC3lB_g&sig2=ICzxDroG2ENTJSUGmdhI2w")
        );
    }

    @Test
    public void testSanitizeUrl() {
        Assert.assertEquals("http://www.vg.no", URLUtil.sanitizeUrl("http://www.vg.no"));
        Assert.assertEquals("http://www.vg.no/fil%20e.html", URLUtil.sanitizeUrl("http://www.vg.no/fil e.html"));
        Assert.assertEquals("http://www.vg.no/fil%20e.html", URLUtil.sanitizeUrl("http://www.vg.no/fil%20e.html"));
        Assert.assertEquals("www.vg.no/fil%20e.html", URLUtil.sanitizeUrl("www.vg.no/fil%20e.html"));
        Assert.assertEquals("www.vg.no/fil%20e.html", URLUtil.sanitizeUrl("\\url{www.vg.no/fil%20e.html}"));
        Assert.assertEquals("ftp://www.vg.no", URLUtil.sanitizeUrl("ftp://www.vg.no"));
        Assert.assertEquals("file://doof.txt", URLUtil.sanitizeUrl("file://doof.txt"));
        Assert.assertEquals("file:///", URLUtil.sanitizeUrl("file:///"));
        Assert.assertEquals("/src/doof.txt", URLUtil.sanitizeUrl("/src/doof.txt"));
        Assert.assertEquals("/", URLUtil.sanitizeUrl("/"));
        Assert.assertEquals("/home/user/example.txt", URLUtil.sanitizeUrl("/home/user/example.txt"));
    }
}
