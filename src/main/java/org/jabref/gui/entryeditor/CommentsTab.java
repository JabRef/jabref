package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.preferences.PreferencesService;

public class CommentsTab extends FieldsEditorTab {
    public static final String NAME = "Comments";

    private final String defaultOwner;
    private final UserSpecificCommentField userSpecificCommentField;

    private final EntryEditorPreferences entryEditorPreferences;
    private Button toggleCommentsButton = new Button("Hide user comments");

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
        this.defaultOwner = preferences.getOwnerPreferences().getDefaultOwner().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        setText(Localization.lang("Comments"));
        setGraphic(IconTheme.JabRefIcons.COMMENT.getGraphicNode());

        userSpecificCommentField = new UserSpecificCommentField(defaultOwner);
        entryEditorPreferences = preferences.getEntryEditorPreferences();
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        SequencedSet<Field> comments = new LinkedHashSet<>();

        // First comes the standard comment field
        comments.add(StandardField.COMMENT);

        // Also show comment field of the current user (if enabled in the preferences)
        if (entry.hasField(userSpecificCommentField) && entryEditorPreferences.shouldShowUserCommentsFields()) {
            comments.add(userSpecificCommentField);
        }

        // Show all non-empty comment fields (otherwise, they are completely hidden)
        comments.addAll(entry.getFields().stream()
                .filter(field -> (field instanceof UserSpecificCommentField
                        && field.getName().toLowerCase().contains("comment") && entryEditorPreferences.shouldShowUserCommentsFields() && !field.equals(userSpecificCommentField)))
                .sorted(Comparator.comparing(Field::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        System.out.println(comments);
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

    @SuppressWarnings("checkstyle:EmptyLineSeparator")
    @Override
    protected void setupPanel(BibEntry entry, boolean compressed) {
        super.setupPanel(entry, compressed);
        toggleCommentsButton.setOnAction(e -> {
            if (toggleCommentsButton.getText().equals("Hide user comments")) {
                entryEditorPreferences.setShowUserCommentsFields(false);
                toggleCommentsButton.setText("Show user comments");
            } else {
                entryEditorPreferences.setShowUserCommentsFields(true);
                toggleCommentsButton.setText("Hide user comments");
            }
            setupPanel(entry, compressed);
        });

        gridPane.add(toggleCommentsButton, 1, gridPane.getRowCount(), 2, 1);
        setCompressedRowLayout();

        // Show "Hide" button only if user-specific comment field is empty. Otherwise, it is a strange UI, because the
        // button would just disappear and no change **in the current** editor would be made
//        if (entryEditorPreferences.shouldShowUserCommentsFields() && !entry.hasField(userSpecificCommentField)) {
//            Button toggleCommentsButton = new Button("Hide user comments");
//            AtomicBoolean commentsVisible = new AtomicBoolean(true); // To keep track of the visibility state
//
//            toggleCommentsButton.setOnAction(e -> {
//                if (commentsVisible.get()) {
//                    gridPane.getChildren().remove(fieldEditorForUserDefinedComment.get().getNode());
//                    toggleCommentsButton.setText("Show user comments");
//                } else {
//                    gridPane.add(fieldEditorForUserDefinedComment.get().getNode(), 1, gridPane.getRowCount(), 2, 1);
//                    toggleCommentsButton.setText("Hide user comments");
//                }
//                commentsVisible.set(!commentsVisible.get()); // Toggle the visibility state
//            });
//
//            gridPane.add(toggleCommentsButton, 1, gridPane.getRowCount(), 2, 1);
//            setCompressedRowLayout();
//        }
        }
    }

