package org.jabref.logic.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildInfoTest {

    @Test
    public void testDefaults() {
        BuildInfo buildInfo = new BuildInfo("asdf");
        assertEquals("*unknown*", buildInfo.getVersion().getFullVersion());
    }

    @Test
    public void testFileImport() {
        BuildInfo buildInfo = new BuildInfo("/org/jabref/util/build.properties");
        assertEquals("42", buildInfo.getVersion().getFullVersion());
    }

}
