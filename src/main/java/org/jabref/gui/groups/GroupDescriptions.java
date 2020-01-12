package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.strings.StringUtil;

public class GroupDescriptions {

    private GroupDescriptions() {
    }

    public static String getDescriptionForPreview(String field, String expr, boolean caseSensitive, boolean regExp) {
        String header = regExp ? Localization.lang(
                "This group contains entries whose <b>%0</b> field contains the regular expression <b>%1</b>",
                field, expr) : Localization.lang(
                        "This group contains entries whose <b>%0</b> field contains the keyword <b>%1</b>",
                        field, expr);
        String caseSensitiveText = caseSensitive ? Localization.lang("case sensitive") : Localization
                .lang("case insensitive");
        String footer = regExp ? Localization
                .lang("Entries cannot be manually assigned to or removed from this group.") : Localization.lang(
                        "Additionally, entries whose <b>%0</b> field does not contain "
                                + "<b>%1</b> can be assigned manually to this group by selecting them "
                                + "then using either drag and drop or the context menu. "
                                + "This process adds the term <b>%1</b> to "
                                + "each entry's <b>%0</b> field. "
                                + "Entries can be removed manually from this group by selecting them "
                                + "then using the context menu. "
                                + "This process removes the term <b>%1</b> from "
                                + "each entry's <b>%0</b> field.",
                        field, expr);
        return String.format("%s (%s). %s", header, caseSensitiveText, footer);
    }

    public static String getShortDescriptionKeywordGroup(KeywordGroup keywordGroup, boolean showDynamic) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (showDynamic) {
            sb.append("<i>").append(StringUtil.quoteForHTML(keywordGroup.getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(keywordGroup.getName()));
        }
        sb.append("</b> - ");
        sb.append(Localization.lang("dynamic group"));
        sb.append(" <b>");
        sb.append(keywordGroup.getSearchField());
        sb.append("</b> ");
        sb.append(Localization.lang("contains"));
        sb.append(" <b>");
        sb.append(StringUtil.quoteForHTML(keywordGroup.getSearchExpression()));
        sb.append("</b>)");
        switch (keywordGroup.getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();

    }

    public static String getDescriptionForPreview() {
        return Localization.lang("This group contains entries based on manual assignment. "
                + "Entries can be assigned to this group by selecting them "
                + "then using either drag and drop or the context menu. "
                + "Entries can be removed from this group by selecting them "
                + "then using the context menu.");
    }

    public static String getShortDescriptionExplicitGroup(ExplicitGroup explicitGroup) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(explicitGroup.getName()).append("</b> - ").append(Localization.lang("static group"));
        switch (explicitGroup.getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();
    }

    public static String getShortDescriptionAllEntriesGroup() {
        return Localization.lang("<b>All Entries</b> (this group cannot be edited or removed)");
    }

    public static String getShortDescription(SearchGroup searchGroup, boolean showDynamic) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (showDynamic) {
            sb.append("<i>").append(StringUtil.quoteForHTML(searchGroup.getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(searchGroup.getName()));
        }
        sb.append("</b> - ");
        sb.append(Localization.lang("dynamic group"));
        sb.append(" (");
        sb.append(Localization.lang("search expression"));
        sb.append(" <b>").append(StringUtil.quoteForHTML(searchGroup.getSearchExpression())).append("</b>)");
        switch (searchGroup.getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();
    }

    protected static String fromTextFlowToHTMLString(TextFlow textFlow) {
        StringBuilder htmlStringBuilder = new StringBuilder();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                htmlStringBuilder.append(TooltipTextUtil.textToHTMLString((Text) node));
            }
        }
        return htmlStringBuilder.toString();
    }

    protected static String formatRegexException(String regExp, Exception e) {
        String[] sa = e.getMessage().split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sa.length; ++i) {
            if (i > 0) {
                sb.append("<br>");
            }
            sb.append(StringUtil.quoteForHTML(sa[i]));
        }
        String s = Localization.lang(
                "The regular expression <b>%0</b> is invalid:",
                StringUtil.quoteForHTML(regExp))
                + "<p><tt>"
                + sb
                + "</tt>";
        if (!(e instanceof PatternSyntaxException)) {
            return s;
        }
        int lastNewline = s.lastIndexOf("<br>");
        int hat = s.lastIndexOf('^');
        if ((lastNewline >= 0) && (hat >= 0) && (hat > lastNewline)) {
            return s.substring(0, lastNewline + 4) + s.substring(lastNewline + 4).replace(" ", "&nbsp;");
        }
        return s;
    }

    protected static ArrayList<Node> createFormattedDescription(String descriptionHTML) {
        ArrayList<Node> nodes = new ArrayList<>();

        descriptionHTML = descriptionHTML.replaceAll("<p>|<br>", "\n");

        String[] boldSplit = descriptionHTML.split("(?=<b>)|(?<=</b>)|(?=<i>)|(?<=</i>)|(?=<tt>)|(?<=</tt>)|(?=<kbd>)|(?<=</kbd>)");

        for (String bs : boldSplit) {

            if (bs.matches("<b>[^<>]*</b>")) {

                bs = bs.replaceAll("<b>|</b>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-weight: bold");
                nodes.add(textElement);
            } else if (bs.matches("<i>[^<>]*</i>")) {

                bs = bs.replaceAll("<i>|</i>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-style: italic");
                nodes.add(textElement);
            } else if (bs.matches("<tt>[^<>]*</tt>|<kbd>[^<>]*</kbd>")) {

                bs = bs.replaceAll("<tt>|</tt>|<kbd>|</kbd>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-family: 'Courier New', Courier, monospace");
                nodes.add(textElement);
            } else {
                nodes.add(new Text(bs));
            }
        }

        return nodes;
    }

}
