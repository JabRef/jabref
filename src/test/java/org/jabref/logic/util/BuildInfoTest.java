package org.jabref.logic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class BuildInfoTest {

    @Test
    public void testDefaults() {
        BuildInfo buildInfo = new BuildInfo("asdf");
        assertEquals("UNKNOWN", buildInfo.version.getFullVersion());
    }

    @Test
    public void testFileImport() {
        BuildInfo buildInfo = new BuildInfo(
            "/org/jabref/util/build.properties"
        );
        assertEquals("42", buildInfo.version.getFullVersion());
    }

    @Test
    public void azureInstrumentationKeyIsNotEmpty() {
        BuildInfo buildInfo = new BuildInfo();
        assertNotNull(buildInfo.azureInstrumentationKey);
    }
}
