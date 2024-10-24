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
                Arguments.of("content:term annotations:term", "term"),
                Arguments.of("content:term annotations:term", "any = term"),
                Arguments.of("content:term annotations:term", "any CONTAINS term"),
                Arguments.of("content:term annotations:term", "any MATCHES term"),
                Arguments.of("content:term annotations:term", "any =! term"),
                Arguments.of("content:term annotations:term", "any == term"),
                Arguments.of("content:term annotations:term", "any ==! term"),

                Arguments.of("content:\"two term\" annotations:\"two term\"", "\"two terms\""),
                Arguments.of("content:\"two term\" annotations:\"two term\"", "any = \"two terms\""),

                Arguments.of("content:imag", "content = image"),
                Arguments.of("annotations:imag", "annotations = image"),
                Arguments.of("content:\"imag process\"", "content = \"image processing\""),
                Arguments.of("+content:imag +annotations:process", "content = image AND annotations = processing"),
                Arguments.of("+(content:imag annotations:process) +(content:term annotations:term)", "(content = image OR annotations = processing) AND term"),
                Arguments.of("(content:on annotations:on) (+(content:two annotations:two) +(content:three annotations:three))", "one OR (two AND three)"),

                Arguments.of("(-content:term) (-annotations:term)", "any != term"),
                Arguments.of("(-content:term) (-annotations:term)", "any !== term"),
                Arguments.of("(-content:term) (-annotations:term)", "any !=! term"),
                Arguments.of("(-content:\"two term\") (-annotations:\"two term\")", "any != \"two terms\""),
                Arguments.of("+(-content:imag) +(-annotations:process)", "content != image AND annotations != processing"),

                Arguments.of("MatchNoDocsQuery(\"\")", "title = image"),
                Arguments.of("content:\"imag process\" annotations:\"imag process\"", "\"image processing\" AND author = smith"),
                Arguments.of("+(content:imag annotations:imag) +(content:process annotations:process)", "image AND (title = term OR processing)"),
                Arguments.of("(content:imag annotations:imag) (content:process annotations:process)", "image OR (title = term OR processing)"),
                Arguments.of("MatchNoDocsQuery(\"\")", "title = \"image processing\" AND author = smith"),

                Arguments.of("content:neighbou?r annotations:neighbou?r", "neighbou?r"),
                Arguments.of("content:neighbo* annotations:neighbo*", "neighbo*"),
                Arguments.of("MatchNoDocsQuery(\"\")", "title = neighbou?r"),
                Arguments.of("MatchNoDocsQuery(\"\")", "(title == chocolate) OR (author == smith)"),

                Arguments.of("content:/(John|Doe).+(John|Doe)/ annotations:/(John|Doe).+(John|Doe)/", "any =~ \"(John|Doe).+(John|Doe)\""),
                Arguments.of("content:/rev*/ annotations:/rev*/", "anyfield=~ rev*"),
                Arguments.of("content:/*rev*/ annotations:/*rev*/", "anyfield=~ *rev*"),
                Arguments.of("(-content:/.+/) (-annotations:/.+/)", "any !=~ .+"),
                Arguments.of("(-content:/.+/) (-annotations:/.+/)", "groups !=~ .+ AND any !=~ .+")
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(String expected, String searchExpression) {
        String result = SearchQueryConversion.searchToLucene(new SearchQuery(searchExpression)).toString();
        assertEquals(expected, result);
    }
}
