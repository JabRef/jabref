package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void azureInstrumentationKeyIsNotEmpty() {
        BuildInfo buildInfo = new BuildInfo();
        assertNotNull(buildInfo.azureInstrumentationKey);
    }
}
