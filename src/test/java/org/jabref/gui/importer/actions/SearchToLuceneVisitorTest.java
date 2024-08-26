package org.jabref.gui.importer.actions;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToLuceneVisitorTest {

    public static Stream<Arguments> transformationNormal() {
        return Stream.of(
                Arguments.of("all:* AND -title:chocolate", "title != chocolate"),
                Arguments.of("chocolate", "chocolate"),
                Arguments.of("title:chocolate OR author:chocolate", "title = chocolate or author = chocolate"),
                Arguments.of("title:chocolate AND author:chocolate", "title = \"chocolate\" AND author = \"chocolate\""),
                Arguments.of("title:chocolate OR author:chocolate", "title == chocolate or author == chocolate"),
                Arguments.of("title:image\\ processing AND author:smith", "title = \"image processing\" AND author= smith")
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformationNormal(String expected, String query) {
        String result = SearchGroupsMigrationAction.migrateToLuceneSyntax(query, false);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> transformationRegularExpression() {
        return Stream.of(
                Arguments.of("all:* AND -groups:/./", "groups != .+"),
                Arguments.of("all:* AND ( -groups:/./ AND -readstatus:/./ )", "groups != .+ and readstatus != .+")
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformationRegularExpression(String expected, String query) {
        String result = SearchGroupsMigrationAction.migrateToLuceneSyntax(query, true);
        assertEquals(expected, result);
    }
}
