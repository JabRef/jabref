package org.jabref.model.entry.field;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldTextMapperTest {

    static Stream<Arguments> displayNames() {
        return Stream.of(
                Arguments.of("Author", StandardField.AUTHOR),
                Arguments.of("DOI", StandardField.DOI),
                Arguments.of("Citation Key", InternalField.KEY_FIELD),
                Arguments.of("Entry Type", InternalField.TYPE_HEADER),
                Arguments.of("JabRef-internal-id", InternalField.INTERNAL_ID_FIELD),
                Arguments.of("myField", new UnknownField("myField"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void displayNames(String expected, Field field) {
        assertEquals(expected, FieldTextMapper.getDisplayName(field));
    }
}
