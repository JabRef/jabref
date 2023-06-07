package org.jabref.model.entry.field;

import java.util.stream.Stream;

import org.jabref.model.entry.types.BiblatexApaEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldFactoryTest {
    @Test
    void testOrFieldsTwoTerms() {
        assertEquals("aaa/bbb", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb")));
    }

    @Test
    void testOrFieldsThreeTerms() {
        assertEquals("aaa/bbb/ccc", FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb"), new UnknownField("ccc")));
    }

    private static Stream<Arguments> commentFields() {
        return Stream.of(
                Arguments.of(new UserSpecificCommentField("user1"), "comment-user1"),
                Arguments.of(new UserSpecificCommentField("other-user-id"), "comment-other-user-id")
        );
    }

    @ParameterizedTest
    @MethodSource
    void commentFields(Field expected, String name) {
        assertEquals(expected, FieldFactory.parseField(name));
    }

    @Test
    void testDoesNotParseApaFieldWithoutEntryType() {
        assertNotEquals(BiblatexApaField.ARTICLE, FieldFactory.parseField("article"));
    }

    @Test
    void testDoesParseApaFieldWithEntryType() {
        assertEquals(BiblatexApaField.ARTICLE, FieldFactory.parseField(BiblatexApaEntryType.Constitution, "article"));
    }
}
