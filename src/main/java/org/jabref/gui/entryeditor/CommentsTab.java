package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.preferences.PreferencesService;

public class CommentsTab extends FieldsEditorTab {
    public static final String NAME = "Comments";

    private final String defaultOwner;
    public CommentsTab(PreferencesService preferences,
                       BibDatabaseContext databaseContext,
                       SuggestionProviders suggestionProviders,
                       UndoManager undoManager,
                       DialogService dialogService,
                       StateManager stateManager,
                       ThemeManager themeManager,
                       IndexingTaskManager indexingTaskManager,
                       TaskExecutor taskExecutor,
                       JournalAbbreviationRepository journalAbbreviationRepository) {
        super(
                false,
                databaseContext,
                suggestionProviders,
                undoManager,
                dialogService,
                preferences,
                stateManager,
                themeManager,
                taskExecutor,
                journalAbbreviationRepository,
                indexingTaskManager
        );
        this.defaultOwner = preferences.getOwnerPreferences().getDefaultOwner();
        setText(Localization.lang("Comments"));
        setGraphic(IconTheme.JabRefIcons.COMMENT.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        SequencedSet<Field> comments = new LinkedHashSet<>();
        comments.add(StandardField.COMMENT);
        comments.addAll(entry.getFields().stream()
                             .filter(field -> field instanceof UserSpecificCommentField ||
                                     field.getName().toLowerCase().contains("comment"))
                             .sorted(Comparator.comparing(Field::getName))
                             .collect(Collectors.toCollection(LinkedHashSet::new)));
        return comments;
    }

    /**
     * Comment editors: thre times size of button
     */
    private void setCompressedRowLayout() {
        int numberOfComments = gridPane.getRowCount() - 1;
        double totalWeight = numberOfComments * 3 + 1;

        RowConstraints commentConstraint = new RowConstraints();
        commentConstraint.setVgrow(Priority.ALWAYS);
        commentConstraint.setValignment(VPos.TOP);
        double commentHeightPercent = 3.0 / totalWeight * 100.0;
        commentConstraint.setPercentHeight(commentHeightPercent);

        RowConstraints buttonConstraint = new RowConstraints();
        buttonConstraint.setVgrow(Priority.ALWAYS);
        buttonConstraint.setValignment(VPos.TOP);
        double addButtonHeightPercent = 1.0 / totalWeight * 100.0;
        buttonConstraint.setPercentHeight(addButtonHeightPercent);

        ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
        rowConstraints.clear();
        for (int i = 1; i <= numberOfComments; i++) {
            rowConstraints.add(commentConstraint);
        }
        rowConstraints.add(buttonConstraint);
    }

    @Override
    protected void setupPanel(BibEntry entry, boolean compressed) {
        super.setupPanel(entry, compressed);

        boolean hasDefaultOwnerField = false;

        for (Map.Entry<Field, FieldEditorFX> fieldEditorEntry : editors.entrySet()) {
            Field field = fieldEditorEntry.getKey();
            FieldEditorFX editor = fieldEditorEntry.getValue();

            boolean isStandardBibtexComment = (field == StandardField.COMMENT);
            boolean isDefaultOwnerComment = field.getName().contains(defaultOwner);
            hasDefaultOwnerField = hasDefaultOwnerField || isDefaultOwnerComment;
            boolean shouldBeEnabled = isStandardBibtexComment || isDefaultOwnerComment;
            editor.getNode().setDisable(!shouldBeEnabled);
        }

        if (!hasDefaultOwnerField) {
            BorderPane container = new BorderPane();
            Button addDefaultOwnerCommentButton = new Button(Localization.lang("Add"));
            addDefaultOwnerCommentButton.setOnAction(e -> {
                ObservableList<Node> children = gridPane.getChildren();
                int rows = gridPane.getRowCount();

                // remove button
                children.removeLast();

                // add comment field for current user
                UserSpecificCommentField userSpecificCommentField = new UserSpecificCommentField(defaultOwner);
                Label label = createLabelAndEditor(entry, userSpecificCommentField);
                Parent node = editors.get(userSpecificCommentField).getNode();
                gridPane.addRow(rows - 1, label, node);
                setCompressedRowLayout(gridPane, rows);
                node.requestFocus();
            });
            // container.setCenter(addDefaultOwnerCommentButton);
            gridPane.add(addDefaultOwnerCommentButton, 1, gridPane.getRowCount(), 2, 1);
            // setRegularRowLayout(gridPane);
            setCompressedRowLayout();
            // gridPane.getRowConstraints().add(new RowConstraints(addDefaultOwnerCommentButton.getMinHeight()));
            // setCompressedRowLayout(gridPane, gridPane.getRowCount());
        }
    }
}
