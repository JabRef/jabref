package net.sf.jabref.logic.net;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class CookieTest {

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectExpiresFormat() throws URISyntaxException {
        new Cookie(new URI("http://jabref.org/"), "name=TestCookie; expires=Tue, 25/07/10 16:43:15 GMT");
        fail();
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectDomain() throws URISyntaxException {
        new Cookie(new URI("http://jabref.org/"), "name=TestCookie; domain=google.com");
        fail();
    }

    @Test
    public void testHasExpiredFalse() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "name=TestCookie; expires=Tue, 25-Jul-45 16:43:15 GMT");
        assertFalse(cookie.hasExpired());
    }


    @Test
    public void testHasExpiredFalseWhenNotSet() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"), "name=TestCookie; domain=jabref.org; path=/");
        assertFalse(cookie.hasExpired());
    }

    @Test
    public void testHasExpiredTrueSpaceFormat() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "name=Nicholas; expires=Sat, 02 May 2009 23:38:25 GMT");
        assertTrue(cookie.hasExpired());
    }

    @Test
    public void testHasExpiredTrueHyphenFormat() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "name=Nicholas; expires=Sat, 02-May-2009 23:38:25 GMT");
        assertTrue(cookie.hasExpired());
    }

    @Test
    public void testHasExpiredTrueTwoDigitYearHyphenFormat() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"), "name=Nicholas; expires=Sat, 02-May-09 23:38:25 GMT");
        assertTrue(cookie.hasExpired());
    }

    @Test
    public void testMatches() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "LSID=LSDLSD; Domain=jabref.org; Path=/; Secure; expires=Sat, 02-May-99 23:38:25 GMT");
        assertTrue(cookie.matches(new URI("http://jabref.org/")));
    }

    @Test
    public void testMatchesWWWInDomain() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://www.jabref.org/"),
                "LSID=LSDLSD; Domain=www.jabref.org; Path=/; Secure; expires=Sat, 02-May-99 23:38:25 GMT");
        assertTrue(cookie.matches(new URI("http://jabref.org/")));
    }

    @Test
    public void testMatchesDotInDomain() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "LSID=LSDLSD; Domain=.jabref.org; Path=/; Secure; expires=Sat, 02-May-99 23:38:25 GMT");
        assertTrue(cookie.matches(new URI("http://jabref.org/")));
    }

    @Test
    public void testNotMatchesWhenExpired() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "LSID=LSDLSD; Domain=jabref.org; Path=/; Secure; expires=Sat, 02-May-09 23:38:25 GMT");
        assertFalse(cookie.matches(new URI("http://jabref.org/")));
    }

    @Test
    public void testNotMatchesWrongPath() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"),
                "LSID=LSDLSD; Domain=jabref.org; Path=/blog/; Secure; expires=Sat, 02-May-99 23:38:25 GMT");
        assertFalse(cookie.matches(new URI("http://jabref.org/")));
    }

    @Test
    public void testToString() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("http://jabref.org/"), "LSID=LSDLSD; Domain=jabref.org; Path=/; Secure");
        assertEquals("LSID=LSDLSD", cookie.toString());
    }
}
