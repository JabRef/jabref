package net.sf.jabref.logic.net;

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
    public void isURLshouldAcceptValidURL() {
        Assert.assertTrue(URLUtil.isURL("http://www.google.com"));
        Assert.assertTrue(URLUtil.isURL("https://www.google.com"));
    }

    @Test
    public void isURLshouldRejectInvalidURL() {
        Assert.assertFalse(URLUtil.isURL("www.google.com"));
        Assert.assertFalse(URLUtil.isURL("google.com"));
    }
}
