package org.jabref.migrations;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToLuceneMigrationTest {

    public static Stream<Arguments> transformationNormal() {
        return Stream.of(
                Arguments.of("chocolate", "chocolate"),

                Arguments.of("title:chocolate", "title=chocolate"),
                Arguments.of("title:chocolate OR author:smith", "title = chocolate or author = smith"),
                Arguments.of("title:chocolate AND author:smith", "title = \"chocolate\" AND author = \"smith\""),
                Arguments.of("title:chocolate AND author:smith", "title contains \"chocolate\" AND author matches \"smith\""),
                Arguments.of("( title:chocolate ) OR ( author:smith )", "(title == chocolate) or (author == smith)"),
                Arguments.of("( title:chocolate OR author:smith ) AND ( year:2024 )", "(title contains chocolate or author matches smith) AND (year = 2024)"),
                Arguments.of("video AND year:1932", "video and year == 1932"),
                Arguments.of("title:neighbou?r", "title =neighbou?r"),
                Arguments.of("abstract:model\\{1,2\\}ing", "abstract = model{1,2}ing"),
                Arguments.of("all:* AND -title:chocolate", "title != chocolate"),
                Arguments.of("all:* AND -title:chocolate", "not title contains chocolate"),

                // not converted, because not working in JabRef 5.x
                // Arguments.of("title:\"image processing\" OR keywords:\"image processing\"", "title|keywords = \"image processing\""),

                // not converted, because wrong syntax for JabRef 5.x
                // Arguments.of("( author:miller OR title:\"image processing\" OR keywords:\"image processing\" ) AND NOT author:brown AND NOT author:blue", "(author = miller or title|keywords = \"image processing\") and not author = brown and != author = blue"),

                // String with a space
                Arguments.of("title:image\\ processing AND author:smith", "title = \"image processing\" AND author= smith"),

                // We renamed fields
                Arguments.of("all:somecontent", "anyfield = somecontent"),
                Arguments.of("keywords:somekeyword", "anykeyword = somekeyword"),
                Arguments.of("citationkey:somebibtexkey", "key = somebibtexkey")
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformationNormal(String expected, String query) {
        String result = SearchToLuceneMigration.migrateToLuceneSyntax(query, false);
        assertEquals(expected, result);
    }

    public static Stream<Arguments> transformationRegularExpression() {
        return Stream.of(
                Arguments.of("all:* AND -groups:/.+/", "groups != .+"),
                Arguments.of("( all:* AND -groups:/.+/ ) AND ( all:* AND -readstatus:/.+/ )", "groups != .+ and readstatus != .+")
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformationRegularExpression(String expected, String query) {
        String result = SearchToLuceneMigration.migrateToLuceneSyntax(query, true);
        assertEquals(expected, result);
    }
}
