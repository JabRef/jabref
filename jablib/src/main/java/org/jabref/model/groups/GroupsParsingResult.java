package org.jabref.model.groups;

import java.util.List;

public record GroupsParsingResult(GroupTreeNode root, List<String> errors) {
    public GroupsParsingResult {
        if (errors == null) {
            errors = List.of();
        }
    }
}
