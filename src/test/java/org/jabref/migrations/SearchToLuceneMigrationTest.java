package org.jabref.migrations;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchToLuceneMigrationTest {

    public static Stream<Arguments> transformationNormal() {
        return Stream.of(
                // If "any:" should not be added, see e46e0a23d7b9526bd449cbecfb189ae6dbc40a28 for a fix
                Arguments.of("any:chocolate", "chocolate"),

                Arguments.of("title:chocolate", "title=chocolate"),
                Arguments.of("title:chocolate OR author:smith", "title = chocolate or author = smith"),
                Arguments.of("groups:\\/exclude", "groups= /exclude"),
                Arguments.of("title:chocolate AND author:smith", "title = \"chocolate\" AND author = \"smith\""),
                Arguments.of("title:chocolate AND author:smith", "title contains \"chocolate\" AND author matches \"smith\""),
                Arguments.of("( title:chocolate ) OR ( author:smith )", "(title == chocolate) or (author == smith)"),
                Arguments.of("( title:chocolate OR author:smith ) AND ( year:2024 )", "(title contains chocolate or author matches smith) AND (year = 2024)"),
                Arguments.of("any:video AND year:1932", "video and year == 1932"),
                Arguments.of("title:neighbou?r", "title =neighbou?r"),
                Arguments.of("abstract:model\\{1,2\\}ing", "abstract = model{1,2}ing"),
                Arguments.of("any:* AND -title:chocolate", "title != chocolate"),
                Arguments.of("any:* AND -title:chocolate", "not title contains chocolate"),
                // https://github.com/JabRef/jabref/issues/11654#issuecomment-2313178736
                Arguments.of("( groups:\\:paywall AND -file:/.+/ ) AND -groups:\\/exclude", "groups=:paywall and file!=\"\" and groups!=/exclude"),

                Arguments.of("( any:* AND -( groups:alpha ) ) AND ( any:* AND -( groups:beta ) )", "NOT(groups=alpha) AND NOT(groups=beta)"),
                // not converted, because not working in JabRef 5.x
                // Arguments.of("title:\"image processing\" OR keywords:\"image processing\"", "title|keywords = \"image processing\""),

                // not converted, because wrong syntax for JabRef 5.x
                // Arguments.of("( author:miller OR title:\"image processing\" OR keywords:\"image processing\" ) AND NOT author:brown AND NOT author:blue", "(author = miller or title|keywords = \"image processing\") and not author = brown and != author = blue"),

                // String with a space
                Arguments.of("title:image\\ processing AND author:smith", "title = \"image processing\" AND author= smith"),

                // We renamed fields
                Arguments.of("any:somecontent", "anyfield = somecontent"),
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
                Arguments.of("any:* AND -groups:/.+/", "groups != .+"),
                Arguments.of("( any:* AND -groups:/.+/ ) AND ( any:* AND -readstatus:/.+/ )", "groups != .+ and readstatus != .+"),
                Arguments.of("author:/(John|Doe).+(John|Doe)/", "author = \"(John|Doe).+(John|Doe)\""),
                Arguments.of("any:/rev*/", "anyfield=rev*"),
                Arguments.of("any:/*rev*/", "anyfield=*rev*")
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformationRegularExpression(String expected, String query) {
        String result = SearchToLuceneMigration.migrateToLuceneSyntax(query, true);
        assertEquals(expected, result);
    }
}
