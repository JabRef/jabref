package org.jabref.model.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.DateGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class TreeCollectorTest {

    @Test
    void mergesChildrenOfEquivalentRoots() {
        GroupTreeNode firstRoot = node("2024");
        firstRoot.addSubgroup(group("2024-01"));
        GroupTreeNode secondRoot = node("2024");
        secondRoot.addSubgroup(group("2024-02"));

        List<GroupTreeNode> mergedRoots = TreeCollector.mergeIntoTree(
                Stream.of(firstRoot, secondRoot),
                GroupTreeNode::getGroup);

        assertEquals(1, mergedRoots.size());
        assertEquals(
                Set.of("2024-01", "2024-02"),
                mergedRoots.getFirst().getChildren().stream().map(GroupTreeNode::getName).collect(Collectors.toSet()));
    }

    @Test
    void mergesCollidingGrandchildrenRecursively() {
        GroupTreeNode firstRoot = node("2024");
        GroupTreeNode firstChild = firstRoot.addSubgroup(group("2024-01"));
        firstChild.addSubgroup(group("2024-01-01"));
        GroupTreeNode secondRoot = node("2024");
        GroupTreeNode secondChild = secondRoot.addSubgroup(group("2024-01"));
        secondChild.addSubgroup(group("2024-01-01"));
        secondChild.addSubgroup(group("2024-01-02"));

        List<GroupTreeNode> mergedRoots = TreeCollector.mergeIntoTree(
                Stream.of(firstRoot, secondRoot),
                GroupTreeNode::getGroup);

        assertEquals(1, mergedRoots.size());
        List<GroupTreeNode> children = mergedRoots.getFirst().getChildren();
        assertEquals(1, children.size());
        assertEquals(
                Set.of("2024-01-01", "2024-01-02"),
                children.getFirst().getChildren().stream().map(GroupTreeNode::getName).collect(Collectors.toSet()));
    }

    private static GroupTreeNode node(String name) {
        return GroupTreeNode.fromGroup(group(name));
    }

    private static DateGroup group(String name) {
        return new DateGroup(name, GroupHierarchyType.INDEPENDENT, StandardField.DATE, name);
    }
}
