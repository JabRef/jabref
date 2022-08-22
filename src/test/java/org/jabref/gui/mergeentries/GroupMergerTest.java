package org.jabref.gui.mergeentries;

import java.util.stream.Stream;

import org.jabref.gui.mergeentries.newmergedialog.fieldsmerger.GroupMerger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupMergerTest {

    GroupMerger groupMerger;

    @BeforeEach
    void setup() {
        this.groupMerger = new GroupMerger();
    }

    private static Stream<Arguments> mergeShouldMergeGroupsCorrectly() {
        return Stream.of(
                Arguments.of("a", "b", "a, b"),
                Arguments.of("a", "", "a"),
                Arguments.of("", "", ""),
                Arguments.of("", "b", "b"),
                Arguments.of("a, b", "c", "a, b, c"),
                Arguments.of("a, b, c", "c", "a, b, c"),
                Arguments.of("a, b", "c, d", "a, b, c, d"),
                Arguments.of("a, b, c", "b, z", "a, b, c, z")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void mergeShouldMergeGroupsCorrectly(String groupsA, String groupsB, String expected) {
     assertEquals(expected, groupMerger.merge(groupsA, groupsB));
    }
}
