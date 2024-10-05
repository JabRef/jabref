package org.jabref.logic.search.query;

import java.util.List;

import org.jabref.model.search.LinkedFilesConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.Token;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

/**
 * Tests are located in {@link org.jabref.logic.search.query.SearchToLuceneVisitor}.
 */
public class SearchToLuceneVisitor extends SearchBaseVisitor<Query> {

    private static final List<String> SEARCH_FIELDS = LinkedFilesConstants.PDF_FIELDS;

    private final QueryBuilder queryBuilder;

    public SearchToLuceneVisitor() {
        this.queryBuilder = new QueryBuilder(LinkedFilesConstants.LINKED_FILES_ANALYZER);
    }

    @Override
    public Query visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Query visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Query visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
        Query innerQuery = visit(ctx.expression());
        if (innerQuery instanceof MatchNoDocsQuery) {
            return innerQuery;
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(innerQuery, BooleanClause.Occur.MUST_NOT);
        return builder.build();
    }

    @Override
    public Query visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
        Query left = visit(ctx.left);
        Query right = visit(ctx.right);

        if (left instanceof MatchNoDocsQuery) {
            return right;
        }
        if (right instanceof MatchNoDocsQuery) {
            return left;
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        if (ctx.operator.getType() == SearchParser.AND) {
            builder.add(left, BooleanClause.Occur.MUST);
            builder.add(right, BooleanClause.Occur.MUST);
        } else if (ctx.operator.getType() == SearchParser.OR) {
            builder.add(left, BooleanClause.Occur.SHOULD);
            builder.add(right, BooleanClause.Occur.SHOULD);
        }

        return builder.build();
    }

    @Override
    public Query visitComparison(SearchParser.ComparisonContext ctx) {
        String field = ctx.left != null ? ctx.left.getText().toLowerCase() : null;
        String term = ctx.right.getText();

        if (term.startsWith("\"") && term.endsWith("\"")) {
            term = term.substring(1, term.length() - 1);
        }

        if (field == null || "anyfield".equals(field) || "any".equals(field)) {
            return createMultiFieldQuery(term, ctx.operator);
        } else if (SEARCH_FIELDS.contains(field)) {
            return createFieldQuery(field, term, ctx.operator);
        } else {
            return new MatchNoDocsQuery();
        }
    }

    private Query createMultiFieldQuery(String value, Token operator) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String field : SEARCH_FIELDS) {
            builder.add(createFieldQuery(field, value, operator), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    private Query createFieldQuery(String field, String value, Token operator) {
        if (operator == null) {
            return createTermOrPhraseQuery(field, value);
        }

        return switch (operator.getType()) {
            case SearchParser.REQUAL,
                 SearchParser.CREEQUAL ->
                    new RegexpQuery(new Term(field, value));
            case SearchParser.NEQUAL,
                 SearchParser.NCEQUAL,
                 SearchParser.NEEQUAL,
                 SearchParser.NCEEQUAL ->
                    createNegatedQuery(createTermOrPhraseQuery(field, value));
            case SearchParser.NREQUAL,
                 SearchParser.NCREEQUAL ->
                    createNegatedQuery(new RegexpQuery(new Term(field, value)));
            default ->
                    createTermOrPhraseQuery(field, value);
        };
    }

    private Query createNegatedQuery(Query query) {
        BooleanQuery.Builder negatedQuery = new BooleanQuery.Builder();
        negatedQuery.add(query, BooleanClause.Occur.MUST_NOT);
        return negatedQuery.build();
    }

    private Query createTermOrPhraseQuery(String field, String value) {
        if (value.contains("*") || value.contains("?")) {
            return new TermQuery(new Term(field, value));
        }
        return queryBuilder.createPhraseQuery(field, value);
    }
}
