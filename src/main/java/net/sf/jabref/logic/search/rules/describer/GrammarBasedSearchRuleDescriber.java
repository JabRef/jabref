/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.search.rules.describer;

import java.util.Objects;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.search.SearchBaseVisitor;
import net.sf.jabref.search.SearchParser;

import org.antlr.v4.runtime.tree.ParseTree;

public class GrammarBasedSearchRuleDescriber implements SearchDescriber {

    private final boolean caseSensitive;
    private final boolean regExp;
    private final ParseTree parseTree;

    public GrammarBasedSearchRuleDescriber(boolean caseSensitive, boolean regExp, ParseTree parseTree) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.parseTree = Objects.requireNonNull(parseTree);
    }

    @Override
    public String getDescription() {
        StringBuilder stringBuilder = new StringBuilder();
        // describe advanced search expression
        stringBuilder.append(Localization.lang("This search contains entries in which")).append(' ')
                .append(new SearchBaseVisitor<String>() {

            @Override
            public String visitStart(SearchParser.StartContext context) {
                return visit(context.expression());
            }

            @Override
            public String visitUnaryExpression(SearchParser.UnaryExpressionContext context) {
                return String.format("%s %s", Localization.lang("not"), visit(context.expression()));
            }

            @Override
            public String visitParenExpression(SearchParser.ParenExpressionContext context) {
                return String.format("%s", context.expression());
            }

            @Override
            public String visitBinaryExpression(SearchParser.BinaryExpressionContext context) {
                if ("AND".equalsIgnoreCase(context.operator.getText())) {
                    return String.format("(%s %s %s)", visit(context.left), Localization.lang("and"), visit(context.right));
                } else {
                    return String.format("(%s %s %s)", visit(context.left), Localization.lang("or"), visit(context.right));
                }
            }

            @Override
            public String visitComparison(SearchParser.ComparisonContext context) {

                final String field = StringUtil.unquote(context.left.getText(), '"');
                final String value = StringUtil.unquote(context.right.getText(), '"');
                final GrammarBasedSearchRule.ComparisonOperator operator = GrammarBasedSearchRule.ComparisonOperator.build(context.operator.getText());

                final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
                final String termQuoted = StringUtil.quoteForHTML(value);
                final String fieldSpecQuoted = regExpFieldSpec ? Localization.lang(
                        "any field that matches the regular expression <b>%0</b>",
                        StringUtil.quoteForHTML(field)) : Localization.lang("the field <b>%0</b>",
                        StringUtil.quoteForHTML(field));

                if (operator == GrammarBasedSearchRule.ComparisonOperator.CONTAINS) {
                    if (regExp) {
                        return Localization.lang("%0 contains the regular expression <b>%1</b>", fieldSpecQuoted,
                                termQuoted);
                    }
                    return Localization.lang("%0 contains the term <b>%1</b>", fieldSpecQuoted, termQuoted);
                } else if (operator == GrammarBasedSearchRule.ComparisonOperator.EXACT) {
                    if (regExp) {
                        return Localization.lang("%0 matches the regular expression <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
                    }
                    return Localization.lang("%0 matches the term <b>%1</b>",
                            fieldSpecQuoted,
                            termQuoted);
                } else if (operator == GrammarBasedSearchRule.ComparisonOperator.DOES_NOT_CONTAIN) {
                    if (regExp) {
                        return Localization.lang("%0 doesn't contain the regular expression <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
                    }
                    return Localization.lang("%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                            termQuoted);
                } else {
                    throw new IllegalStateException("CANNOT HAPPEN!");
                }
            }

        }.visit(parseTree));
        stringBuilder.append(". ");
        stringBuilder.append(caseSensitive ? Localization
                .lang("The search is case sensitive.") :
            Localization.lang("The search is case insensitive."));
        return stringBuilder.toString();
    }

}
