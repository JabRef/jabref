package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.WindowEvent;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.ai.chat.AiGroupChatWindow;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.tasks.generateembeddingsforseveral.GenerateEmbeddingsForSeveralTaskRequest;
import org.jabref.logic.ai.summarization.tasks.GenerateSummaryTaskRequest;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.query.GroupNameFilterVisitor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableGroupTreeChange;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();
    private @Nullable MetaData observedMetaData;
    private final ListProperty<GroupNodeViewModel> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final AiService aiService;
    private final GuiPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final CustomLocalDragboard localDragboard;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;
    private final ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicate = new SimpleObjectProperty<>();
    private final StringProperty filterText = new SimpleStringProperty();
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCase = (GroupTreeNode v1, GroupTreeNode v2) -> v1
            .getName()
            .compareToIgnoreCase(v2.getName());
    private final Comparator<GroupTreeNode> compAlphabetIgnoreCaseReverse = (GroupTreeNode v1, GroupTreeNode v2) -> v2
            .getName()
            .compareToIgnoreCase(v1.getName());
    private final Comparator<GroupTreeNode> compEntries = (GroupTreeNode v1, GroupTreeNode v2) -> {
        int numChildren1 = v1.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
        int numChildren2 = v2.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
        return Integer.compare(numChildren2, numChildren1);
    };
    private final Comparator<GroupTreeNode> compEntriesReverse = (GroupTreeNode v1, GroupTreeNode v2) -> {
        int numChildren1 = v1.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
        int numChildren2 = v2.getEntriesInGroup(this.currentDatabase.get().getEntries()).size();
        return Integer.compare(numChildren1, numChildren2);
    };
    private Optional<BibDatabaseContext> currentDatabase = Optional.empty();

    public GroupTreeViewModel(@NonNull StateManager stateManager,
                              @NonNull BibEntryTypesManager entryTypesManager,
                              @NonNull GuiPreferences preferences,
                              @NonNull DialogService dialogService,
                              @NonNull AiService aiService,
                              @NonNull CustomLocalDragboard localDragboard,
                              @NonNull TaskExecutor taskExecutor
    ) {
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.fieldPreferences = preferences.getFieldPreferences();
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.localDragboard = localDragboard;
        this.taskExecutor = taskExecutor;

        // Register listener
        EasyBind.subscribe(stateManager.activeDatabaseProperty(), this::onActiveDatabaseChanged);
        EasyBind.subscribe(selectedGroups, this::onSelectedGroupChanged);

        // Set-up bindings
        filterPredicate.bind(EasyBind.map(filterText, text ->
                group -> GroupNameFilterVisitor.matches(group.getDisplayName(), text)
        ));
    }

    private void refresh() {
        onActiveDatabaseChanged(stateManager.activeDatabaseProperty().getValue());
    }

    /// Rebuilds the displayed group tree when the group root was replaced underneath it -
    /// e.g. applied from a shared database (see `DBMSSynchronizer#synchronizeLocalMetaData`).
    /// Local edits only mutate nodes below the existing root, which the view models observe
    /// themselves - rebuilding on those would reset selection and expansion state.
    @Subscribe
    public void listen(GroupUpdatedEvent event) {
        UiTaskExecutor.runInJavaFXThread(() -> {
            if (event.getMetaData() != observedMetaData) {
                // Stale callback: the observed library changed after the event was queued
                return;
            }
            GroupNodeViewModel currentRoot = rootGroup.get();
            // Rebuilding is expensive on large libraries (automatic groups scan every entry), so it
            // only happens when the replacing root differs structurally from the displayed one -
            // e.g. a remote metadata change that did not touch the groups replaces the root
            // with an equal tree and is skipped here
            boolean rootReplaced = (currentRoot == null)
                    || event.getMetaData().getGroups()
                            .filter(root -> (root == currentRoot.getGroupNode()) || root.equals(currentRoot.getGroupNode()))
                            .isEmpty();
            if (rootReplaced) {
                // Event bursts coalesce naturally: after the first rebuild, the displayed root is
                // the current one, and the remaining queued callbacks fall through here
                refresh();
            }
        });
    }

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public ListProperty<GroupNodeViewModel> selectedGroupsProperty() {
        return selectedGroups;
    }

    public ObjectProperty<Predicate<GroupNodeViewModel>> filterPredicateProperty() {
        return filterPredicate;
    }

    public StringProperty filterTextProperty() {
        return filterText;
    }

    /// Gets invoked if the user selects a different group.
    /// We need to notify the [StateManager] about this change so that the main table gets updated.
    private void onSelectedGroupChanged(ObservableList<GroupNodeViewModel> newValue) {
        if (!currentDatabase.equals(stateManager.activeDatabaseProperty().getValue())) {
            // Switch of database occurred -> do nothing
            return;
        }

        currentDatabase.ifPresent(database -> {
            if ((newValue == null) || newValue.isEmpty()) {
                stateManager.clearSelectedGroups(database);
            } else {
                stateManager.setSelectedGroups(database, newValue.stream().map(GroupNodeViewModel::getGroupNode).collect(Collectors.toList()));
            }
        });
    }

    /// Opens "New Group Dialog" and add the resulting group to the root
    public void addNewGroupToRoot() {
        if (currentDatabase.isPresent()) {
            addNewSubgroup(rootGroup.get(), GroupDialogHeader.GROUP);
        } else {
            dialogService.showWarningDialogAndWait(Localization.lang("Cannot create group"), Localization.lang("Cannot create group. Please create a library first."));
        }
    }

    /// Gets invoked if the user changes the active database.
    /// We need to get the new group tree and update the view
    private void onActiveDatabaseChanged(Optional<BibDatabaseContext> newDatabase) {
        currentDatabase = newDatabase;
        MetaData newMetaData = newDatabase.map(BibDatabaseContext::getMetaData).orElse(null);
        if (newMetaData != observedMetaData) {
            if (observedMetaData != null) {
                observedMetaData.unregisterListener(this);
            }
            observedMetaData = newMetaData;
            if (observedMetaData != null) {
                observedMetaData.registerListener(this);
            }
        }
        if (newDatabase.isEmpty()) {
            rootGroup.setValue(null);
            return;
        }

        GroupNodeViewModel newRoot = newDatabase
                .map(BibDatabaseContext::getMetaData)
                .flatMap(MetaData::getGroups)
                .map(root -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, root, localDragboard, preferences))
                .orElse(GroupNodeViewModel.getAllEntriesGroup(newDatabase.get(), stateManager, taskExecutor, localDragboard, preferences));

        rootGroup.setValue(newRoot);
        if (stateManager.getSelectedGroups(newDatabase.get()).isEmpty()) {
            stateManager.setSelectedGroups(newDatabase.get(), List.of(newRoot.getGroupNode()));
        }
        selectedGroups.setAll(
                stateManager.getSelectedGroups(newDatabase.get()).stream()
                            .map(selectedGroup -> new GroupNodeViewModel(newDatabase.get(), stateManager, taskExecutor, selectedGroup, localDragboard, preferences))
                            .toList());
    }

    /// Opens "New Group Dialog" and adds the resulting group as subgroup to the specified group
    public void addNewSubgroup(GroupNodeViewModel parent, GroupDialogHeader groupDialogHeader) {
        currentDatabase.ifPresent(database -> {
            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialogView(
                    database,
                    parent.getGroupNode(),
                    null,
                    groupDialogHeader));

            newGroup.ifPresent(group -> recordTreeChange(Localization.lang("Add group"), _ -> {
                GroupTreeNode newSubgroup = parent.addSubgroup(group);
                // [impl->req~ux.groups.create-explicit-from-selection~1]
                selectedGroups.setAll(new GroupNodeViewModel(database, stateManager, taskExecutor, newSubgroup, localDragboard, preferences));

                // TODO: expand the parent so the new group is visible
                dialogService.notify(Localization.lang("Added group \"%0\".", group.getName()));
            }));
        });
    }

    public void writeGroupChangesToMetaData() {
        currentDatabase.ifPresent(database -> database.getMetaData().setGroups(rootGroup.get().getGroupNode()));
    }

    /// Records entry assignments as one undo step. The tree is untouched, so only the entries'
    /// group fields are recorded — [#recordTreeChange] is for operations that change the tree.
    private void recordEntryChange(String name, Consumer<CompoundEdit> operation) {
        currentDatabase.ifPresent(database -> stateManager.getUndoManager(database).addEdit(name, operation));
    }

    /// Runs a group operation and records it as one undo step, tree and entries together.
    ///
    /// Public because the gesture and the operation do not always belong to the same class: a drag
    /// moves several groups at once, and that is one step, opened here by the view that handles the
    /// drop.
    ///
    /// The tree is recorded as a whole because that is how every operation reaches the model: the
    /// nodes are edited in place and the root is written back afterwards. Operations that also
    /// change entries hand their [org.jabref.model.FieldChange]s to the recorder, so that taking the
    /// group back takes the assignments with it.
    ///
    /// The prior tree is copied before the operation runs — it is the very tree the operation is
    /// about to mutate.
    // [impl->req~logic.undo.group-operations-recorded~1]
    public void recordTreeChange(String name, Consumer<CompoundEdit> operation) {
        currentDatabase.ifPresent(database -> {
            MetaData metaData = database.getMetaData();
            Optional<GroupTreeNode> before = metaData.getGroups().map(GroupTreeNode::copySubtree);
            stateManager.getUndoManager(database).addEdit(name, edit -> {
                try {
                    operation.accept(edit);
                } finally {
                    writeBackAndRecord(metaData, before, edit);
                }
            });
        });
    }

    /// For a gesture handler that has no changes of its own to hand over — see
    /// [#recordTreeChange(String,Consumer)].
    public void recordTreeChange(String name, Runnable operation) {
        recordTreeChange(name, _ -> operation.run());
    }

    /// Writes the view models back to the metadata and records the tree change, if there is one.
    ///
    /// Called also when the operation failed part-way: the tree holds what it managed to change,
    /// and a step that took back only the entry assignments would leave the library in a state
    /// nothing describes. The journal hands over a failed block's changes for the same reason.
    private void writeBackAndRecord(MetaData metaData, Optional<GroupTreeNode> before, CompoundEdit edit) {
        writeGroupChangesToMetaData();
        // Sorting an already sorted group, or dropping one where it already is, changes nothing:
        // recording that would enable Undo over a step that does nothing.
        if (!before.equals(metaData.getGroups())) {
            edit.addEdit(new UndoableGroupTreeChange(metaData, before, metaData.getGroups()));
        }
    }

    private boolean isGroupTypeEqual(AbstractGroup oldGroup, AbstractGroup newGroup) {
        return oldGroup.getClass().equals(newGroup.getClass());
    }

    /// Adds JabRef suggested groups under the "All Entries" parent node.
    /// Assumes the parent is already validated as "All Entries" by the caller.
    ///
    /// @param parent The "All Entries" parent node.
    public void addSuggestedGroups(GroupNodeViewModel parent) {
        currentDatabase.ifPresent(database -> {
            GroupTreeNode rootNode = parent.getGroupNode();
            List<GroupTreeNode> newSuggestedSubgroups = new ArrayList<>();

            // 1. Create "Entries without linked files" group if it doesn't exist
            SearchGroup withoutFilesGroup = GroupsFactory.createWithoutFilesGroup();
            if (!parent.hasSimilarSearchGroup(withoutFilesGroup)) {
                GroupTreeNode subGroup = rootNode.addSubgroup(withoutFilesGroup);
                newSuggestedSubgroups.add(subGroup);
            }

            // 2. Create "Entries without groups" group if it doesn't exist
            SearchGroup withoutGroupsGroup = GroupsFactory.createWithoutGroupsGroup();
            if (!parent.hasSimilarSearchGroup(withoutGroupsGroup)) {
                GroupTreeNode subGroup = rootNode.addSubgroup(withoutGroupsGroup);
                newSuggestedSubgroups.add(subGroup);
            }

            selectedGroups.setAll(newSuggestedSubgroups
                    .stream()
                    .map(newSubGroup -> new GroupNodeViewModel(database, stateManager, taskExecutor, newSubGroup, localDragboard, preferences))
                    .toList());

            writeGroupChangesToMetaData();

            dialogService.notify(Localization.lang("Created %0 suggested groups.", String.valueOf(newSuggestedSubgroups.size())));
        });
    }

    /// Check if it is necessary to show a group modified, reassign entry dialog
    ///
    /// Group name change is handled separately
    ///
    /// @param oldGroup Original Group
    /// @param newGroup Edited group
    /// @return true if just trivial modifications (e.g. color or description) or the relevant group properties are equal, false otherwise
    boolean onlyMinorChanges(AbstractGroup oldGroup, AbstractGroup newGroup) {
        // we need to use getclass here because we have different subclass inheritance e.g. ExplicitGroup is a subclass of WordKeyWordGroup
        if (oldGroup.getClass() == WordKeywordGroup.class) {
            WordKeywordGroup oldWordKeywordGroup = (WordKeywordGroup) oldGroup;
            WordKeywordGroup newWordKeywordGroup = (WordKeywordGroup) newGroup;

            return Objects.equals(oldWordKeywordGroup.getSearchField().getName(), newWordKeywordGroup.getSearchField().getName())
                    && Objects.equals(oldWordKeywordGroup.getSearchExpression(), newWordKeywordGroup.getSearchExpression())
                    && Objects.equals(oldWordKeywordGroup.isCaseSensitive(), newWordKeywordGroup.isCaseSensitive());
        } else if (oldGroup.getClass() == RegexKeywordGroup.class) {
            RegexKeywordGroup oldRegexKeywordGroup = (RegexKeywordGroup) oldGroup;
            RegexKeywordGroup newRegexKeywordGroup = (RegexKeywordGroup) newGroup;

            return Objects.equals(oldRegexKeywordGroup.getSearchField().getName(), newRegexKeywordGroup.getSearchField().getName())
                    && Objects.equals(oldRegexKeywordGroup.getSearchExpression(), newRegexKeywordGroup.getSearchExpression())
                    && Objects.equals(oldRegexKeywordGroup.isCaseSensitive(), newRegexKeywordGroup.isCaseSensitive());
        } else if (oldGroup.getClass() == SearchGroup.class) {
            SearchGroup oldSearchGroup = (SearchGroup) oldGroup;
            SearchGroup newSearchGroup = (SearchGroup) newGroup;

            return Objects.equals(oldSearchGroup.getSearchExpression(), newSearchGroup.getSearchExpression())
                    && Objects.equals(oldSearchGroup.getSearchFlags(), newSearchGroup.getSearchFlags());
        } else if (oldGroup.getClass() == AutomaticKeywordGroup.class) {
            AutomaticKeywordGroup oldAutomaticKeywordGroup = (AutomaticKeywordGroup) oldGroup;
            AutomaticKeywordGroup newAutomaticKeywordGroup = (AutomaticKeywordGroup) newGroup;

            return Objects.equals(oldAutomaticKeywordGroup.getKeywordDelimiter(), newAutomaticKeywordGroup.getKeywordDelimiter())
                    && Objects.equals(oldAutomaticKeywordGroup.getKeywordHierarchicalDelimiter(), newAutomaticKeywordGroup.getKeywordHierarchicalDelimiter())
                    && Objects.equals(oldAutomaticKeywordGroup.getField().getName(), newAutomaticKeywordGroup.getField().getName());
        } else if (oldGroup.getClass() == AutomaticPersonsGroup.class) {
            AutomaticPersonsGroup oldAutomaticPersonsGroup = (AutomaticPersonsGroup) oldGroup;
            AutomaticPersonsGroup newAutomaticPersonsGroup = (AutomaticPersonsGroup) newGroup;

            return Objects.equals(oldAutomaticPersonsGroup.getField().getName(), newAutomaticPersonsGroup.getField().getName());
        } else if (oldGroup.getClass() == TexGroup.class) {
            TexGroup oldTexGroup = (TexGroup) oldGroup;
            TexGroup newTexGroup = (TexGroup) newGroup;
            return Objects.equals(oldTexGroup.getFilePath().toString(), newTexGroup.getFilePath().toString());
        }
        return true;
    }

    /// Opens "Edit Group Dialog" and changes the given group to the edited one.
    public void editGroup(GroupNodeViewModel oldGroup) {
        currentDatabase.ifPresent(database -> {
            Optional<AbstractGroup> newGroup = dialogService.showCustomDialogAndWait(new GroupDialogView(
                    database,
                    oldGroup.getGroupNode().getParent().orElse(null),
                    oldGroup.getGroupNode().getGroup(),
                    GroupDialogHeader.SUBGROUP));

            newGroup.ifPresent(group -> {
                AbstractGroup oldGroupDef = oldGroup.getGroupNode().getGroup();
                String oldGroupName = oldGroupDef.getName();

                boolean groupTypeEqual = isGroupTypeEqual(oldGroupDef, group);
                boolean onlyMinorModifications = groupTypeEqual && onlyMinorChanges(oldGroupDef, group);

                // dialog already warns us about this if the new group is named like another existing group
                // We need to check if only the name changed as this is relevant for the entry's group field
                if (groupTypeEqual && !group.getName().equals(oldGroupName) && onlyMinorModifications) {
                    int groupsWithSameName = 0;
                    Optional<GroupTreeNode> databaseRootGroup = currentDatabase.get().getMetaData().getGroups();
                    if (databaseRootGroup.isPresent()) {
                        // we need to check the old name for duplicates. If the new group name occurs more than once, it won't matter
                        groupsWithSameName = databaseRootGroup.get().findChildrenSatisfying(g -> g.getName().equals(oldGroupName)).size();
                    }
                    // We found more than 2 groups, so we cannot simply remove old assignment
                    boolean removePreviousAssignments = groupsWithSameName < 2;

                    boolean keepPreviousAssignments = true;
                    recordTreeChange(Localization.lang("Modify group"), edit -> {
                        edit.addAll(oldGroup.getGroupNode().setGroup(
                                group,
                                keepPreviousAssignments,
                                removePreviousAssignments,
                                database.getEntries()));

                        dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
                    });
                    // This is ugly, but we have no proper update mechanism in place to propagate the changes, so redraw everything
                    refresh();
                    return;
                }

                if (groupTypeEqual && onlyMinorChanges(oldGroup.getGroupNode().getGroup(), group)) {
                    boolean keepPreviousAssignments = true;
                    boolean removePreviousAssignments = true;
                    recordTreeChange(Localization.lang("Modify group"), edit ->
                            edit.addAll(oldGroup.getGroupNode().setGroup(
                                    group,
                                    keepPreviousAssignments,
                                    removePreviousAssignments,
                                    database.getEntries())));

                    refresh();
                    return;
                }

                // Major modifications

                String content = Localization.lang("Assign the original group's entries to this group?");
                ButtonType keepAssignments = new ButtonType(Localization.lang("Assign"), ButtonBar.ButtonData.YES);
                ButtonType removeAssignments = new ButtonType(Localization.lang("Do not assign"), ButtonBar.ButtonData.NO);
                ButtonType cancel = new ButtonType(Localization.lang("Cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

                if (newGroup.get().getClass() == WordKeywordGroup.class) {
                    content += "\n\n" +
                            Localization.lang("(Note: If original entries lack keywords to qualify for the new group configuration, confirming here will add them)");
                }
                Optional<ButtonType> previousAssignments = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.WARNING,
                        Localization.lang("Change of Grouping Method"),
                        content,
                        keepAssignments,
                        removeAssignments,
                        cancel);
                boolean removePreviousAssignments = (oldGroup.getGroupNode().getGroup() instanceof ExplicitGroup)
                        && (group instanceof ExplicitGroup);

                int groupsWithSameName = 0;
                Optional<GroupTreeNode> databaseRootGroup = currentDatabase.get().getMetaData().getGroups();
                if (databaseRootGroup.isPresent()) {
                    String name = oldGroup.getGroupNode().getGroup().getName();
                    groupsWithSameName = databaseRootGroup.get().findChildrenSatisfying(g -> g.getName().equals(name)).size();
                }
                // okay we found more than 2 groups with the same name
                // If we only found one we can still do it
                if (groupsWithSameName >= 2) {
                    removePreviousAssignments = false;
                }

                if (previousAssignments.isPresent() && (previousAssignments.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)) {
                    return;
                }

                boolean keepPreviousAssignments = previousAssignments.isPresent()
                        && (previousAssignments.get().getButtonData() == ButtonBar.ButtonData.YES);
                boolean removeAssignmentsFromOldGroup = removePreviousAssignments;
                recordTreeChange(Localization.lang("Modify group"), edit -> {
                    if (previousAssignments.isPresent()) {
                        edit.addAll(oldGroup.getGroupNode().setGroup(
                                group,
                                keepPreviousAssignments,
                                removeAssignmentsFromOldGroup,
                                database.getEntries()));
                    }

                    dialogService.notify(Localization.lang("Modified group \"%0\".", group.getName()));
                });
                // This is ugly, but we have no proper update mechanism in place to propagate the changes, so redraw everything
                refresh();
            });
        });
    }

    public void chatWithGroup(GroupNodeViewModel group) {
        assert currentDatabase.isPresent();

        BibDatabaseContext context = currentDatabase.get();
        String groupName = group.getGroupNode().getGroup().getName();

        Optional<AiGroupChatWindow> existingWindow = stateManager.getAiChatWindowForGroup(context, groupName);

        if (existingWindow.isPresent()) {
            BaseDialog.bringToFront(existingWindow.get());
            return;
        }

        AiGroupChatWindow aiChatWindow = new AiGroupChatWindow();
        aiChatWindow.databaseContextProperty().set(context);
        aiChatWindow.groupNodeProperty().set(group);

        aiChatWindow.getDialogPane().getScene().getWindow().addEventHandler(
                WindowEvent.WINDOW_CLOSE_REQUEST,
                _ -> stateManager.removeAiChatWindowForGroup(context, groupName)
        );

        stateManager.setAiChatWindowForGroup(context, groupName, aiChatWindow);

        dialogService.showCustomDialogModal(aiChatWindow);
        BaseDialog.bringToFront(aiChatWindow);
    }

    public void generateEmbeddings(GroupNodeViewModel groupNode) {
        if (!preferences.getAiPreferences().getAiFeaturesEnabled() || !preferences.getAiPreferences().getAutoGenerateEmbeddings()) {
            return;
        }

        assert currentDatabase.isPresent();

        AbstractGroup group = groupNode.getGroupNode().getGroup();

        List<LinkedFile> linkedFiles = currentDatabase
                .get()
                .getDatabase()
                .getEntries()
                .stream()
                .filter(group::isMatch)
                .flatMap(entry -> entry.getFiles().stream())
                .toList();

        aiService.getIngestionTaskAggregator()
                 .start(new GenerateEmbeddingsForSeveralTaskRequest(
                         preferences.getFilePreferences(),
                         aiService.getIngestedDocumentsRepository(),
                         aiService.getEmbeddingsStore(),
                         aiService.getCurrentEmbeddingModel(),
                         aiService.getCurrentDocumentSplitter(),
                         currentDatabase.get(),
                         group.nameProperty(),
                         linkedFiles,
                         taskExecutor
                 ));

        dialogService.notify(Localization.lang("Ingestion started for group \"%0\".", group.getName()));
    }

    public void generateSummaries(GroupNodeViewModel groupNode) {
        if (!preferences.getAiPreferences().getAiFeaturesEnabled() || !preferences.getAiPreferences().getAutoGenerateSummaries()) {
            return;
        }

        assert currentDatabase.isPresent();

        AbstractGroup group = groupNode.getGroupNode().getGroup();

        List<BibEntry> entries = currentDatabase
                .get()
                .getDatabase()
                .getEntries()
                .stream()
                .filter(group::isMatch)
                .toList();

        entries.forEach(entry ->
                aiService.getSummarizationTaskAggregator().start(
                        new GenerateSummaryTaskRequest(
                                preferences.getFilePreferences(),
                                aiService.getCurrentChatModel(),
                                aiService.getCurrentSummarizator(),
                                new FullBibEntry(currentDatabase.get(), entry),
                                false
                        )
                )
        );

        dialogService.notify(Localization.lang("Summarization started for group \"%0\".", group.getName()));
    }

    public void removeSubgroups(GroupNodeViewModel group) {
        boolean confirmation = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", group.getDisplayName()));
        if (confirmation) {
            recordTreeChange(Localization.lang("Remove subgroups"), edit -> {
                for (GroupNodeViewModel child : group.getChildren()) {
                    removeGroupsAndSubGroupsFromEntries(child, edit);
                }
                group.getGroupNode().removeAllChildren();
                dialogService.notify(Localization.lang("Removed all subgroups of group \"%0\".", group.getDisplayName()));
            });
        }
    }

    public void removeGroupKeepSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group"),
                    Localization.lang("Remove group \"%0\" and keep its subgroups?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups"),
                    Localization.lang("Remove all selected groups and keep their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            recordTreeChange(Localization.lang("Remove groups"), _ -> {
                List<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
                selectedGroupNodes.forEach(eachNode -> {
                    GroupTreeNode groupNode = eachNode.getGroupNode();

                    groupNode.getParent()
                             .ifPresent(parent -> groupNode.moveAllChildrenTo(parent, parent.getIndexOfChild(groupNode).get()));
                    groupNode.removeFromParent();
                });

                if (selectedGroupNodes.size() > 1) {
                    dialogService.notify(Localization.lang("Removed all selected groups."));
                } else {
                    dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
                }
            });
        }
    }

    /// Removes the specified group and its subgroups (after asking for confirmation).
    public void removeGroupAndSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group and subgroups"),
                    Localization.lang("Remove group \"%0\" and its subgroups?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups and subgroups"),
                    Localization.lang("Remove all selected groups and their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            recordTreeChange(Localization.lang("Remove groups"), edit -> {
                List<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
                selectedGroupNodes.forEach(eachNode -> {
                    removeGroupsAndSubGroupsFromEntries(eachNode, edit);
                    eachNode.getGroupNode().removeFromParent();
                });

                if (selectedGroupNodes.size() > 1) {
                    dialogService.notify(Localization.lang("Removed all selected groups and their subgroups."));
                } else {
                    dialogService.notify(Localization.lang("Removed group \"%0\" and its subgroups.", group.getDisplayName()));
                }
            });
        }
    }

    /// Removes the specified group (after asking for confirmation).
    public void removeGroupNoSubgroups(GroupNodeViewModel group) {
        boolean confirmed;
        if (selectedGroups.size() <= 1) {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove group"),
                    Localization.lang("Remove group \"%0\"?", group.getDisplayName()),
                    Localization.lang("Remove"));
        } else {
            confirmed = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Remove groups and subgroups"),
                    Localization.lang("Remove all selected groups and their subgroups?"),
                    Localization.lang("Remove all"));
        }

        if (confirmed) {
            recordTreeChange(Localization.lang("Remove groups"), edit -> {
                List<GroupNodeViewModel> selectedGroupNodes = new ArrayList<>(selectedGroups);
                selectedGroupNodes.forEach(eachNode -> {
                    removeGroupsAndSubGroupsFromEntries(eachNode, edit);
                    eachNode.getGroupNode().removeFromParent();
                });

                if (selectedGroupNodes.size() > 1) {
                    dialogService.notify(Localization.lang("Removed all selected groups."));
                } else {
                    dialogService.notify(Localization.lang("Removed group \"%0\".", group.getDisplayName()));
                }
            });
        }
    }

    /// @param edit collects the entry changes, so that undoing the removal puts the assignments back
    void removeGroupsAndSubGroupsFromEntries(GroupNodeViewModel group, CompoundEdit edit) {
        for (GroupNodeViewModel child : group.getChildren()) {
            removeGroupsAndSubGroupsFromEntries(child, edit);
        }

        // only remove explicit groups from the entries, keyword groups should not be deleted
        if (group.getGroupNode().getGroup() instanceof ExplicitGroup) {
            int groupsWithSameName = 0;
            String name = group.getGroupNode().getGroup().getName();
            BibDatabaseContext bibDatabaseContext = currentDatabase.get();
            Optional<GroupTreeNode> rootGroup = bibDatabaseContext.getMetaData().getGroups();
            if (rootGroup.isPresent()) {
                groupsWithSameName = rootGroup.get().findChildrenSatisfying(g -> g.getName().equals(name)).size();
            }
            if (groupsWithSameName < 2) {
                List<BibEntry> entriesInGroup = group.getGroupNode().getEntriesInGroup(bibDatabaseContext.getEntries());
                edit.addAll(group.getGroupNode().removeEntriesFromGroup(entriesInGroup));
            }
        }
    }

    /// TODO: warn before assigning to a group whose membership is written to a field other than
    /// `keywords`, since that edits the entries in a way the user may not expect.
    public void addSelectedEntries(GroupNodeViewModel group) {
        recordEntryChange(StandardActions.GROUP_ENTRIES_ADD.getText(),
                edit -> edit.addAll(group.getGroupNode().addEntriesToGroup(stateManager.getSelectedEntries())));
    }

    /// See [#addSelectedEntries] for the warning this still owes the user.
    public void removeSelectedEntries(GroupNodeViewModel group) {
        recordEntryChange(StandardActions.GROUP_ENTRIES_REMOVE.getText(),
                edit -> edit.addAll(group.getGroupNode().removeEntriesFromGroup(stateManager.getSelectedEntries())));
    }

    public void clearGroup(GroupNodeViewModel group) {
        GroupTreeNode groupNode = group.getGroupNode();
        if (groupNode.getGroup() instanceof ExplicitGroup) {
            boolean confirmation = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("This removes all entries from the group '%0'.", group.getDisplayName()),
                    Localization.lang("Clear group \"%0\"?", group.getDisplayName()),
                    Localization.lang("Clear"));
            if (confirmation) {
                List<BibEntry> entriesInGroup = groupNode.getEntriesInGroup(this.currentDatabase.get().getEntries());
                recordEntryChange(StandardActions.GROUP_ENTRIES_CLEAR.getText(),
                        edit -> edit.addAll(groupNode.removeEntriesFromGroup(entriesInGroup)));
                dialogService.notify(Localization.lang("Cleared group \"%0\".", group.getDisplayName()));
            }
        }
    }

    public void sortAlphabeticallyRecursive(GroupTreeNode group) {
        recordTreeChange(Localization.lang("Sort subgroups"), _ -> group.sortChildren(compAlphabetIgnoreCase, true));
    }

    public void sortReverseAlphabeticallyRecursive(GroupTreeNode group) {
        recordTreeChange(Localization.lang("Sort subgroups"), _ -> group.sortChildren(compAlphabetIgnoreCaseReverse, true));
    }

    public void sortEntriesRecursive(GroupTreeNode group) {
        recordTreeChange(Localization.lang("Sort subgroups"), _ -> group.sortChildren(compEntries, true));
    }

    public void sortReverseEntriesRecursive(GroupTreeNode group) {
        recordTreeChange(Localization.lang("Sort subgroups"), _ -> group.sortChildren(compEntriesReverse, true));
    }
}
