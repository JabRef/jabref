package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BuildInfoTest {

    @Test
    public void defaults() {
        BuildInfo buildInfo = new BuildInfo("asdf");
        assertEquals("UNKNOWN", buildInfo.version.getFullVersion());
    }

    @Test
    public void fileImport() {
        BuildInfo buildInfo = new BuildInfo("/org/jabref/util/build.properties");
        assertEquals("42", buildInfo.version.getFullVersion());
    }

    @Test
    public void azureInstrumentationKeyIsNotEmpty() {
        BuildInfo buildInfo = new BuildInfo();
        assertNotNull(buildInfo.azureInstrumentationKey);
    }
}
