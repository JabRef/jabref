package org.jabref.gui.entryeditor;

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

public class BibEntryView {

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

    private static IconTheme.JabRefIcons getIcon(EntryType type) {
        EnumSet<StandardEntryType> crossRefTypes = EnumSet.of(StandardEntryType.InBook, StandardEntryType.InProceedings, StandardEntryType.InCollection);
        if (type == StandardEntryType.Book) {
            return IconTheme.JabRefIcons.BOOK;
        } else if (crossRefTypes.contains(type)) {
            return IconTheme.JabRefIcons.OPEN_LINK;
        }
        return IconTheme.JabRefIcons.ARTICLE;
    }
}
