package net.sf.jabref.logic.net;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class CookieTest {

    @Test
    public void testCookieDashedFormat() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("hhh"), "name=TestCookie; expires=Tue, 25-Jul-17 16:43:15 GMT");
    }

    @Test
    public void testCookieSpaceFormat() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("hhh"), "name=TestCookie; expires=Tue, 25 Jul 17 16:43:15 GMT");
    }

    @Test
    public void testHasExpiredFalse() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("hhh"), "name=TestCookie; expires=Tue, 25-Jul-45 16:43:15 GMT");
        assertFalse(cookie.hasExpired());
    }

    @Test
    public void testHasExpiredTrue() throws URISyntaxException {
        Cookie cookie = new Cookie(new URI("hhh"), "name=Nicholas; expires=Sat, 02 May 09 23:38:25 GMT");
        assertTrue(cookie.hasExpired());
    }
}
