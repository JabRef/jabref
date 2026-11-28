package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.actions.SearchGroupsMigrationAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.exporter.GroupSerializer;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticDateGroup;
import org.jabref.model.groups.AutomaticEntryTypeGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangeSource;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.undo.CompoundEdit;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// org.jabref.gui.groups.GroupNodeViewModel.refreshGroup is used, which uses "Platform.runlater"
@ExtendWith(JavaFxExtension.class)
@ResourceLock("Localization.lang")
class GroupTreeViewModelTest {

    private StateManager stateManager;
    private GroupTreeViewModel groupTree;
    private BibDatabaseContext databaseContext;
    private OptionalObjectProperty<BibDatabaseContext> activeDatabase;
    private TaskExecutor taskExecutor;
    private GuiPreferences preferences;
    private DialogService dialogService;
    private HeadlessGuiUndoManager journal;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();

        stateManager = mock(JabRefGuiStateManager.class);
        activeDatabase = OptionalObjectProperty.empty();
        activeDatabase.setValue(Optional.of(databaseContext));
        when(stateManager.activeDatabaseProperty()).thenReturn(activeDatabase);
        when(stateManager.getSearchContext(databaseContext)).thenReturn(new SearchContext(
                new SimpleBooleanProperty(false),
                NoOpSearchBackend::new,
                NoOpSearchBackend::new));
        when(stateManager.getSelectedGroups(databaseContext)).thenReturn(FXCollections.emptyObservableList());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        journal = new HeadlessGuiUndoManager();
        when(stateManager.getUndoManager(databaseContext)).thenReturn(journal);

        taskExecutor = new CurrentThreadTaskExecutor();
        preferences = mock(GuiPreferences.class);
        dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);

        when(preferences.getLibraryPreferences()).thenReturn(new LibraryPreferences(
                databaseContext.getMode(),
                false,
                false,
                false,
                "Imported entries"
        ));
        when(preferences.getGroupsPreferences()).thenReturn(new GroupsPreferences(
                EnumSet.noneOf(GroupViewMode.class),
                true,
                true,
                false,
                GroupHierarchyType.INDEPENDENT,
                false));
        BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
    }

    @Test
    void addSubgroupFromPreviousRootPersistsInCurrentTree() {
        JavaFxExtension.invokeAndWait(() -> {
            GroupNodeViewModel previousRoot = groupTree.rootGroupProperty().get();
            databaseContext.getDatabase().insertEntry(new BibEntry());
            activeDatabase.setValue(Optional.empty());
            activeDatabase.setValue(Optional.of(databaseContext));

            try (var ignored = mockConstruction(GroupDialogView.class)) {
                when(dialogService.showCustomDialogAndWait(any(GroupDialogView.class)))
                        .thenReturn(Optional.of(new ExplicitGroup("a", GroupHierarchyType.INDEPENDENT, ',')));
                groupTree.addNewSubgroup(previousRoot, GroupDialogHeader.SUBGROUP);
            }
        });

        assertEquals(List.of("a"), childNames());
    }

    @Test
    void rootGroupIsAllEntriesByDefault() {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), preferences), groupTree.rootGroupProperty().getValue());
    }

    private static Stream<Arguments> replacementGroups() throws IOException {
        AuxParser auxParser = mock(AuxParser.class);
        when(auxParser.parse(any())).thenReturn(new AuxParserResult(new BibDatabase()));
        return Stream.of(
                Arguments.of(new WordKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.GROUPS, "2", true, ',', false), Optional.of("2")),
                Arguments.of(new WordKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "biology", true, ',', false), Optional.empty()),
                Arguments.of(new RegexKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "biology", true), Optional.empty()),
                Arguments.of(new SearchGroup("1", GroupHierarchyType.INDEPENDENT, "keywords:biology", EnumSet.noneOf(SearchFlags.class)), Optional.empty()),
                Arguments.of(new AutomaticKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>'), Optional.empty()),
                Arguments.of(new AutomaticPersonsGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.AUTHOR), Optional.empty()),
                Arguments.of(new AutomaticDateGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.DATE), Optional.empty()),
                Arguments.of(new AutomaticEntryTypeGroup("1", GroupHierarchyType.INDEPENDENT), Optional.empty()),
                Arguments.of(TexGroup.create("1", GroupHierarchyType.INDEPENDENT, Path.of("group.aux"), auxParser, new MetaData(), "user"), Optional.empty()));
    }

    // [utest->req~groups.edit.remove-obsolete-explicit-assignments~1]
    @ParameterizedTest
    @MethodSource("replacementGroups")
    void editingExplicitGroupRemovesObsoleteAssignmentWhenAssigning(AbstractGroup replacement, Optional<String> expectedGroups) {
        BibEntry entry = new BibEntry().withField(StandardField.GROUPS, "1")
                                       .withField(StandardField.KEYWORDS, "biology");
        databaseContext.getDatabase().insertEntry(entry);

        editGroup(new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ','), replacement, ButtonType.YES);

        assertEquals(expectedGroups, entry.getField(StandardField.GROUPS));
        assertEquals(Optional.of("biology"), entry.getField(StandardField.KEYWORDS));
    }

    @ParameterizedTest
    @MethodSource("replacementGroups")
    void editingExplicitGroupRemovesObsoleteAssignmentWithoutAssigning(AbstractGroup replacement, Optional<String> expectedGroupsWhenAssigning) {
        BibEntry entry = new BibEntry().withField(StandardField.GROUPS, "1");
        databaseContext.getDatabase().insertEntry(entry);

        editGroup(new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ','), replacement, ButtonType.NO);

        assertEquals(Optional.empty(), entry.getField(StandardField.GROUPS));
        assertEquals(Optional.empty(), entry.getField(StandardField.KEYWORDS));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void editingExplicitGroupPreservesAssignmentForDuplicateName(boolean assign) {
        BibEntry entry = new BibEntry().withField(StandardField.GROUPS, "1");
        databaseContext.getDatabase().insertEntry(entry);
        JavaFxExtension.invokeAndWait(() -> groupTree.rootGroupProperty().getValue().getGroupNode()
                                                     .addSubgroup(new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ',')));
        WordKeywordGroup replacement = new WordKeywordGroup("2", GroupHierarchyType.INDEPENDENT, StandardField.GROUPS, "2", true, ',', false);

        editGroup(new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ','), replacement, assign ? ButtonType.YES : ButtonType.NO);

        assertEquals(Optional.of(assign ? "1, 2" : "1"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void editingExplicitGroupPreservesUnrelatedAssignmentsAndEntries() {
        BibEntry member = new BibEntry().withField(StandardField.GROUPS, "1, other");
        BibEntry nonMember = new BibEntry().withField(StandardField.GROUPS, "other");
        databaseContext.getDatabase().insertEntries(List.of(member, nonMember));

        editGroup(new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ','),
                new WordKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "biology", true, ',', false), ButtonType.YES);

        assertEquals(Optional.of("other"), member.getField(StandardField.GROUPS));
        assertEquals(Optional.of("biology"), member.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("other"), nonMember.getField(StandardField.GROUPS));
        assertEquals(Optional.empty(), nonMember.getField(StandardField.KEYWORDS));
    }

    @Test
    void editingKeywordGroupPreservesBibliographicKeywords() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "biology");
        databaseContext.getDatabase().insertEntry(entry);

        editGroup(new WordKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "biology", true, ',', false),
                new ExplicitGroup("2", GroupHierarchyType.INDEPENDENT, ','), ButtonType.YES);

        assertEquals(Optional.of("biology"), entry.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("2"), entry.getField(StandardField.GROUPS));
    }

    @Test
    void cancellingGroupingMethodChangePreservesExplicitAssignment() {
        BibEntry entry = new BibEntry().withField(StandardField.GROUPS, "1");
        databaseContext.getDatabase().insertEntry(entry);
        ExplicitGroup original = new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ',');

        editGroup(original, new AutomaticEntryTypeGroup("1", GroupHierarchyType.INDEPENDENT), ButtonType.CANCEL);

        assertEquals(Optional.of("1"), entry.getField(StandardField.GROUPS));
        assertEquals(original, databaseContext.getMetaData().getGroups().orElseThrow().getChildren().getFirst().getGroup());
        assertFalse(journal.canUndo());
    }

    @Test
    void groupingMethodChangeUndoesAndRedoesEntryAssignmentsWithGroupDefinition() {
        BibEntry entry = new BibEntry().withField(StandardField.GROUPS, "1");
        databaseContext.getDatabase().insertEntry(entry);
        ExplicitGroup original = new ExplicitGroup("1", GroupHierarchyType.INDEPENDENT, ',');
        WordKeywordGroup replacement = new WordKeywordGroup("1", GroupHierarchyType.INDEPENDENT, StandardField.GROUPS, "2", true, ',', false);
        editGroup(original, replacement, ButtonType.YES);

        JavaFxExtension.invokeAndWait(journal::undo);
        assertEquals(Optional.of("1"), entry.getField(StandardField.GROUPS));
        assertEquals(original, databaseContext.getMetaData().getGroups().orElseThrow().getChildren().getFirst().getGroup());

        JavaFxExtension.invokeAndWait(journal::redo);
        assertEquals(Optional.of("2"), entry.getField(StandardField.GROUPS));
        assertEquals(replacement, databaseContext.getMetaData().getGroups().orElseThrow().getChildren().getFirst().getGroup());
    }

    private void editGroup(AbstractGroup original, AbstractGroup replacement, ButtonType assignmentChoice) {
        JavaFxExtension.invokeAndWait(() -> {
            GroupTreeNode root = groupTree.rootGroupProperty().getValue().getGroupNode();
            GroupTreeNode node = root.addSubgroup(original);
            databaseContext.getMetaData().setGroups(root);
            groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
            when(dialogService.showCustomDialogAndWait(any(GroupDialogView.class))).thenReturn(Optional.of(replacement));
            when(dialogService.showCustomButtonDialogAndWait(any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(assignmentChoice));

            try (MockedConstruction<GroupDialogView> _ = mockConstruction(GroupDialogView.class)) {
                groupTree.editGroup(groupTree.rootGroupProperty().getValue().findGroupNodeViewModel(node).orElseThrow());
            }
        });
    }

    @Test
    void switchingLibraryDetachesThePreviousGroupTreeFromEntryChanges() {
        GroupNodeViewModel previousRoot = groupTree.rootGroupProperty().getValue();
        previousRoot.ensureMatchedEntriesLoaded();
        assertEquals(0, previousRoot.getHits().getValue().intValue());

        BibDatabaseContext newDatabaseContext = new BibDatabaseContext();
        when(stateManager.getSelectedGroups(newDatabaseContext)).thenReturn(FXCollections.emptyObservableList());
        activeDatabase.setValue(Optional.empty());
        activeDatabase.setValue(Optional.of(newDatabaseContext));

        databaseContext.getDatabase().insertEntry(new BibEntry());

        assertEquals(0, previousRoot.getHits().getValue().intValue());
    }

    /// Group operations reach the model by editing the tree in place and writing the root back, so
    /// they are recorded as the tree they produced - one step, undone by installing the tree that
    /// was there before.
    @Test
    // [utest->req~logic.undo.group-operations-recorded~1]
    void reorderingSubgroupsIsUndoable() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        // The view model reads the tree when it is told about the library, and refreshes on later
        // changes only through Platform.runLater - so it is rebuilt here rather than waited on.
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        // Both operations edit the tree the queued refresh reads. Run them on the JavaFX thread, so
        // that the refresh either has finished or has not started - it must not walk a list of
        // children while this thread is reordering it.
        JavaFxExtension.invokeAndWait(() -> groupTree.sortAlphabeticallyRecursive(databaseContext.getMetaData().getGroups().orElseThrow()));
        assertEquals(List.of("A", "B"), childNames());

        assertTrue(journal.canUndo(), "the sort was not recorded");
        JavaFxExtension.invokeAndWait(journal::undo);
        assertEquals(List.of("B", "A"), childNames());
    }

    /// A step that describes only half of what happened is worse than none: the library would hold
    /// a tree the journal cannot take back.
    @Test
    void aFailingOperationStillRecordsWhatItChanged() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        assertThrows(IllegalStateException.class, () -> groupTree.recordTreeChange("failing", () -> {
            databaseContext.getMetaData().getGroups().orElseThrow()
                           .addSubgroup(new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','));
            throw new IllegalStateException("operation failed");
        }));

        assertEquals(List.of("Books"), childNames());
        assertTrue(journal.canUndo(), "what the failed operation changed was not recorded");
        journal.undo();
        assertEquals(List.of(), childNames());
    }

    /// Sorting a group that is already sorted changes nothing, and an undo step that does nothing
    /// makes the next Ctrl+Z look broken.
    @Test
    void anOperationThatChangesNothingIsNotAnUndoStep() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        groupTree.sortAlphabeticallyRecursive(databaseContext.getMetaData().getGroups().orElseThrow());

        assertFalse(journal.canUndo(), "sorting an already sorted group became an undo step");
    }

    /// A group operation writes its tree back and records a step for it, so the library is told
    /// the journal is behind the write. Were it told nothing, the tab would mark the library and
    /// undoing the operation would leave the marker set.
    @Test
    void aRecordedGroupOperationSaysTheJournalIsBehindIt() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<MetaDataChangedEvent> events = new ArrayList<>();
        databaseContext.getMetaData().registerListener(new Object() {
            @Subscribe
            public void listen(MetaDataChangedEvent event) {
                events.add(event);
            }
        });

        groupTree.sortAlphabeticallyRecursive(databaseContext.getMetaData().getGroups().orElseThrow());

        assertEquals(List.of(MetaDataChangeSource.JOURNAL),
                events.stream().map(MetaDataChangedEvent::getSource).distinct().toList());
    }

    @Test
    void removingSubgroupsIsConfirmedWithTheRemoveLabel() {
        GroupNodeViewModel root = rootWithTwoSubgroups();
        when(dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", root.getDisplayName()),
                Localization.lang("Remove")))
                .thenReturn(true);

        JavaFxExtension.invokeAndWait(() -> groupTree.removeSubgroups(root));

        assertEquals(List.of(), childNames());
    }

    @Test
    void cancellingTheRemoveSubgroupsDialogKeepsTheSubgroups() {
        GroupNodeViewModel root = rootWithTwoSubgroups();
        when(dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", root.getDisplayName()),
                Localization.lang("Remove")))
                .thenReturn(false);

        JavaFxExtension.invokeAndWait(() -> groupTree.removeSubgroups(root));

        verify(dialogService).showConfirmationDialogAndWait(
                Localization.lang("Remove subgroups"),
                Localization.lang("Remove all subgroups of \"%0\"?", root.getDisplayName()),
                Localization.lang("Remove"));
        assertEquals(List.of("A", "B"), childNames());
    }

    private GroupNodeViewModel rootWithTwoSubgroups() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        return groupTree.rootGroupProperty().getValue();
    }

    private List<String> childNames() {
        return databaseContext.getMetaData().getGroups().orElseThrow().getChildren().stream()
                              .map(node -> node.getGroup().getName())
                              .toList();
    }

    @Test
    void explicitGroupsAreRemovedFromEntriesOnDelete() {
        ExplicitGroup group = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferences);
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model, new CompoundEdit("test"));

        assertEquals(Optional.empty(), entry.getField(StandardField.GROUPS));
    }

    @Test
    void keywordGroupsAreNotRemovedFromEntriesOnDelete() {
        String groupName = "A";
        WordKeywordGroup group = new WordKeywordGroup(groupName, GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, groupName, true, ',', true);
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupNodeViewModel model = new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, group, new CustomLocalDragboard(), preferences);
        model.addEntriesToGroup(databaseContext.getEntries());
        groupTree.removeGroupsAndSubGroupsFromEntries(model, new CompoundEdit("test"));

        assertEquals(groupName, entry.getField(StandardField.KEYWORDS).get());
    }

    @Test
    void shouldNotShowDialogWhenGroupNameChanges() {
        AbstractGroup oldGroup = new ExplicitGroup("group", GroupHierarchyType.INDEPENDENT, ',');
        AbstractGroup newGroup = new ExplicitGroup("newGroupName", GroupHierarchyType.INDEPENDENT, ',');
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldNotShowDialogWhenGroupsAreEqual() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertTrue(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenKeywordDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", true, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void shouldShowDialogWhenCaseSensitivityDiffers() {
        AbstractGroup oldGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordTest", false, ',', true);
        AbstractGroup newGroup = new WordKeywordGroup("group", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "keywordChanged", true, ',', true);

        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        assertFalse(model.onlyMinorChanges(oldGroup, newGroup));
    }

    @Test
    void rootNodeShouldNotHaveSuggestedGroupsByDefault() {
        GroupNodeViewModel rootGroup = groupTree.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddsAllSuggestedGroupsWhenNoneExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        assertFalse(rootGroup.hasAllSuggestedGroups());

        model.addSuggestedGroups(rootGroup);

        assertEquals(3, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddOnlyMissingGroup() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        assertEquals(1, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(3, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
        // The only search group present is a suggested one, which uses the current syntax
        assertEquals(Optional.of(SearchGroupsMigrationAction.VERSION_6_0_ALPHA_1), databaseContext.getMetaData().getGroupSearchSyntaxVersion());
    }

    /// Migrating search groups of an older syntax also rewrites the suggested groups, which use the current syntax
    @Test
    // [utest->req~ux.groups.suggested-groups-need-current-syntax~1]
    void addsNoSuggestedGroupsWhileSearchGroupsNeedMigration() {
        GroupTreeNode root = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        root.addSubgroup(new SearchGroup("Old syntax", GroupHierarchyType.INDEPENDENT, "title = test", EnumSet.of(SearchFlags.CASE_SENSITIVE)));
        databaseContext.getMetaData().setGroups(root);
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<String> before = new GroupSerializer().serializeTree(root);

        JavaFxExtension.invokeAndWait(() -> groupTree.addSuggestedGroups(groupTree.rootGroupProperty().getValue()));

        assertEquals(before, new GroupSerializer().serializeTree(databaseContext.getMetaData().getGroups().orElseThrow()));
        assertEquals(Optional.empty(), databaseContext.getMetaData().getGroupSearchSyntaxVersion());
        assertFalse(journal.canUndo());
        verify(dialogService).notify(Localization.lang("The search groups of this library use an outdated syntax. Reopen the library and migrate them before adding suggested groups."));
    }

    @Test
    void shouldNotAddSuggestedGroupsWhenAllExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        rootGroup.getGroupNode().addChild(GroupsFactory.createMarkingAndGradingGroups());
        assertEquals(3, rootGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(3, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    /// The name of a suggested group is translated when the group is created, so after switching
    /// the language it no longer matches the name a new suggestion gets.
    @Test
    // [utest->req~ux.groups.suggested-groups-recognized-by-expression~1]
    void shouldNotDuplicateSuggestedGroupsCreatedInAnotherLanguage() {
        GroupTreeNode root = createLibraryWithAllSuggestedGroups();
        root.iterateOverTree()
            .filter(node -> node != root)
            .forEach(node -> node.getGroup().nameProperty().set("Übersetzt: " + node.getGroup().getName()));
        List<String> translatedNames = childNames();
        long groupsBefore = root.iterateOverTree().count();
        GroupNodeViewModel rootGroup = groupTree.rootGroupProperty().getValue();

        assertTrue(rootGroup.hasAllSuggestedGroups());

        // Runs on the JavaFX thread, so that it does not edit the tree while the queued refresh walks it
        JavaFxExtension.invokeAndWait(() -> groupTree.addSuggestedGroups(rootGroup));

        assertEquals(translatedNames, childNames());
        assertEquals(groupsBefore, root.iterateOverTree().count());
    }

    @Test
        // [utest->req~logic.undo.group-operations-recorded~1]
    void addingSuggestedGroupsIsOneUndoStep() {
        databaseContext.getMetaData().setGroups(GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup()));
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        // Both operations edit the tree the queued refresh reads, so they run on the JavaFX thread
        JavaFxExtension.invokeAndWait(() -> groupTree.addSuggestedGroups(groupTree.rootGroupProperty().getValue()));
        assertEquals(List.of(
                        GroupsFactory.createWithoutFilesGroup().getName(),
                        GroupsFactory.createWithoutGroupsGroup().getName(),
                        GroupsFactory.createMarkingAndGradingGroups().getGroup().getName()),
                childNames());
        // The first search groups of a library use the current syntax, so reopening it must not offer to migrate them
        assertEquals(Optional.of(SearchGroupsMigrationAction.VERSION_6_0_ALPHA_1), databaseContext.getMetaData().getGroupSearchSyntaxVersion());

        assertTrue(journal.canUndo(), "adding the suggested groups was not recorded");
        List<String> created = new GroupSerializer().serializeTree(databaseContext.getMetaData().getGroups().orElseThrow());
        var storedSelection = FXCollections.<GroupTreeNode>observableArrayList();
        JavaFxExtension.invokeAndWait(() -> {
            storedSelection.setAll(databaseContext.getMetaData().getGroups().orElseThrow().getChildren().getLast());
            when(stateManager.getSelectedGroups(databaseContext)).thenReturn(storedSelection);
            doAnswer(invocation -> {
                storedSelection.setAll(invocation.<List<GroupTreeNode>>getArgument(1));
                return null;
            }).when(stateManager).setSelectedGroups(any(BibDatabaseContext.class), any());
            journal.undo();
        });
        JavaFxExtension.invokeAndWait(() -> {
        });
        assertEquals(List.of(), childNames());
        assertEquals(List.of(groupTree.rootGroupProperty().getValue().getGroupNode()), storedSelection);
        assertEquals(storedSelection, selectedGroupNodes());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getGroupSearchSyntaxVersion());
        assertFalse(journal.canUndo());
        JavaFxExtension.invokeAndWait(journal::redo);
        assertEquals(created, new GroupSerializer().serializeTree(databaseContext.getMetaData().getGroups().orElseThrow()));
        assertEquals(Optional.of(SearchGroupsMigrationAction.VERSION_6_0_ALPHA_1), databaseContext.getMetaData().getGroupSearchSyntaxVersion());
    }

    /// Whether the whole tree is created or a single group restored, the selection shows what was added.
    @Test
    void selectsRestoredGroupOnly() {
        GroupTreeNode root = createLibraryWithAllSuggestedGroups();
        GroupTreeNode rank = root.getChildren().getLast().getChildren().getFirst();
        AbstractGroup threeStars = rank.getChildren().get(2).getGroup();
        rank.getChildren().get(2).removeFromParent();

        JavaFxExtension.invokeAndWait(() -> groupTree.addSuggestedGroups(groupTree.rootGroupProperty().getValue()));

        assertEquals(List.of(threeStars), groupTree.selectedGroupsProperty().stream().map(group -> group.getGroupNode().getGroup()).toList());
    }

    @Test
    void notificationCountsAllCreatedGroups() {
        databaseContext.getMetaData().setGroups(GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup()));
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);

        JavaFxExtension.invokeAndWait(() -> groupTree.addSuggestedGroups(groupTree.rootGroupProperty().getValue()));

        long createdGroups = databaseContext.getMetaData().getGroups().orElseThrow().iterateOverTree().count() - 1;
        assertEquals(25, createdGroups);
        verify(dialogService).notify(Localization.lang("Created %0 suggested groups.", "25"));
    }

    /// Replaces the library's groups by "All entries" with every suggested group below it
    private GroupTreeNode createLibraryWithAllSuggestedGroups() {
        GroupTreeNode root = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
        root.addSubgroup(GroupsFactory.createWithoutFilesGroup());
        root.addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        root.addChild(GroupsFactory.createMarkingAndGradingGroups());
        databaseContext.getMetaData().setGroups(root);
        // The view model reads the tree when it is told about the library, and refreshes on later
        // changes only through Platform.runLater - so it is rebuilt here rather than waited on.
        groupTree = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        return root;
    }

    @Test
    void shouldNotCreateImportedEntriesGroupWhenEnabled() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<GroupNodeViewModel> groups = model.rootGroupProperty().getValue().getChildren();

        assertEquals(0, groups.size());
    }

    @Test
    void shouldNotCreateImportedEntriesGroupWhenCustomNameIsSet() {
        preferences.getLibraryPreferences().setAddImportedEntries(true);
        preferences.getLibraryPreferences().setAddImportedEntriesGroupName("Review list");

        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        List<GroupNodeViewModel> groups = model.rootGroupProperty().getValue().getChildren();

        assertEquals(0, groups.size());
    }

    @Test
        // [utest->req~ux.groups.library-switch-preserves-selection~1]
    void selectionOfNewDatabaseSurvivesSwitch() {
        BibDatabaseContext secondDatabase = new BibDatabaseContext();
        secondDatabase.setDatabasePath(Path.of("second.bib"));
        GroupTreeNode groupA = setUpLibraryWithGroup(databaseContext, "A");
        GroupTreeNode groupB = setUpLibraryWithGroup(secondDatabase, "B");
        var storedSelectionA = stateManager.getSelectedGroups(databaseContext);

        assertEquals(List.of(groupA), selectedGroupNodes());

        groupTree.rootGroupProperty().addListener(_ -> groupTree.selectedGroupsProperty().clear());

        clearInvocations(stateManager);

        activeDatabase.setValue(Optional.of(secondDatabase));
        assertEquals(List.of(groupB), selectedGroupNodes());
        activeDatabase.setValue(Optional.of(databaseContext));
        assertEquals(List.of(groupA), selectedGroupNodes());
        assertEquals(List.of(groupA), storedSelectionA);

        verify(stateManager, never()).clearSelectedGroups(any(BibDatabaseContext.class));
    }

    /// Storing the same selection again makes the entry table recompute which entries the selected groups match
    @Test
    void switchingLibrariesLeavesUnchangedSelectionAlone() {
        BibDatabaseContext secondDatabase = new BibDatabaseContext();
        secondDatabase.setDatabasePath(Path.of("second.bib"));
        setUpLibraryWithGroup(databaseContext, "A");
        setUpLibraryWithGroup(secondDatabase, "B");
        clearInvocations(stateManager);

        activeDatabase.setValue(Optional.of(secondDatabase));
        activeDatabase.setValue(Optional.of(databaseContext));

        verify(stateManager, never()).setSelectedGroups(any(BibDatabaseContext.class), any());
    }

    @Test
        // [utest->req~ux.groups.shared-update-preserves-selection~1]
    void sharedDatabaseGroupUpdatePreservesSelection() {
        setUpLibraryWithGroup(databaseContext, "A");
        var storedSelection = stateManager.getSelectedGroups(databaseContext);
        GroupTreeNode updatedRoot = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        GroupTreeNode updatedGroup = updatedRoot.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        updatedRoot.addSubgroup(new ExplicitGroup("B", GroupHierarchyType.INDEPENDENT, ','));

        groupTree.rootGroupProperty().addListener(_ -> groupTree.selectedGroupsProperty().clear());
        clearInvocations(stateManager);

        JavaFxExtension.invokeAndWait(() -> databaseContext.getMetaData().setGroups(updatedRoot));
        JavaFxExtension.invokeAndWait(() -> {
        });

        assertSame(updatedGroup, storedSelection.getFirst());
        assertSame(updatedGroup, selectedGroupNodes().getFirst());
        verify(stateManager, never()).clearSelectedGroups(databaseContext);
    }

    /// A change below the selected group gives it a node that no longer equals the old one
    @Test
    // [utest->req~ux.groups.shared-update-preserves-selection~1]
    void sharedDatabaseGroupUpdatePreservesSelectionOfChangedGroup() {
        setUpLibraryWithGroup(databaseContext, "A");
        GroupTreeNode updatedRoot = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        GroupTreeNode updatedGroup = updatedRoot.addSubgroup(new ExplicitGroup("A", GroupHierarchyType.INDEPENDENT, ','));
        updatedGroup.addSubgroup(new ExplicitGroup("A1", GroupHierarchyType.INDEPENDENT, ','));

        groupTree.rootGroupProperty().addListener(_ -> groupTree.selectedGroupsProperty().clear());
        clearInvocations(stateManager);

        JavaFxExtension.invokeAndWait(() -> databaseContext.getMetaData().setGroups(updatedRoot));
        JavaFxExtension.invokeAndWait(() -> {
        });

        assertEquals(List.of(updatedGroup), selectedGroupNodes());
        verify(stateManager, never()).setSelectedGroups(databaseContext, List.of(updatedRoot));
    }

    private GroupTreeNode setUpLibraryWithGroup(BibDatabaseContext library, String name) {
        var root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        GroupTreeNode group = root.addSubgroup(new ExplicitGroup(name, GroupHierarchyType.INDEPENDENT, ','));
        var storedSelection = FXCollections.observableArrayList(group);
        when(stateManager.getSelectedGroups(library)).thenReturn(storedSelection);
        doAnswer(_ -> {
            storedSelection.clear();
            return null;
        }).when(stateManager).clearSelectedGroups(library);
        doAnswer(invocation -> {
            storedSelection.setAll(invocation.<List<GroupTreeNode>>getArgument(1));
            return null;
        }).when(stateManager).setSelectedGroups(eq(library), any());
        // the root is attached last otherwise refresh will overlap with stubbing
        // the second invokeAndWait() waits for the refresh queued by setGroups to finish
        JavaFxExtension.invokeAndWait(() -> library.getMetaData().setGroups(root));
        JavaFxExtension.invokeAndWait(() -> {
        });
        return group;
    }

    private List<GroupTreeNode> selectedGroupNodes() {
        return groupTree.selectedGroupsProperty().stream().map(GroupNodeViewModel::getGroupNode).toList();
    }
}
