package org.jabref.http.server;

import java.util.EnumSet;

import org.jabref.http.server.resources.GroupsResource;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupsResourceTest extends ServerTest {

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(GroupsResource.class);
        addGuiBridgeToResourceConfig(resourceConfig);
        addGsonToResourceConfig(resourceConfig);
        addPreferencesToResourceConfig(resourceConfig);
        addGlobalExceptionMapperToResourceConfig(resourceConfig);
        return resourceConfig.getApplication();
    }

    @Test
    void listsGroupsAsBreadcrumbs() {
        // [utest->req~jabsrv.groups.list~1]
        setAvailableLibraries(EnumSet.of(TestBibFile.GROUPS_SERVER_TEST));
        String result = target("/libraries/" + TestBibFile.GROUPS_SERVER_TEST.id + "/groups")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
        assertEquals("""
                [
                  {
                    "name": "Papers",
                    "path": [
                      "Papers"
                    ]
                  },
                  {
                    "name": "2024",
                    "path": [
                      "Papers",
                      "2024"
                    ]
                  },
                  {
                    "name": "Machine Learning",
                    "path": [
                      "Papers",
                      "Machine Learning"
                    ]
                  },
                  {
                    "name": "Reading List",
                    "path": [
                      "Reading List"
                    ]
                  }
                ]""", result);
    }

    @Test
    void libraryWithoutGroupsReturnsEmptyList() {
        // [utest->req~jabsrv.groups.list~1]
        String result = target("/libraries/" + TestBibFile.GENERAL_SERVER_TEST.id + "/groups")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
        assertEquals("[]", result);
    }
}
