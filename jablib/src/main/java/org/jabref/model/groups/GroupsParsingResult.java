package org.jabref.model.groups;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record GroupsParsingResult(GroupTreeNode root, List<String> errors) {
}
