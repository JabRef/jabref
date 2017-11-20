package org.jabref.gui.search.rules.describer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.util.TextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.model.strings.StringUtil;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.tree.ParseTree;

public class GrammarBasedSearchRuleDescriber implements SearchDescriber {

    private final boolean caseSensitive;
    private final boolean regExp;
    private final ParseTree parseTree;
    private final double textSize = 13;

    public GrammarBasedSearchRuleDescriber(boolean caseSensitive, boolean regExp, ParseTree parseTree) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.parseTree = Objects.requireNonNull(parseTree);
    }

    @Override
    public TextFlow getDescription() {
        TextFlow textFlow = new TextFlow();
        DescriptionSearchBaseVisitor descriptionSearchBaseVisitor = new DescriptionSearchBaseVisitor();

        // describe advanced search expression
        textFlow.getChildren().add(TextUtil.createText(Localization.lang("This search contains entries in which "), textSize));
        textFlow.getChildren().addAll(descriptionSearchBaseVisitor.visit(parseTree));
        textFlow.getChildren().add(TextUtil.createText(". ", textSize));
        textFlow.getChildren().add(TextUtil.createText(caseSensitive ? Localization
                .lang("The search is case sensitive.") :
                Localization.lang("The search is case insensitive."), textSize));
        return textFlow;
    }

    private class DescriptionSearchBaseVisitor extends SearchBaseVisitor<List<Text>> {

        @Override
        public List<Text> visitStart(SearchParser.StartContext context) {
            return visit(context.expression());
        }

        @Override
        public List<Text> visitUnaryExpression(SearchParser.UnaryExpressionContext context) {
            List<Text> textList = visit(context.expression());
            textList.add(0, TextUtil.createText(Localization.lang("not "), textSize));
            return textList;
        }

        @Override
        public List<Text> visitParenExpression(SearchParser.ParenExpressionContext context) {
            ArrayList<Text> textList = new ArrayList<>();
            textList.add(TextUtil.createText(String.format("%s", context.expression()), textSize));
            return textList;
        }

        @Override
        public List<Text> visitBinaryExpression(SearchParser.BinaryExpressionContext context) {
            List<Text> textList = visit(context.left);
            if ("AND".equalsIgnoreCase(context.operator.getText())) {
                textList.add(TextUtil.createText(Localization.lang(" and "), textSize));
            } else {
                textList.add(TextUtil.createText(Localization.lang(" or "), textSize));
            }
            textList.addAll(visit(context.right));
            return textList;
        }

        @Override
        public List<Text> visitComparison(SearchParser.ComparisonContext context) {
            final List<Text> textList = new ArrayList<>();
            final Optional<SearchParser.NameContext> fieldDescriptor = Optional.ofNullable(context.left);
            final String value = StringUtil.unquote(context.right.getText(), '"');
            if (!fieldDescriptor.isPresent()) {
                TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(caseSensitive, regExp, value).getDescription();
                description.getChildren().forEach(it -> textList.add((Text) it));
                return textList;
            }

            final String field = StringUtil.unquote(fieldDescriptor.get().getText(), '"');
            final GrammarBasedSearchRule.ComparisonOperator operator = GrammarBasedSearchRule.ComparisonOperator.build(context.operator.getText());

            final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
            textList.add(TextUtil.createText(regExpFieldSpec ? Localization.lang(
                    "any field that matches the regular expression ") : Localization.lang("the field "), textSize));
            textList.add(TextUtil.createTextBold(field, textSize));

            if (operator == GrammarBasedSearchRule.ComparisonOperator.CONTAINS) {
                if (regExp) {
                    textList.add(TextUtil.createText(Localization.lang(" contains the regular expression "), textSize));
                    textList.add(TextUtil.createTextBold(value, textSize));
                    return textList;
                }
                textList.add(TextUtil.createText(Localization.lang(" contains the term "), textSize));
                textList.add(TextUtil.createTextBold(value, textSize));
                return textList;
            } else if (operator == GrammarBasedSearchRule.ComparisonOperator.EXACT) {
                if (regExp) {
                    textList.add(TextUtil.createText(Localization.lang(" matches the regular expression "), textSize));
                    textList.add(TextUtil.createTextBold(value, textSize));
                    return textList;
                }
                textList.add(TextUtil.createText(Localization.lang(" matches the term "), textSize));
                textList.add(TextUtil.createTextBold(value, textSize));
                return textList;
            } else if (operator == GrammarBasedSearchRule.ComparisonOperator.DOES_NOT_CONTAIN) {
                if (regExp) {
                    textList.add(TextUtil.createText(Localization.lang(" doesn't contain the regular expression "), textSize));
                    textList.add(TextUtil.createTextBold(value, textSize));
                    return textList;
                }
                textList.add(TextUtil.createText(Localization.lang(" doesn't contain the term "), textSize));
                textList.add(TextUtil.createTextBold(value, textSize));
                return textList;
            } else {
                throw new IllegalStateException("CANNOT HAPPEN!");
            }
        }

    }

}
