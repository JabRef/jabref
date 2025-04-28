package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildInfoTest {

    @Test
    void defaults() {
        BuildInfo buildInfo = new BuildInfo("asdf");
        assertEquals("UNKNOWN", buildInfo.version.getFullVersion());
    }

    @Test
    void fileImport() {
        BuildInfo buildInfo = new BuildInfo("/org/jabref/util/build.properties");
        assertEquals("42", buildInfo.version.getFullVersion());
    }
}
