package org.jabref.model.entry.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.adr.embedded.ADR;
import java.util.stream.Stream;
import org.jabref.model.entry.types.BiblatexApaEntryType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FieldFactoryTest {

  @ADR(49)
  @Test
  void orFieldsTwoTerms() {
    assertEquals("aaa/bbb",
        FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb")));
  }

  @ADR(49)
  @Test
  void orFieldsThreeTerms() {
    assertEquals("aaa/bbb/ccc",
        FieldFactory.serializeOrFields(new UnknownField("aaa"), new UnknownField("bbb"),
            new UnknownField("ccc")));
  }

  private static Stream<Arguments> fieldsWithoutFieldProperties() {
    return Stream.of(
        // comment fields
        Arguments.of(new UserSpecificCommentField("user1"), "comment-user1"),
        Arguments.of(new UserSpecificCommentField("other-user-id"), "comment-other-user-id"),
        // unknown field
        Arguments.of(new UnknownField("cAsEd"), "cAsEd"),
        Arguments.of(new UnknownField("rights"), "UnknownField{name='rights'}")
    );
  }

  @ParameterizedTest
  @MethodSource
  void fieldsWithoutFieldProperties(Field expected, String name) {
    assertEquals(expected, FieldFactory.parseField(name));
  }

  @Test
  void doesNotParseApaFieldWithoutEntryType() {
    assertNotEquals(BiblatexApaField.ARTICLE, FieldFactory.parseField("article"));
  }

  @Test
  void doesParseApaFieldWithEntryType() {
    assertEquals(BiblatexApaField.ARTICLE,
        FieldFactory.parseField(BiblatexApaEntryType.Constitution, "article"));
  }
}
