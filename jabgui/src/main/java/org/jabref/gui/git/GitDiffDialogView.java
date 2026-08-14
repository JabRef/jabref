package org.jabref.gui.git;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.git.diff.DiffFiles;
import org.jabref.logic.git.diff.DiffLine;
import org.jabref.logic.git.diff.EntryDiffFiles;
import org.jabref.logic.git.diff.LineDiffFiles;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class GitDiffDialogView extends BaseDialog<Void> {

    @FXML private ListView<DiffFiles> fileList;
    @FXML private Label fileNameLabel;
    @FXML private TableView<DiffLine> tableView;
    @FXML private TableColumn<DiffLine, Number> oldLineNumberColumn;
    @FXML private TableColumn<DiffLine, String> oldLinesColumn;
    @FXML private TableColumn<DiffLine, Number> newLineNumberColumn;
    @FXML private TableColumn<DiffLine, String> newLinesColumn;

    private final List<DiffFiles> diffFiles;

    public GitDiffDialogView(List<DiffFiles> diffFiles) {
        this.diffFiles = diffFiles;
        setTitle(Localization.lang("Git Diff"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        initializeDiffTable();

        fileList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(DiffFiles item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.fileName());
                }
            }
        });
        fileList.setItems(FXCollections.observableArrayList(diffFiles));
        fileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> showFile(newValue));

        if (!diffFiles.isEmpty()) {
            fileList.getSelectionModel().selectFirst();
        }
    }

    private void showFile(DiffFiles diffFile) {
        if (diffFile == null) {
            fileNameLabel.setText("");
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }
        fileNameLabel.setText(diffFile.fileName());
        tableView.setItems(FXCollections.observableArrayList(toDisplayLines(diffFile)));
    }

    private static List<DiffLine> toDisplayLines(DiffFiles diffFile) {
        return switch (diffFile) {
            case LineDiffFiles lineDiffFiles ->
                    lineDiffFiles.lines();
            case EntryDiffFiles entryDiffFiles ->
                    summarize(entryDiffFiles.entryDiffs());
        };
    }

    private static List<DiffLine> summarize(List<BibEntryDiff> entryDiffs) {
        List<DiffLine> summary = new ArrayList<>();
        int row = 0;
        for (BibEntryDiff entryDiff : entryDiffs) {
            BibEntry originalEntry = entryDiff.originalEntry();
            BibEntry newEntry = entryDiff.newEntry();

            if (originalEntry == null) {
                row++;
                summary.add(DiffLine.added(row, describe(newEntry)));
            } else if (newEntry == null) {
                row++;
                summary.add(DiffLine.deleted(row, describe(originalEntry)));
            } else {
                row = appendFieldDiffs(summary, row, originalEntry, newEntry);
            }
        }
        return summary;
    }

    private static int appendFieldDiffs(List<DiffLine> summary, int row, BibEntry originalEntry, BibEntry newEntry) {
        String label = describe(originalEntry);
        boolean anyDifference = false;

        if (!originalEntry.getType().equals(newEntry.getType())) {
            anyDifference = true;
            row++;
            String typeLabel = label + " - entry type";
            summary.add(DiffLine.changed(row, row, typeLabel + ": " + originalEntry.getType().getName(), typeLabel + ": " + newEntry.getType().getName()));
        }

        TreeSet<Field> allFields = new TreeSet<>(Comparator.comparing(Field::getName));
        allFields.addAll(originalEntry.getFields());
        allFields.addAll(newEntry.getFields());

        for (Field field : allFields) {
            Optional<String> oldValue = originalEntry.getField(field);
            Optional<String> newValue = newEntry.getField(field);
            if (oldValue.equals(newValue)) {
                continue;
            }

            anyDifference = true;
            row++;
            String fieldLabel = label + " - " + field.getName();
            if (oldValue.isEmpty()) {
                summary.add(DiffLine.added(row, fieldLabel + ": " + newValue.get()));
            } else if (newValue.isEmpty()) {
                summary.add(DiffLine.deleted(row, fieldLabel + ": " + oldValue.get()));
            } else {
                summary.add(DiffLine.changed(row, row, fieldLabel + ": " + oldValue.get(), fieldLabel + ": " + newValue.get()));
            }
        }

        if (!originalEntry.getUserComments().equals(newEntry.getUserComments())) {
            anyDifference = true;
            row++;
            String commentsLabel = label + " - comments before entry";
            summary.add(DiffLine.changed(row, row, commentsLabel + ": " + originalEntry.getUserComments(), commentsLabel + ": " + newEntry.getUserComments()));
        }

        if (!anyDifference) {
            row++;
            summary.add(DiffLine.context(row, row, label, label));
        }

        return row;
    }

    private static String describe(BibEntry entry) {
        return entry.getCitationKey().orElse("(no citation key)");
    }

    private void initializeDiffTable() {
        oldLineNumberColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().oldLineNumber().orElse(0)));
        oldLinesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().oldLines().orElse("")));
        newLineNumberColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().newLineNumber().orElse(0)));
        newLinesColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().newLines().orElse("")));

        tableView.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(DiffLine item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("diff-row-added", "diff-row-deleted", "diff-row-changed");
                if (empty || item == null) {
                    return;
                }
                switch (item.type()) {
                    case ADDED ->
                            getStyleClass().add("diff-row-added");
                    case DELETED ->
                            getStyleClass().add("diff-row-deleted");
                    case CHANGED ->
                            getStyleClass().add("diff-row-changed");
                    case CONTEXT -> {
                    }
                }
            }
        });
    }
}
