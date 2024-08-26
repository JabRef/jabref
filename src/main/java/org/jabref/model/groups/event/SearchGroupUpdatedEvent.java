package org.jabref.model.groups.event;

import org.jabref.model.groups.GroupTreeNode;

public record SearchGroupUpdatedEvent(GroupTreeNode group) {
}
