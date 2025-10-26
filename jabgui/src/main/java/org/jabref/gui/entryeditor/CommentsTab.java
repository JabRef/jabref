package org.jabref.gui.entryeditor;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.StateManager;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.fieldeditors.MarkdownEditor;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.LinkedFile;


public class CommentsTab extends FieldsEditorTab {
    public static final String NAME = "Comments";
    private final GuiPreferences preferences;

    private final String defaultOwner;
    private final UserSpecificCommentField userSpecificCommentField;
    private final EntryEditorPreferences entryEditorPreferences;
    private boolean isFieldCurrentlyVisible;
    private boolean shouldShowHideButton;


    public CommentsTab(GuiPreferences preferences,
                       UndoManager undoManager,
                       UndoAction undoAction,
                       RedoAction redoAction,
                       JournalAbbreviationRepository journalAbbreviationRepository,
                       StateManager stateManager,
                       PreviewPanel previewPanel) {
        super(false,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                journalAbbreviationRepository,
                stateManager,
                previewPanel);
        this.defaultOwner = preferences.getOwnerPreferences().getDefaultOwner().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        this.preferences = preferences;
        setText(Localization.lang("Comments"));
        setGraphic(IconTheme.JabRefIcons.COMMENT.getGraphicNode());

        userSpecificCommentField = new UserSpecificCommentField(defaultOwner);
        entryEditorPreferences = preferences.getEntryEditorPreferences();
        shouldShowHideButton = true;
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        SequencedSet<Field> comments = new LinkedHashSet<>();

        // First comes the standard comment field
        comments.add(StandardField.COMMENT);

        // Only show the user-specific comment field if it's enabled and should be visible
        if (entryEditorPreferences.shouldShowUserCommentsFields() && shouldShowHideButton) {
            comments.add(userSpecificCommentField);
        }

        // Show all non-empty comment fields (otherwise, they are completely hidden)
        comments.addAll(entry.getFields().stream()
                             .filter(field -> (field instanceof UserSpecificCommentField && !field.equals(userSpecificCommentField))
                                     || field.getName().toLowerCase().contains("comment"))
                             .sorted(Comparator.comparing(Field::getName))
                             .collect(Collectors.toCollection(LinkedHashSet::new)));

        return comments;
    }

    /**
     * Comment editors: three times size of button
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
    protected void setupPanel(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed) {
        super.setupPanel(bibDatabaseContext, entry, compressed);

        Optional<FieldEditorFX> fieldEditorForUserDefinedComment = editors.entrySet().stream().filter(f -> f.getKey().getName().contains(defaultOwner)).map(Map.Entry::getValue).findFirst();

        for (Map.Entry<Field, FieldEditorFX> fieldEditorEntry : editors.entrySet()) {
            Field field = fieldEditorEntry.getKey();
            MarkdownEditor editor = (MarkdownEditor) fieldEditorEntry.getValue().getNode();

            boolean isStandardBibtexComment = field == StandardField.COMMENT;
            boolean isDefaultOwnerComment = field.equals(userSpecificCommentField);
            boolean shouldBeEnabled = isStandardBibtexComment || isDefaultOwnerComment;
            editor.setEditable(shouldBeEnabled);

            editor.setOnDragOver(event -> {
                if (event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });

            editor.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    if (!files.isEmpty()) {
                        File file = files.get(0);

                        Optional<Path> fileDir = bibDatabaseContext.getFirstExistingFileDir(preferences.getFilePreferences());

                        if (fileDir.isEmpty()) {
                            System.err.println("No file directory found. Cannot copy file.");
                        } else {
                            Path destinationPath = fileDir.get().resolve(file.getName());

                            try {
                                FileUtil.copyFile(file.toPath(), destinationPath, false);

                                List<Path> fileDirectories = bibDatabaseContext.getFileDirectories(preferences.getFilePreferences());
                                ExternalApplicationsPreferences externalAppPrefs = preferences.getExternalApplicationsPreferences();

                                LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destinationPath, fileDirectories, externalAppPrefs);

                                String relativePath = newLinkedFile.getLink();

                                String markdownLink = "![](" + relativePath + ")";
                                editor.insertText(editor.getTextInputControl().getCaretPosition(), markdownLink);
                                success = true;

                            } catch (IOException e) {
                                System.err.println("Error copying file: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        if (entryEditorPreferences.shouldShowUserCommentsFields()) {
            // Show "Hide" button only if user-specific comment field is empty
            if (!entry.hasField(userSpecificCommentField)) {
                if (shouldShowHideButton) {
                    Button hideDefaultOwnerCommentButton = new Button(Localization.lang("Hide user-specific comments field"));
                    hideDefaultOwnerCommentButton.setOnAction(e -> {
                        gridPane.getChildren().removeIf(node ->
                                (node instanceof FieldNameLabel fieldNameLabel && fieldNameLabel.getText().equals(userSpecificCommentField.getName()))
                        );
                        fieldEditorForUserDefinedComment.ifPresent(f -> gridPane.getChildren().remove(f.getNode()));
                        editors.remove(userSpecificCommentField);
                        entry.clearField(userSpecificCommentField);
                        shouldShowHideButton = false;

                        setupPanel(bibDatabaseContext, entry, false);
                    });
                    gridPane.add(hideDefaultOwnerCommentButton, 1, gridPane.getRowCount(), 2, 1);
                    setCompressedRowLayout();
                } else {
                    // Show "Show" button when user comments field is hidden
                    Button showDefaultOwnerCommentButton = new Button(Localization.lang("Show user-specific comments field"));
                    showDefaultOwnerCommentButton.setOnAction(e -> {
                        shouldShowHideButton = true;
                        setupPanel(bibDatabaseContext, entry, false);
                    });
                    gridPane.add(showDefaultOwnerCommentButton, 1, gridPane.getRowCount(), 2, 1);
                    setCompressedRowLayout();
                }
            }
        }
    }
}
