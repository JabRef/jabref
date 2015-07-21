package net.sf.jabref.search;

import antlr.collections.AST;
import net.sf.jabref.Globals;
import net.sf.jabref.util.StringUtil;

import java.util.regex.Pattern;

public class SearchExpressionDescriber {

    private final boolean caseSensitive;
    private final boolean regExp;
    private final String expr;
    private final AST ast;

    public SearchExpressionDescriber(boolean caseSensitive, boolean regExp, String expr, AST ast) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.expr = expr;
        this.ast = ast;
    }

    public String getDescriptionForPreview() {
        StringBuilder sb = new StringBuilder();
        if (ast == null) {
            sb.append(regExp ? Globals.lang(
                    "This group contains entries in which any field contains the regular expression <b>%0</b>",
                    StringUtil.quoteForHTML(expr))
                    : Globals.lang(
                    "This group contains entries in which any field contains the term <b>%0</b>",
                    StringUtil.quoteForHTML(expr)));
            sb.append(" (").append(caseSensitive ? Globals.lang("case sensitive")
                    : Globals.lang("case insensitive")).append("). ");
            sb.append(Globals.lang(
                    "Entries cannot be manually assigned to or removed from this group."));
            sb.append("<p><br>").append(Globals.lang(
                    "Hint%c To search specific fields only, enter for example%c<p><tt>author%esmith and title%eelectrical</tt>"));
            return sb.toString();
        }
        // describe advanced search expression
        sb.append(Globals.lang("This group contains entries in which")).append(' ');
        sb.append(describeNode(ast, false, false, false));
        sb.append(". ");
        sb.append(caseSensitive ? Globals.lang("The search is case sensitive.")
                : Globals.lang("The search is case insensitive."));
        return sb.toString();
    }

    private String describeNode(AST node, boolean not, boolean and, boolean or) {
        StringBuilder sb = new StringBuilder();
        switch (node.getType()) {
            case SearchExpressionTreeParserTokenTypes.And:
                if (not) {
                    sb.append(Globals.lang("not")).append(' ');
                }
                // if there was an "or" in this subtree so far, braces may be needed
                if (or || not) {
                    sb.append('(');
                }
                sb.append(describeNode(node.getFirstChild(),
                        false, true, false)).append(' ').append(Globals.lang("and")).append(' ').append(describeNode(node.getFirstChild()
                        .getNextSibling(), false, true, false));
                if (or || not) {
                    sb.append(')');
                }
                return sb.toString();
            case SearchExpressionTreeParserTokenTypes.Or:
                if (not) {
                    sb.append(Globals.lang("not")).append(' ');
                }
                // if there was an "and" in this subtree so far, braces may be
                // needed
                if (and || not) {
                    sb.append('(');
                }
                sb.append(describeNode(node.getFirstChild(),
                        false, false, true)).append(' ').append(Globals.lang("or")).append(' ').append(describeNode(node.getFirstChild()
                        .getNextSibling(), false, false, true));
                if (and || not) {
                    sb.append(')');
                }
                return sb.toString();
            case SearchExpressionTreeParserTokenTypes.Not:
                return describeNode(node.getFirstChild(), !not,
                        and, or);
            default:
                node = node.getFirstChild();
                final String field = node.getText();
                final boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
                node = node.getNextSibling();
                final int type = node.getType();
                node = node.getNextSibling();
                final String termQuoted = StringUtil.quoteForHTML(node.getText());
                final String fieldSpecQuoted = regExpFieldSpec ? Globals.lang(
                        "any field that matches the regular expression <b>%0</b>",
                        StringUtil.quoteForHTML(field)) : Globals.lang("the field <b>%0</b>",
                        StringUtil.quoteForHTML(field));
                switch (type) {
                    case SearchExpressionTreeParserTokenTypes.LITERAL_contains:
                    case SearchExpressionTreeParserTokenTypes.EQUAL:
                        if (regExp) {
                            return not ? Globals.lang(
                                    "%0 doesn't contain the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted)
                                    : Globals.lang(
                                    "%0 contains the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
                        }
                        return not ? Globals.lang(
                                "%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                                termQuoted) : Globals.lang("%0 contains the term <b>%1</b>",
                                fieldSpecQuoted, termQuoted);
                    case SearchExpressionTreeParserTokenTypes.LITERAL_matches:
                    case SearchExpressionTreeParserTokenTypes.EEQUAL:
                        if (regExp) {
                            return not ? Globals.lang(
                                    "%0 doesn't match the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted)
                                    : Globals.lang(
                                    "%0 matches the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
                        }
                        return not ? Globals.lang(
                                "%0 doesn't match the term <b>%1</b>",
                                fieldSpecQuoted, termQuoted)
                                : Globals.lang("%0 matches the term <b>%1</b>",
                                fieldSpecQuoted,
                                termQuoted);
                    case SearchExpressionTreeParserTokenTypes.NEQUAL:
                        if (regExp) {
                            return not ? Globals.lang(
                                    "%0 contains the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted)
                                    : Globals.lang(
                                    "%0 doesn't contain the Regular Expression <b>%1</b>",
                                    fieldSpecQuoted, termQuoted);
                        }
                        return not ? Globals.lang("%0 contains the term <b>%1</b>",
                                fieldSpecQuoted, termQuoted) : Globals.lang(
                                "%0 doesn't contain the term <b>%1</b>", fieldSpecQuoted,
                                termQuoted);
                    default:
                        return "Internal error: Unknown AST node type. "
                                + "Please report this on www.sf.net/projects/jabref";
                    // this should never happen
                }
        }
    }
}
