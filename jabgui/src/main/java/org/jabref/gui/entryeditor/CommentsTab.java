package org.jabref.gui.entryeditor;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.jabref.gui.StateManager;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.gui.fieldeditors.MarkdownEditor;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The "Comments" tab: the general comment field (always shown, even when unset) plus all
/// set user-specific comment fields. Other users' comment fields are read-only; the current
/// user's own comment field is offered as a one-click "+" chip while unset (only when
/// user-specific comment fields are enabled).
// [impl->req~entry-editor.comments-tab~1]
@NullMarked
public class CommentsTab extends FieldsEditorTab {

    /// Characters not allowed in the user-specific comment field name.
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    /// The current user's personal comment field (derived from the default-owner preference).
    private final UserSpecificCommentField userSpecificCommentField;
    private final EntryEditorPreferences entryEditorPreferences;

    /// Fields the user added via the chip that are still empty: they are not part of
    /// [BibEntry#getFields()] yet, but must stay visible while this entry is edited.
    private final Set<Field> userAddedFields = new LinkedHashSet<>();
    private @Nullable BibEntry entryOfUserAddedFields;

    /// The entry whose event bus this tab is currently subscribed to (for live refresh
    /// when comment fields are set/unset from outside, e.g. Source tab, undo).
    private Optional<BibEntry> subscribedEntry = Optional.empty();

    public CommentsTab(UndoManager undoManager,
                       UndoAction undoAction,
                       RedoAction redoAction,
                       GuiPreferences preferences,
                       JournalAbbreviationRepository journalAbbreviationRepository,
                       StateManager stateManager,
                       PreviewPanel previewPanel) {
        super(
                false,
                undoManager,
                undoAction,
                redoAction,
                preferences,
                journalAbbreviationRepository,
                stateManager,
                previewPanel
        );

        String defaultOwner = NON_ALPHANUMERIC.matcher(
                preferences.getOwnerPreferences().getDefaultOwner().toLowerCase(Locale.ROOT)).replaceAll("-");
        this.userSpecificCommentField = new UserSpecificCommentField(defaultOwner);
        this.entryEditorPreferences = preferences.getEntryEditorPreferences();

        setText(EntryEditorTabModel.BuiltIn.COMMENTS.displayName());
        setGraphic(IconTheme.JabRefIcons.COMMENT.getGraphicNode());
    }

    /// The general comment always comes first (even when unset), followed by all set
    /// comment fields sorted by name, then the still-empty chip-added own comment field.
    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        if (entry != entryOfUserAddedFields) {
            userAddedFields.clear();
            entryOfUserAddedFields = entry;
        }

        SequencedSet<Field> comments = new LinkedHashSet<>();
        comments.add(StandardField.COMMENT);
        entry.getFields().stream()
             .filter(field -> FieldListSections.sectionOf(field) == FieldListSections.SectionType.COMMENTS)
             .sorted(Comparator.comparing(Field::getName))
             .forEach(comments::add);
        comments.addAll(userAddedFields);
        return comments;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (subscribedEntry.filter(current -> current == entry).isEmpty()) {
            subscribedEntry.ifPresent(previous -> previous.unregisterListener(this));
            entry.registerListener(this);
            subscribedEntry = Optional.of(entry);
        }
        super.bindToEntry(entry);
    }

    /// Refreshes the tab when a comment field is set or unset from outside this tab
    /// (Source tab, undo, …). Rebuilds only when the set of shown comment fields actually
    /// changes, so typing inside a visible editor never rebuilds or steals focus.
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (subscribedEntry.filter(current -> current == event.getBibEntry()).isEmpty()) {
            return;
        }
        Platform.runLater(() -> refreshShownFieldsIfNeeded(event));
    }

    // [impl->req~entry-editor.comments-tab.live-refresh~1]
    private void refreshShownFieldsIfNeeded(FieldChangedEvent event) {
        BibEntry entry = event.getBibEntry();
        if (getCurrentEntry() != entry) {
            return;
        }
        // The user's own comment editor stays visible while this entry is edited, even when its
        // content was just cleared (otherwise deleting the last character would remove the editor
        // mid-edit). Other users' fields are read-only here, so they can only vanish from outside.
        if (userSpecificCommentField.equals(event.getField())
                && editors.containsKey(event.getField())
                && StringUtil.isBlank(event.getNewValue())) {
            userAddedFields.add(userSpecificCommentField);
        }
        if (!determineFieldsToShow(entry).equals(editors.keySet())) {
            setupPanel(activeDatabaseContext(), entry, false);
        }
    }

    @Override
    protected void setupPanel(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed) {
        super.setupPanel(bibDatabaseContext, entry, compressed);

        // Only the general comment and the user's own comment field are editable;
        // other users' comment fields are shown read-only.
        for (Map.Entry<Field, FieldEditorFX> fieldEditorEntry : editors.entrySet()) {
            Field field = fieldEditorEntry.getKey();
            if (fieldEditorEntry.getValue() instanceof MarkdownEditor markdownEditor) {
                boolean isOwnField = (StandardField.COMMENT == field) || field.equals(userSpecificCommentField);
                markdownEditor.setEditable(isOwnField);
            }
        }

        if (entryEditorPreferences.shouldShowUserCommentsFields()
                && !editors.containsKey(userSpecificCommentField)) {
            gridPane.add(createOwnCommentChip(bibDatabaseContext, entry, compressed), 1, gridPane.getRowCount());
            setChipRowLayout();
        }
    }

    /// The "+ comment-<owner>" chip: shows an empty, focused editor for the user's own
    /// comment field, which stays visible — even while still empty — until another entry is opened.
    private Button createOwnCommentChip(BibDatabaseContext bibDatabaseContext, BibEntry entry, boolean compressed) {
        Button chip = new Button(Localization.lang("+ %0", FieldsUtil.getDisplayName(userSpecificCommentField)));
        chip.getStyleClass().add("all-fields-add-chip");
        chip.setOnAction(_ -> {
            userAddedFields.add(userSpecificCommentField);
            setupPanel(bibDatabaseContext, entry, compressed);
            Platform.runLater(() -> {
                // The tab may have been rebound to a different entry before this deferred block
                // runs; focusing then would act on the wrong entry's editors.
                if (getCurrentEntry() == entry) {
                    requestFocus(userSpecificCommentField);
                }
            });
        });
        return chip;
    }

    /// Comment editors share the tab height with weight 3 each; the chip row gets weight 1.
    private void setChipRowLayout() {
        int numberOfComments = gridPane.getRowCount() - 1;
        double totalWeight = numberOfComments * 3 + 1;

        RowConstraints commentConstraint = new RowConstraints();
        commentConstraint.setVgrow(Priority.ALWAYS);
        commentConstraint.setValignment(VPos.TOP);
        commentConstraint.setPercentHeight(3.0 / totalWeight * 100.0);

        RowConstraints chipConstraint = new RowConstraints();
        chipConstraint.setVgrow(Priority.ALWAYS);
        chipConstraint.setValignment(VPos.TOP);
        chipConstraint.setPercentHeight(1.0 / totalWeight * 100.0);

        gridPane.getRowConstraints().clear();
        for (int i = 0; i < numberOfComments; i++) {
            gridPane.getRowConstraints().add(commentConstraint);
        }
        gridPane.getRowConstraints().add(chipConstraint);
    }
}
