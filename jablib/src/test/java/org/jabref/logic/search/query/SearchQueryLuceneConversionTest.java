package org.jabref.logic.search.query;

import java.util.stream.Stream;

import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryLuceneConversionTest {

    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of("a", "a"),
                Arguments.of("the", "the"),
                Arguments.of("term", "term"),
                Arguments.of("term", "any = term"),
                Arguments.of("term", "any CONTAINS term"),
                Arguments.of("term", "any MATCHES term"),
                Arguments.of("term", "any =! term"),
                Arguments.of("term", "any == term"),
                Arguments.of("term", "any ==! term"),

                Arguments.of("\"two terms\"", "\"two terms\""),
                Arguments.of("\"two terms\"", "any = \"two terms\""),
                Arguments.of("NOT (term)", "NOT term"),

                Arguments.of("neighbou?r", "neighbou?r"),
                Arguments.of("neighbo*", "neighbo*"),
                Arguments.of("*term*", "*term*"),
                Arguments.of("content:*image*", "content = *image*"),

                Arguments.of("content:image", "content = image"),
                Arguments.of("annotations:image", "annotations = image"),
                Arguments.of("content:\"image processing\"", "content = \"image processing\""),
                Arguments.of("content:image AND annotations:processing", "content = image AND annotations = processing"),
                Arguments.of("(content:image OR annotations:processing) AND term", "(content = image OR annotations = processing) AND term"),
                Arguments.of("one OR (two AND three)", "one OR (two AND three)"),

                Arguments.of("NOT term", "any != term"),
                Arguments.of("NOT term", "any !== term"),
                Arguments.of("NOT term", "any !=! term"),
                Arguments.of("NOT \"two terms\"", "any != \"two terms\""),
                Arguments.of("content:image AND NOT annotations:processing", "content = image AND annotations != processing"),

                // ignore non pdf fields
                Arguments.of("", "title = image"),
                Arguments.of("\"image processing\"", "\"image processing\" AND author = smith"),
                Arguments.of("image AND (processing)", "image AND (title = term OR processing)"),
                Arguments.of("image OR (processing)", "image OR (title = term OR processing)"),
                Arguments.of("", "title = \"image processing\" AND author = smith"),

                Arguments.of("", "title = neighbou?r"),
                Arguments.of("", "(title == chocolate) OR (author == smith)"),

                // regex
                Arguments.of("/(John|Doe).+(John|Doe)/", "any =~ \"(John|Doe).+(John|Doe)\""),
                Arguments.of("/rev*/", "anyfield=~ rev*"),
                Arguments.of("NOT /.+/", "any !=~ .+")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(String expected, String searchExpression) {
        assertEquals(expected, SearchQueryConversion.searchToLucene(new SearchQuery(searchExpression)));
    }
}
