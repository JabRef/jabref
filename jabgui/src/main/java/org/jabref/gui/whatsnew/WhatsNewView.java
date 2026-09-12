package org.jabref.gui.whatsnew;

import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import org.jabref.logic.whatsnew.AttributedEntry;
import org.jabref.logic.whatsnew.Contributor;
import org.jabref.logic.whatsnew.News;

import org.jspecify.annotations.NullMarked;

/// The news as a scrollable page: a title per contributor group, the release section and the heading of each
/// entry, then the entry itself with links opening through `openUrl`.
///
/// Styled through the base stylesheet's `h3`, `h4`, `bold` and `text-muted` classes; the jbang script
/// `.jbang/WhatsNewLauncher.java`, which shows this view without the base stylesheet, defines them itself.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class WhatsNewView extends ScrollPane {

    private static final Insets PAGE_PADDING = new Insets(16);
    private static final Insets ENTRY_PADDING = new Insets(0, 0, 4, 16);
    private static final double GROUP_GAP = 12;
    private static final double SECTION_GAP = 10;
    private static final double HEADING_GAP = 4;

    private final VBox page = new VBox(6);
    private final Consumer<String> openUrl;

    public WhatsNewView(News news, Consumer<String> openUrl) {
        this.openUrl = openUrl;
        page.setPadding(PAGE_PADDING);
        news.grouped().forEach(this::addGroup);
        setContent(page);
        setFitToWidth(true);
    }

    private void addGroup(Contributor contributor, List<AttributedEntry> entries) {
        addTitle(News.groupTitle(contributor), GROUP_GAP, "h3", "bold");
        String section = "";
        String heading = "";
        for (AttributedEntry item : entries) {
            if (!item.entry().section().equals(section)) {
                section = item.entry().section();
                heading = "";
                addTitle(section, SECTION_GAP, "h4");
            }
            if (!item.entry().heading().equals(heading)) {
                heading = item.entry().heading();
                addTitle(heading, HEADING_GAP, "text-muted");
            }
            TextFlow entry = InlineMarkdown.render(item.entry().text(), openUrl);
            entry.setPadding(ENTRY_PADDING);
            page.getChildren().add(entry);
        }
    }

    private void addTitle(String text, double gapAbove, String... styleClasses) {
        Label title = new Label(text);
        title.getStyleClass().addAll(styleClasses);
        title.setPadding(new Insets(gapAbove, 0, 0, 0));
        page.getChildren().add(title);
    }
}
