package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    public void azureInstrumentationKeyIsNotEmpty() {
        BuildInfo buildInfo = new BuildInfo();
        assertNotNull(buildInfo.getAzureInstrumentationKey());
        assertNotEquals("", buildInfo.getAzureInstrumentationKey());
    }
}
