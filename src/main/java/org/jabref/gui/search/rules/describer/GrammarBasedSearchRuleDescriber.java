package org.jabref.gui.search.rules.describer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.search.rules.GrammarBasedSearchRule;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.strings.StringUtil;
import org.jabref.search.SearchBaseVisitor;
import org.jabref.search.SearchParser;

import org.antlr.v4.runtime.tree.ParseTree;

public class GrammarBasedSearchRuleDescriber implements SearchDescriber {

    private final EnumSet<SearchFlags> searchFlags;
    private final ParseTree parseTree;

    public GrammarBasedSearchRuleDescriber(EnumSet<SearchFlags> searchFlags, ParseTree parseTree) {
        this.searchFlags = searchFlags;
        this.parseTree = Objects.requireNonNull(parseTree);
    }

    @Override
    public TextFlow getDescription() {
        TextFlow textFlow = new TextFlow();
        DescriptionSearchBaseVisitor descriptionSearchBaseVisitor = new DescriptionSearchBaseVisitor();

        // describe advanced search expression
        textFlow.getChildren().add(TooltipTextUtil.createText(String.format("%s ", Localization.lang("This search contains entries in which")), TooltipTextUtil.TextType.NORMAL));
        textFlow.getChildren().addAll(descriptionSearchBaseVisitor.visit(parseTree));
        textFlow.getChildren().add(TooltipTextUtil.createText(". ", TooltipTextUtil.TextType.NORMAL));
        textFlow.getChildren().add(TooltipTextUtil.createText(searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE) ? Localization
                .lang("The search is case sensitive.") :
                Localization.lang("The search is case insensitive."), TooltipTextUtil.TextType.NORMAL));
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
            textList.add(0, TooltipTextUtil.createText(Localization.lang("not").concat(" "), TooltipTextUtil.TextType.NORMAL));
            return textList;
        }

        @Override
        public List<Text> visitParenExpression(SearchParser.ParenExpressionContext context) {
            ArrayList<Text> textList = new ArrayList<>();
            textList.add(TooltipTextUtil.createText(String.format("%s", context.expression()), TooltipTextUtil.TextType.NORMAL));
            return textList;
        }

        @Override
        public List<Text> visitBinaryExpression(SearchParser.BinaryExpressionContext context) {
            List<Text> textList = visit(context.left);
            if ("AND".equalsIgnoreCase(context.operator.getText())) {
                textList.add(TooltipTextUtil.createText(String.format(" %s ", Localization.lang("and")), TooltipTextUtil.TextType.NORMAL));
            } else {
                textList.add(TooltipTextUtil.createText(String.format(" %s ", Localization.lang("or")), TooltipTextUtil.TextType.NORMAL));
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
                TextFlow description = new ContainsAndRegexBasedSearchRuleDescriber(searchFlags, value).getDescription();
                description.getChildren().forEach(it -> textList.add((Text) it));
                return textList;
            }

            final String field = StringUtil.unquote(fieldDescriptor.get().getText(), '"');
            final GrammarBasedSearchRule.ComparisonOperator operator = GrammarBasedSearchRule.ComparisonOperator.build(context.operator.getText());

            final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
            String temp = regExpFieldSpec ? Localization.lang(
                    "any field that matches the regular expression <b>%0</b>") : Localization.lang("the field <b>%0</b>");

            if (operator == GrammarBasedSearchRule.ComparisonOperator.CONTAINS) {
                if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
                    temp = Localization.lang("%0 contains the regular expression <b>%1</b>", temp);
                } else {
                    temp = Localization.lang("%0 contains the term <b>%1</b>", temp);
                }
            } else if (operator == GrammarBasedSearchRule.ComparisonOperator.EXACT) {
                if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
                    temp = Localization.lang("%0 matches the regular expression <b>%1</b>", temp);
                } else {
                    temp = Localization.lang("%0 matches the term <b>%1</b>", temp);
                }
            } else if (operator == GrammarBasedSearchRule.ComparisonOperator.DOES_NOT_CONTAIN) {
                if (searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION)) {
                    temp = Localization.lang("%0 doesn't contain the regular expression <b>%1</b>", temp);
                } else {
                    temp = Localization.lang("%0 doesn't contain the term <b>%1</b>", temp);
                }
            } else {
                throw new IllegalStateException("CANNOT HAPPEN!");
            }

            List<Text> formattedTexts = TooltipTextUtil.formatToTexts(temp,
                    new TooltipTextUtil.TextReplacement("<b>%0</b>", field, TooltipTextUtil.TextType.BOLD),
                    new TooltipTextUtil.TextReplacement("<b>%1</b>", value, TooltipTextUtil.TextType.BOLD));
            textList.addAll(formattedTexts);
            return textList;
        }
    }
}
