package org.jabref.http.server.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jabref.http.SrvStateManager;
import org.jabref.http.dto.GroupDTO;
import org.jabref.http.server.services.ServerUtils;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.GroupTreeNode;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/// Lists the groups of a library as a flat, pre-order list.
///
/// [impl->req~jabsrv.groups.list~1]
/// The group tree is flattened depth-first so parents precede their children. Each
/// group carries its full breadcrumb path, which lets a client render the tree as a
/// list of breadcrumbs and pick one for, e.g., import targeting.
@Path("libraries/{id}/groups")
public class GroupsResource {

    @Inject
    CliPreferences preferences;

    @Inject
    SrvStateManager srvStateManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<GroupDTO> get(@PathParam("id") String id) throws IOException {
        BibDatabaseContext context = ServerUtils.getBibDatabaseContext(id, srvStateManager, preferences.getImportFormatPreferences());
        List<GroupDTO> groups = new ArrayList<>();
        context.getMetaData().getGroups().ifPresent(root -> flatten(root, groups));
        return groups;
    }

    /// Depth-first pre-order traversal. The root `AllEntriesGroup` itself is skipped;
    /// only its descendants are emitted.
    private static void flatten(GroupTreeNode node, List<GroupDTO> out) {
        for (GroupTreeNode child : node.getChildren()) {
            out.add(toDto(child));
            flatten(child, out);
        }
    }

    private static GroupDTO toDto(GroupTreeNode node) {
        List<String> path = node.getPathFromRoot().stream()
                                .skip(1) // skip the root AllEntriesGroup
                                .map(GroupTreeNode::getName)
                                .toList();
        return new GroupDTO(node.getName(), path);
    }
}
