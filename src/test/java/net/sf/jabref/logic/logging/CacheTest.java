package net.sf.jabref.logic.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheTest {

    @Test
    public void testCaching() {
        Cache cache = new Cache(2);
        assertEquals("", cache.get());

        cache.add("1");
        assertEquals("1", cache.get());

        cache.add("2");
        assertEquals("12", cache.get());

        cache.add("3");
        assertEquals("23", cache.get());
    }

}