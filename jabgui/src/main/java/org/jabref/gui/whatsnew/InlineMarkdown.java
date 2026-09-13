package org.jabref.gui.whatsnew;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/// Renders the inline markup a changelog entry uses — `**bold**`, `` `code` ``, `<kbd>key</kbd>`, `[label](url)`,
/// `<url>` and bare URLs — into a [TextFlow], styled through the base stylesheet's `bold` and `font-monospace`
/// classes.
///
/// Not [org.jabref.gui.util.component.MarkdownTextFlow]: this class is also compiled into the jbang script
/// `.jbang/WhatsNewLauncher.java`, which runs before jabgui is built and can only take along classes that depend
/// on nothing but JavaFX and jablib.
final class InlineMarkdown {

    private static final String BOLD = "\\*\\*(?<bold>.+?)\\*\\*";
    private static final String CODE = "`(?<code>[^`]+)`";
    private static final String KEY = "<kbd>(?<key>[^<]+)</kbd>";
    private static final String LINK = "\\[(?<label>[^\\]]+)]\\((?<url>https?://[^)]+)\\)";
    private static final String AUTOLINK = "<(?<auto>https?://[^>\\s]+)>";
    /// A bare URL ends before whitespace, a closing bracket and the punctuation a sentence may follow it with.
    private static final String BARE_URL = "(?<bare>https?://[^\\s<>)\\]]*[^\\s<>)\\].,;:!?])";
    private static final Pattern MARKUP = Pattern.compile(String.join("|", BOLD, CODE, KEY, LINK, AUTOLINK, BARE_URL));

    private static final String BOLD_CLASS = "bold";
    private static final String CODE_CLASS = "font-monospace";

    private InlineMarkdown() {
    }

    /// `line` as a flow of text and links, the links opening through `openUrl`.
    static TextFlow render(String line, Consumer<String> openUrl) {
        TextFlow flow = new TextFlow();
        Matcher matcher = MARKUP.matcher(line);
        int plainFrom = 0;
        while (matcher.find()) {
            flow.getChildren().add(new Text(line.substring(plainFrom, matcher.start())));
            flow.getChildren().addAll(markup(matcher, openUrl));
            plainFrom = matcher.end();
        }
        flow.getChildren().add(new Text(line.substring(plainFrom)));
        return flow;
    }

    /// The nodes for one match of [#MARKUP]: bold text may nest code and links, so it is rendered again.
    private static List<Node> markup(Matcher matcher, Consumer<String> openUrl) {
        if (matcher.group("bold") != null) {
            List<Node> bold = List.copyOf(render(matcher.group("bold"), openUrl).getChildren());
            bold.forEach(node -> node.getStyleClass().add(BOLD_CLASS));
            return bold;
        }
        if (matcher.group("code") != null || matcher.group("key") != null) {
            Text code = new Text(matcher.group("code") != null ? matcher.group("code") : matcher.group("key"));
            code.getStyleClass().add(CODE_CLASS);
            return List.of(code);
        }
        if (matcher.group("label") != null) {
            return List.of(link(matcher.group("label"), matcher.group("url"), openUrl));
        }
        String url = matcher.group("auto") != null ? matcher.group("auto") : matcher.group("bare");
        return List.of(link(url, url, openUrl));
    }

    private static Hyperlink link(String label, String url, Consumer<String> openUrl) {
        Hyperlink hyperlink = new Hyperlink(label);
        hyperlink.setPadding(Insets.EMPTY);
        hyperlink.setOnAction(_ -> openUrl.accept(url));
        return hyperlink;
    }
}
