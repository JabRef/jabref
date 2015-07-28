package net.sf.jabref.search.describer;

import com.google.common.base.Preconditions;
import net.sf.jabref.Globals;
import net.sf.jabref.search.SearchBaseVisitor;
import net.sf.jabref.search.SearchParser;
import net.sf.jabref.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.util.StringUtil;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.regex.Pattern;

public class GrammarBasedSearchRuleDescriber implements SearchDescriber {

    private final boolean caseSensitive;
    private final boolean regExp;
    private final ParseTree parseTree;

    public GrammarBasedSearchRuleDescriber(boolean caseSensitive, boolean regExp, ParseTree parseTree) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.parseTree = Preconditions.checkNotNull(parseTree);
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        // describe advanced search expression
        sb.append(Globals.lang("This group contains entries in which")).append(' ');
        sb.append(new SearchBaseVisitor<String>() {

            @Override
            public String visitStart(SearchParser.StartContext ctx) {
                return visit(ctx.expression());
            }

            @Override
            public String visitUnaryExpression(SearchParser.UnaryExpressionContext ctx) {
                return String.format("%s %s", Globals.lang("not"), visit(ctx.expression()));
            }

            @Override
            public String visitParenExpression(SearchParser.ParenExpressionContext ctx) {
                return String.format("%s", ctx.expression());
            }

            @Override
            public String visitBinaryExpression(SearchParser.BinaryExpressionContext ctx) {
                if (ctx.operator.getText().equalsIgnoreCase("AND")) {
                    return String.format("(%s %s %s)", visit(ctx.left), Globals.lang("and"), visit(ctx.right));
                } else {
                    return String.format("(%s %s %s)", visit(ctx.left), Globals.lang("or"), visit(ctx.right));
                }
            }

            @Override
            public String visitComparison(SearchParser.ComparisonContext ctx) {

                final String field = StringUtil.unquote(ctx.left.getText(), '"');
                final String value = StringUtil.unquote(ctx.right.getText(), '"');
                final GrammarBasedSearchRule.ComparisonOperator operator = GrammarBasedSearchRule.ComparisonOperator.build(ctx.operator.getText());

                final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
                final String termQuoted = StringUtil.quoteForHTML(value);
                final String fieldSpecQuoted = regExpFieldSpec ? Globals.lang(
                        "any field that matches the regular expression <b>%0</b>",
                        StringUtil.quoteForHTML(field)) : Globals.lang("the field <b>%0</b>",
                        StringUtil.quoteForHTML(field));

                if (operator == GrammarBasedSearchRule.ComparisonOperator.CONTAINS) {
                    if (regExp) {
                        return Globals.lang(
                                "%0 contains the Regular Expression <b>%1</b>", fieldSpecQuoted, termQuoted);
                    }
                    return Globals.lang("%0 contains the term <b>%1</b>", fieldSpecQuoted, termQuoted);
                } else if (operator == GrammarBasedSearchRule.ComparisonOperator.EXACT) {
                    if (regExp) {
                        return Globals.lang("%0 matches the Regular Expression <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
                    }
                    return Globals.lang("%0 matches the term <b>%1</b>",
                            fieldSpecQuoted,
                            termQuoted);
                } else if (operator == GrammarBasedSearchRule.ComparisonOperator.DOES_NOT_CONTAIN) {
                    if (regExp) {
                        return Globals.lang("%0 doesn't contain the Regular Expression <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
                    }
                    return Globals.lang("%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                            termQuoted);
                } else {
                    throw new IllegalStateException("CANNOT HAPPEN!");
                }
            }

        }.visit(parseTree));
        sb.append(". ");
        sb.append(caseSensitive ? Globals.lang("The search is case sensitive.") : Globals.lang("The search is case insensitive."));
        return sb.toString();
    }

}
