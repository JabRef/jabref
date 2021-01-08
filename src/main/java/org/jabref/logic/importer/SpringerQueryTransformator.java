package org.jabref.logic.importer;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.JabRefException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.AndQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.BooleanQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.OrQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts a query string written in lucene syntax into a complex  query.
 *
 * For simplicity this is currently limited to fielded data and the boolean AND operator.
 */
public class SpringerQueryTransformator extends AbstractQueryTransformator {

    @Override
    public String getLogicalAndOperator() {
        return " AND ";
    }

    @Override
    public String getLogicalOrOperator() {
        return " OR ";
    }

    @Override
    protected String handleAuthor(String textAsString) {
        return String.format("name:\"%s\"", textAsString);
    }

    @Override
    protected String handleTitle(String textAsString) {
        return String.format("title:\"%s\"", textAsString);
    }

    @Override
    protected String handleJournal(String textAsString) {
        return String.format("journal:\"%s\"", textAsString);

    }

    @Override
    protected String handleYear(String textAsString) {
        return String.format("date:%s*", textAsString);
    }

    @Override
    protected String handleYearRange(String textAsString) {
        String[] split = textAsString.split("-");
        StringJoiner resultBuilder = new StringJoiner("* OR date:", "(date:", "*)");
        for (int i = Integer.parseInt(split[0]); i <= Integer.parseInt(split[1]); i++) {
            resultBuilder.add(String.valueOf(i));
        }
        return resultBuilder.toString();
    }

    @Override
    protected String handleUnFieldedTerm(String term) {
        return "\"" + term + "\"";
    }
}
