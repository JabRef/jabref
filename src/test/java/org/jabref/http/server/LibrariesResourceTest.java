package org.jabref.http.server;

import java.util.EnumSet;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibrariesResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibrariesResource.class);
        addPreferencesToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void defaultOneTestLibrary() throws Exception {
        assertEquals("[\"" + TestBibFile.GENERAL_SERVER_TEST.id + "\"]", target("/libraries").request().get(String.class));
    }

    @Test
    void twoTestLibraries() {
        EnumSet<TestBibFile> availableLibraries = EnumSet.of(TestBibFile.GENERAL_SERVER_TEST, TestBibFile.JABREF_AUTHORS);
        setAvailableLibraries(availableLibraries);
        // We cannot use a string constant as the path changes from OS to OS. Therefore, we need to dynamically create the expected result.
        String expected = availableLibraries.stream().map(file -> file.id).collect(Collectors.joining("\",\"", "[\"", "\"]"));
        assertEquals(expected, target("/libraries").request().get(String.class));
    }
}
