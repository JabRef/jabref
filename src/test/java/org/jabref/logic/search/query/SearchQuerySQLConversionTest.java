package org.jabref.logic.search.query;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.stream.Stream;

import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SqlQueryNode;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.model.search.SearchFlags.CASE_SENSITIVE;
import static org.jabref.model.search.SearchFlags.REGULAR_EXPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQuerySQLConversionTest {
    private static EmbeddedPostgres pg;

    @BeforeAll
    public static void setup() throws IOException {
        pg = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    public static void teardown() throws IOException {
        pg.close();
    }

    public static Stream<Arguments> searchConversion() {
        return Stream.of(
                Arguments.of(
                        "author CONTAINS smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal ILIKE ('%smith%')) OR (main_table.field_value_transformed ILIKE ('%smith%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author = smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal ILIKE ('%smith%')) OR (main_table.field_value_transformed ILIKE ('%smith%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author =! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal LIKE ('%smith%')) OR (main_table.field_value_transformed LIKE ('%smith%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author != smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                WHERE (
                                    (inner_table.field_name = 'author') AND ((inner_table.field_value_literal ILIKE ('%smith%')) OR (inner_table.field_value_transformed ILIKE ('%smith%')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author !=! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                WHERE (
                                    (inner_table.field_name = 'author') AND ((inner_table.field_value_literal LIKE ('%smith%')) OR (inner_table.field_value_transformed LIKE ('%smith%')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author MATCHES smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            LEFT JOIN bib_fields."tableName_split_values" AS split_table
                            ON (main_table.entry_id = split_table.entry_id AND main_table.field_name = split_table.field_name)
                            WHERE (
                                ((main_table.field_name = 'author') AND ((main_table.field_value_literal ILIKE ('smith')) OR (main_table.field_value_transformed ILIKE ('smith'))))
                                OR
                                ((split_table.field_name = 'author') AND ((split_table.field_value_literal ILIKE ('smith')) OR (split_table.field_value_transformed ILIKE ('smith'))))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author == smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            LEFT JOIN bib_fields."tableName_split_values" AS split_table
                            ON (main_table.entry_id = split_table.entry_id AND main_table.field_name = split_table.field_name)
                            WHERE (
                                ((main_table.field_name = 'author') AND ((main_table.field_value_literal ILIKE ('smith')) OR (main_table.field_value_transformed ILIKE ('smith'))))
                                OR
                                ((split_table.field_name = 'author') AND ((split_table.field_value_literal ILIKE ('smith')) OR (split_table.field_value_transformed ILIKE ('smith'))))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author ==! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            LEFT JOIN bib_fields."tableName_split_values" AS split_table
                            ON (main_table.entry_id = split_table.entry_id AND main_table.field_name = split_table.field_name)
                            WHERE (
                                ((main_table.field_name = 'author') AND ((main_table.field_value_literal LIKE ('smith')) OR (main_table.field_value_transformed LIKE ('smith'))))
                                OR
                                ((split_table.field_name = 'author') AND ((split_table.field_value_literal LIKE ('smith')) OR (split_table.field_value_transformed LIKE ('smith'))))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author !== smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                LEFT JOIN bib_fields."tableName_split_values" AS split_table
                                ON (inner_table.entry_id = split_table.entry_id AND inner_table.field_name = split_table.field_name)
                                WHERE (
                                    ((inner_table.field_name = 'author') AND ((inner_table.field_value_literal ILIKE ('smith')) OR (inner_table.field_value_transformed ILIKE ('smith'))))
                                    OR
                                    ((split_table.field_name = 'author') AND ((split_table.field_value_literal ILIKE ('smith')) OR (split_table.field_value_transformed ILIKE ('smith'))))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author !==! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                LEFT JOIN bib_fields."tableName_split_values" AS split_table
                                ON (inner_table.entry_id = split_table.entry_id AND inner_table.field_name = split_table.field_name)
                                WHERE (
                                    ((inner_table.field_name = 'author') AND ((inner_table.field_value_literal LIKE ('smith')) OR (inner_table.field_value_transformed LIKE ('smith'))))
                                    OR
                                    ((split_table.field_name = 'author') AND ((split_table.field_value_literal LIKE ('smith')) OR (split_table.field_value_transformed LIKE ('smith'))))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author =~ smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal ~* ('smith')) OR (main_table.field_value_transformed ~* ('smith')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author =~! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal ~ ('smith')) OR (main_table.field_value_transformed ~ ('smith')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author !=~ smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                WHERE (
                                    (inner_table.field_name = 'author') AND ((inner_table.field_value_literal ~* ('smith')) OR (inner_table.field_value_transformed ~* ('smith')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "author !=~! smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                WHERE (
                                    (inner_table.field_name = 'author') AND ((inner_table.field_value_literal ~ ('smith')) OR (inner_table.field_value_transformed ~ ('smith')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%smith%')) OR (main_table.field_value_transformed ILIKE ('%smith%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "any == smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            LEFT JOIN bib_fields."tableName_split_values" AS split_table
                            ON (main_table.entry_id = split_table.entry_id AND main_table.field_name = split_table.field_name)
                            WHERE (
                                (main_table.field_name != 'groups')
                                AND (
                                    ((main_table.field_value_literal ILIKE ('smith')) OR (main_table.field_value_transformed ILIKE ('smith')))
                                    OR
                                    ((split_table.field_value_literal ILIKE ('smith')) OR (split_table.field_value_transformed ILIKE ('smith')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "anyfield != smith",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                                SELECT inner_table.entry_id
                                FROM bib_fields."tableName" AS inner_table
                                WHERE (
                                    (inner_table.field_name != 'groups') AND ((inner_table.field_value_literal ILIKE ('%smith%')) OR (inner_table.field_value_transformed ILIKE ('%smith%')))
                                )
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "title = \"computer science\"",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'title') AND ((main_table.field_value_literal ILIKE ('%computer science%')) OR (main_table.field_value_transformed ILIKE ('%computer science%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "term1 term2 term3",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%term1%')) OR (main_table.field_value_transformed ILIKE ('%term1%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%term2%')) OR (main_table.field_value_transformed ILIKE ('%term2%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%term3%')) OR (main_table.field_value_transformed ILIKE ('%term3%')))
                            )
                        )
                        ,
                        cte3 AS (
                            SELECT entry_id FROM cte0
                            INTERSECT
                            SELECT entry_id FROM cte1
                            INTERSECT
                            SELECT entry_id FROM cte2
                        )
                        SELECT * FROM cte3 GROUP BY entry_id"""
                ),

                Arguments.of(
                    "a OR b AND c",
                    """
                    WITH
                    cte0 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                        )
                    )
                    ,
                    cte1 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                        )
                    )
                    ,
                    cte2 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                        )
                    )
                    ,
                    cte3 AS (
                        SELECT entry_id
                        FROM cte1
                        INTERSECT
                        SELECT entry_id
                        FROM cte2
                    )
                    ,
                    cte4 AS (
                        SELECT entry_id
                        FROM cte0
                        UNION
                        SELECT entry_id
                        FROM cte3
                    )
                    SELECT * FROM cte4 GROUP BY entry_id"""
                ),

                Arguments.of(
                    "a AND b OR c",
                    """
                    WITH
                    cte0 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                        )
                    )
                    ,
                    cte1 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                        )
                    )
                    ,
                    cte2 AS (
                        SELECT entry_id
                        FROM cte0
                        INTERSECT
                        SELECT entry_id
                        FROM cte1
                    )
                    ,
                    cte3 AS (
                        SELECT main_table.entry_id
                        FROM bib_fields."tableName" AS main_table
                        WHERE (
                            (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                        )
                    )
                    ,
                    cte4 AS (
                        SELECT entry_id
                        FROM cte2
                        UNION
                        SELECT entry_id
                        FROM cte3
                    )
                    SELECT * FROM cte4 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "(a OR b) AND c",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT entry_id
                            FROM cte0
                            UNION
                            SELECT entry_id
                            FROM cte1
                        )
                        ,
                        cte3 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                            )
                        )
                        ,
                        cte4 AS (
                            SELECT entry_id
                            FROM cte2
                            INTERSECT
                            SELECT entry_id
                            FROM cte3
                        )
                        SELECT * FROM cte4 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "a OR (b AND c)",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                            )
                        )
                        ,
                        cte3 AS (
                            SELECT entry_id
                            FROM cte1
                            INTERSECT
                            SELECT entry_id
                            FROM cte2
                        )
                        ,
                        cte4 AS (
                            SELECT entry_id
                            FROM cte0
                            UNION
                            SELECT entry_id
                            FROM cte3
                        )
                        SELECT * FROM cte4 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "(a OR b) AND (c OR d)",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT entry_id
                            FROM cte0
                            UNION
                            SELECT entry_id
                            FROM cte1
                        )
                        ,
                        cte3 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                            )
                        )
                        ,
                        cte4 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%d%')) OR (main_table.field_value_transformed ILIKE ('%d%')))
                            )
                        )
                        ,
                        cte5 AS (
                            SELECT entry_id
                            FROM cte3
                            UNION
                            SELECT entry_id
                            FROM cte4
                        )
                        ,
                        cte6 AS (
                            SELECT entry_id
                            FROM cte2
                            INTERSECT
                            SELECT entry_id
                            FROM cte5
                        )
                        SELECT * FROM cte6 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "a AND NOT (b OR c)",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a%')) OR (main_table.field_value_transformed ILIKE ('%a%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%b%')) OR (main_table.field_value_transformed ILIKE ('%b%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%c%')) OR (main_table.field_value_transformed ILIKE ('%c%')))
                            )
                        )
                        ,
                        cte3 AS (
                            SELECT entry_id
                            FROM cte1
                            UNION
                            SELECT entry_id
                            FROM cte2
                        )
                        ,
                        cte4 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE main_table.entry_id NOT IN (
                               SELECT entry_id
                               FROM cte3
                            )
                        )
                        ,
                        cte5 AS (
                            SELECT entry_id
                            FROM cte0
                            INTERSECT
                            SELECT entry_id
                            FROM cte4
                        )
                        SELECT * FROM cte5 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "a'b",
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%a''b%')) OR (main_table.field_value_transformed ILIKE ('%a''b%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void searchConversion(String searchExpression, String expected) throws SQLException {
        try (Connection connection = pg.getPostgresDatabase().getConnection()) {
            SqlQueryNode sqlQueryNode = SearchQueryConversion.searchToSql("tableName", new SearchQuery(searchExpression));
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQueryNode.cte())) {
                for (int i = 0; i < sqlQueryNode.params().size(); i++) {
                    preparedStatement.setString(i + 1, sqlQueryNode.params().get(i));
                }
                String sql = preparedStatement.toString();
                assertEquals(expected, sql);
            }
        }
    }

    public static Stream<Arguments> unFieldedTermsWithSearchBarFlags() {
        return Stream.of(
                Arguments.of(
                        "Test",
                        EnumSet.noneOf(SearchFlags.class),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%Test%')) OR (main_table.field_value_transformed ILIKE ('%Test%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "Test",
                        EnumSet.of(CASE_SENSITIVE),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal LIKE ('%Test%')) OR (main_table.field_value_transformed LIKE ('%Test%')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "Test",
                        EnumSet.of(REGULAR_EXPRESSION),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ~* ('Test')) OR (main_table.field_value_transformed ~* ('Test')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "Test",
                        EnumSet.of(CASE_SENSITIVE, REGULAR_EXPRESSION),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ~ ('Test')) OR (main_table.field_value_transformed ~ ('Test')))
                            )
                        )
                        SELECT * FROM cte0 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "Test AND author =! Smith",
                        EnumSet.noneOf(SearchFlags.class),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ILIKE ('%Test%')) OR (main_table.field_value_transformed ILIKE ('%Test%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal LIKE ('%Smith%')) OR (main_table.field_value_transformed LIKE ('%Smith%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT entry_id
                            FROM cte0
                            INTERSECT
                            SELECT entry_id
                            FROM cte1
                        )
                        SELECT * FROM cte2 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "Test AND author =! Smith",
                        EnumSet.of(CASE_SENSITIVE),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal LIKE ('%Test%')) OR (main_table.field_value_transformed LIKE ('%Test%')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal LIKE ('%Smith%')) OR (main_table.field_value_transformed LIKE ('%Smith%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT entry_id
                            FROM cte0
                            INTERSECT
                            SELECT entry_id
                            FROM cte1
                        )
                        SELECT * FROM cte2 GROUP BY entry_id"""
                ),

                Arguments.of(
                        "any =~ Test AND author =! Smith",
                        EnumSet.of(CASE_SENSITIVE),
                        """
                        WITH
                        cte0 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name != 'groups') AND ((main_table.field_value_literal ~* ('Test')) OR (main_table.field_value_transformed ~* ('Test')))
                            )
                        )
                        ,
                        cte1 AS (
                            SELECT main_table.entry_id
                            FROM bib_fields."tableName" AS main_table
                            WHERE (
                                (main_table.field_name = 'author') AND ((main_table.field_value_literal LIKE ('%Smith%')) OR (main_table.field_value_transformed LIKE ('%Smith%')))
                            )
                        )
                        ,
                        cte2 AS (
                            SELECT entry_id
                            FROM cte0
                            INTERSECT
                            SELECT entry_id
                            FROM cte1
                        )
                        SELECT * FROM cte2 GROUP BY entry_id"""
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void unFieldedTermsWithSearchBarFlags(String searchExpression, EnumSet<SearchFlags> searchFlags, String expected) throws SQLException {
        try (Connection connection = pg.getPostgresDatabase().getConnection()) {
            SqlQueryNode sqlQueryNode = SearchQueryConversion.searchToSql("tableName", new SearchQuery(searchExpression, searchFlags));
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQueryNode.cte())) {
                for (int i = 0; i < sqlQueryNode.params().size(); i++) {
                    preparedStatement.setString(i + 1, sqlQueryNode.params().get(i));
                }
                String sql = preparedStatement.toString();
                assertEquals(expected, sql);
            }
        }
    }
}
