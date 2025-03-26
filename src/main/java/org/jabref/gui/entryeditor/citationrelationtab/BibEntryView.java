package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.EnumSet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TextFlowLimited;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * Class to unify the display method of BibEntries in ListViews.
 */
public class BibEntryView {

    public static final EnumSet<StandardEntryType> CROSS_REF_TYPES = EnumSet.of(StandardEntryType.InBook,
            StandardEntryType.InProceedings, StandardEntryType.InCollection);

    /**
     * Creates a layout for a given {@link BibEntry} to be displayed in a List
     *
     * @param entry {@link BibEntry} to display
     * @return layout container displaying the entry
     */
    public static Node getEntryNode(BibEntry entry) {
        Node entryType = getIcon(entry.getType()).getGraphicNode();
        entryType.getStyleClass().add("type");
        String authorsText = entry.getFieldOrAliasLatexFree(StandardField.AUTHOR).orElse("");
        Node authors = createTextNode(authorsText, "authors");
        authors.getStyleClass().add("authors");
        String titleText = entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse("");
        Node title = createTextNode(titleText, "title");
        title.getStyleClass().add("title");
        Label year = new Label(entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(""));
        year.getStyleClass().add("year");
        String journalText = entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse("");
        Node journal = createTextNode(journalText, "journal");
        journal.getStyleClass().add("journal");

        VBox entryContainer = new VBox(
                new HBox(10, entryType, title),
                new HBox(5, year, journal),
                authors
        );

        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(summaryText -> {
            Node summary = createTextNode(summaryText, "summary");
            summary.getStyleClass().add("summary");
            entryContainer.getChildren().add(summary);
        });

        entryContainer.getStyleClass().add("bibEntry");
        return entryContainer;
    }

    /**
     * Gets the correct Icon for a given {@link EntryType}
     *
     * @param type {@link EntryType} to get Icon for
     * @return Icon corresponding to {@link EntryType}
     */
    private static IconTheme.JabRefIcons getIcon(EntryType type) {
        if (type instanceof StandardEntryType standardEntry) {
            if (standardEntry == StandardEntryType.Book) {
                return IconTheme.JabRefIcons.BOOK;
            } else if (CROSS_REF_TYPES.contains(standardEntry)) {
                return IconTheme.JabRefIcons.OPEN_LINK;
            }
        }
        return IconTheme.JabRefIcons.ARTICLE;
    }

    private static boolean isRTL(String text) {
        for (char c : text.toCharArray()) {
            if (Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
                    Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING ||
                    Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
                return true;
            }
        }
        return false;
    }

    private static Node createTextNode(String text, String fieldName) {
        if (isRTL(text)) {
            WebView webView = new WebView();
            webView.getEngine().loadContent(
                    "<html><body dir='rtl'>" + text + "</body></html>"
            );
            webView.setPrefSize(200, 38);
            return webView;
        } else if (fieldName.equals("summary")) {
            return new TextFlowLimited(new Text(text));
        } else {
            Label label = new Label(text);
            label.setWrapText(true);
            return label;
        }
    }
}
