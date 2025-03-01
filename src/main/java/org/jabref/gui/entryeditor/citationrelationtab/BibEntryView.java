package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.EnumSet;

import javafx.scene.Node;
import javafx.scene.control.Label;
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
        Label authors = new Label(entry.getFieldOrAliasLatexFree(StandardField.AUTHOR).orElse(""));
        authors.getStyleClass().add("authors");
        authors.setWrapText(true);
        Label title = new Label(entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse(""));
        title.getStyleClass().add("title");
        title.setWrapText(true);
        Label year = new Label(entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(""));
        year.getStyleClass().add("year");
        Label journal = new Label(entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(""));
        journal.getStyleClass().add("journal");

        VBox entryContainer = new VBox(
                new HBox(10, entryType, title),
                new HBox(5, year, journal),
                authors
        );
        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(summaryText -> {
            TextFlowLimited summary = new TextFlowLimited(new Text(summaryText));
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
}
