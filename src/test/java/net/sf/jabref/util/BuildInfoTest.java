package net.sf.jabref.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class BuildInfoTest {

    @Test
    public void testDefaults() {
        BuildInfo buildInfo = new BuildInfo("asdf");
        assertEquals("1", buildInfo.getNumber());
        assertEquals("", buildInfo.getDate());
        assertEquals("dev", buildInfo.getVersion());
    }

    @Test
    public void testFileImport() {
        BuildInfo buildInfo = new BuildInfo("/net/sf/jabref/util/build.properties");
        assertEquals("2", buildInfo.getNumber());
        assertEquals("June 30 2015", buildInfo.getDate());
        assertEquals("42", buildInfo.getVersion());
    }

}