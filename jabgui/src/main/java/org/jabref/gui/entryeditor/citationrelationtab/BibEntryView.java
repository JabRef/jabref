package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.EnumSet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

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
        Node authors = createLabel(authorsText);
        authors.getStyleClass().add("authors");
        String titleText = entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse("");
        Node title = createLabel(titleText);
        title.getStyleClass().add("title");
        Label year = new Label(entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(""));
        year.getStyleClass().add("year");
        String journalText = entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse("");
        Node journal = createLabel(journalText);
        journal.getStyleClass().add("journal");

        VBox entryContainer = new VBox(
                new HBox(10, entryType, title),
                new HBox(5, year, journal),
                authors
        );

        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(summaryText -> {
            Node summary = createSummary(summaryText);
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

    /**
     * Checks if text contains right-to-left characters
     *
     * @param text Text to check
     * @return true if text contains RTL characters
     */
    private static boolean isRTL(String text) {
        for (char c : text.toCharArray()) {
            if (Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a text node for the summary with horizontal scrolling for RTL text,
     * avoiding JavaFX bug related to RTL text wrapping
     *
     * @param text The summary text content
     * @return Node with either:
     * - ScrollPane (for RTL text)
     * - TextFlowLimited (for LTR text)
     */
    private static Node createSummary(String text) {
        if (isRTL(text)) {
            Text textNode = new Text(text);
            ScrollPane scrollPane = new ScrollPane(textNode);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setFitToHeight(true);
            return scrollPane;
        } else {
            return new TextFlowLimited(new Text(text));
        }
    }

    /**
     * Creates a label with horizontal scrolling for RTL text,
     * avoiding JavaFX bug related to RTL text wrapping
     *
     * @param text The label text content
     * @return Node with either:
     * - ScrollPane (for RTL text)
     * - Wrapped Label (for LTR text)
     */
    private static Node createLabel(String text) {
        if (isRTL(text)) {
            Label label = new Label(text);
            ScrollPane scrollPane = new ScrollPane(label);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setFitToHeight(true);
            return scrollPane;
        } else {
            Label label = new Label(text);
            label.setWrapText(true);
            return label;
        }
    }
}
