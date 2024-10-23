package org.jabref.logic.search.query;

import java.util.List;

import org.jabref.model.search.LinkedFilesConstants;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

/**
 * Tests are located in {@link org.jabref.logic.search.query.SearchQueryLuceneConversionTest}.
 */
public class SearchToLuceneVisitor extends SearchBaseVisitor<Query> {

    private static final List<String> SEARCH_FIELDS = LinkedFilesConstants.PDF_FIELDS;

    private final QueryBuilder queryBuilder;

    public SearchToLuceneVisitor() {
        this.queryBuilder = new QueryBuilder(LinkedFilesConstants.LINKED_FILES_ANALYZER);
    }

    @Override
    public Query visitStart(SearchParser.StartContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public Query visitImplicitAndExpression(SearchParser.ImplicitAndExpressionContext ctx) {
        List<Query> children = ctx.expression().stream().map(this::visit).toList();
        if (children.size() == 1) {
            return children.getFirst();
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (Query child : children) {
            builder.add(child, BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    @Override
    public Query visitParenExpression(SearchParser.ParenExpressionContext ctx) {
        return visit(ctx.andExpression());
    }

    @Override
    public Query visitNegatedExpression(SearchParser.NegatedExpressionContext ctx) {
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

        if (ctx.bin_op.getType() == SearchParser.AND) {
            builder.add(left, BooleanClause.Occur.MUST);
            builder.add(right, BooleanClause.Occur.MUST);
        } else if (ctx.bin_op.getType() == SearchParser.OR) {
            builder.add(left, BooleanClause.Occur.SHOULD);
            builder.add(right, BooleanClause.Occur.SHOULD);
        }

        return builder.build();
    }

    @Override
    public Query visitComparisonExpression(SearchParser.ComparisonExpressionContext ctx) {
        return visit(ctx.comparison());
    }

    @Override
    public Query visitComparison(SearchParser.ComparisonContext ctx) {
        String field = ctx.FIELD() == null ? null : ctx.FIELD().getText();
        String term = SearchQueryConversion.unescapeSearchValue(ctx.searchValue());

        // unfielded expression
        if (field == null || "anyfield".equals(field) || "any".equals(field)) {
            return createMultiFieldQuery(term, ctx.operator());
        } else if (SEARCH_FIELDS.contains(field)) {
            return createFieldQuery(field, term, ctx.operator());
        } else {
            return new MatchNoDocsQuery();
        }
    }

    private Query createMultiFieldQuery(String value, SearchParser.OperatorContext operator) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String field : SEARCH_FIELDS) {
            builder.add(createFieldQuery(field, value, operator), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    private Query createFieldQuery(String field, String value, SearchParser.OperatorContext operator) {
        if (operator == null) {
            return createTermOrPhraseQuery(field, value);
        }

        return switch (operator.getStart().getType()) {
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
