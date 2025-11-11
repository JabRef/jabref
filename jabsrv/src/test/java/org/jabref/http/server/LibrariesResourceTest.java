package org.jabref.http.server;

import java.util.EnumSet;

import org.jabref.http.server.resources.LibrariesResource;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibrariesResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(LibrariesResource.class);
        addFilesToServeToResourceConfig(resourceConfig);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void defaultOneTestLibrary() {
        String expected = """
                [
                  "%s",
                  "%s"
                ]""".formatted(TestBibFile.GENERAL_SERVER_TEST.id, "demo");
        assertEquals(expected, target("/libraries").request().get(String.class));
    }

    @Test
    void twoTestLibraries() {
        EnumSet<TestBibFile> availableLibraries = EnumSet.of(TestBibFile.GENERAL_SERVER_TEST, TestBibFile.CHOCOLATE_BIB);
        setAvailableLibraries(availableLibraries);

        String expected = """
                [
                  "%s",
                  "%s",
                  "%s"
                ]""".formatted(TestBibFile.GENERAL_SERVER_TEST.id, TestBibFile.CHOCOLATE_BIB.id, "demo");
        assertEquals(expected, target("/libraries").request().get(String.class));
    }
}
