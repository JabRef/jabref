package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// A group of a library, flattened out of the group tree.
///
/// `name` is the group's own name. JabRef enforces library-scoped unique group
/// names, so `name` serves as the group's identity within a library.
/// `path` is the breadcrumb trail from the top-level group down to and including
/// this group; its last element therefore equals `name`. The root `AllEntriesGroup`
/// is not part of the path.
@NullMarked
public record GroupDTO(String name, List<String> path) {
}
