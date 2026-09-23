package org.jabref.gui.groups;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.event.MetaDataChangeSource;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.undo.CompoundEdit;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// org.jabref.gui.groups.GroupNodeViewModel.refreshGroup is used, which uses "Platform.runlater"
@ExtendWith(JavaFxExtension.class)
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
    void rootGroupIsAllEntriesByDefault() {
        AllEntriesGroup allEntriesGroup = new AllEntriesGroup("All entries");
        assertEquals(new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, allEntriesGroup, new CustomLocalDragboard(), preferences), groupTree.rootGroupProperty().getValue());
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
    void shouldShowDialogWhenCaseSensitivyDiffers() {
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

    @ParameterizedTest
    @MethodSource("initialTreeSetupProvider")
    void addSuggestedGroupsCreatesAllMissingSuggestedGroups(Consumer<GroupTreeNode> initialSetup) {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();

        initialSetup.accept(rootGroup.getGroupNode());
        model.addSuggestedGroups(rootGroup);

        Set<String> expectedGroupNames = Set.of(
                Localization.lang("Entries without linked files"),
                Localization.lang("Entries without groups"),
                Localization.lang("Marking and Grading"));
        Set<String> actualGroupNames = rootGroup.getChildren().stream()
                                                .map(GroupNodeViewModel::getDisplayName)
                                                .collect(Collectors.toSet());
        assertEquals(expectedGroupNames, actualGroupNames);

        GroupNodeViewModel markingGroup = rootGroup.getChildren().stream()
                                                   .filter(child -> Localization.lang("Marking and Grading").equals(child.getDisplayName()))
                                                   .findFirst()
                                                   .orElseThrow();
        assertEquals(6, markingGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }

    private static Stream<Arguments> initialTreeSetupProvider() {
        return Stream.of(
                Arguments.of(Named.of("no groups exist", (Consumer<GroupTreeNode>) root -> {
                })),
                Arguments.of(Named.of("only without files group exists", (Consumer<GroupTreeNode>) root -> root.addSubgroup(GroupsFactory.createWithoutFilesGroup()))),
                Arguments.of(Named.of("only without groups group exists", (Consumer<GroupTreeNode>) root -> root.addSubgroup(GroupsFactory.createWithoutGroupsGroup()))),
                Arguments.of(Named.of("only marking and grading group exists", (Consumer<GroupTreeNode>) root -> root.addChild(GroupsFactory.createMarkingNode(',')))));
    }

    @Test
    void shouldNotAddSuggestedGroupsWhenAllExist() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        rootGroup.getGroupNode().addChild(GroupsFactory.createMarkingNode(','));

        assertEquals(3, rootGroup.getChildren().size());
        GroupNodeViewModel markingGroup = rootGroup.getChildren().get(2);
        assertEquals(6, markingGroup.getChildren().size());

        model.addSuggestedGroups(rootGroup);

        assertEquals(3, rootGroup.getChildren().size());
        assertEquals(6, markingGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
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
    void shouldReportMissingGroupsWhenMarkingGroupIsPartiallyPopulated() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());

        // Create marking and grading node, but remove Rank subgroup to make it partial
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        Optional<GroupTreeNode> rankNodeOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeOpt.isPresent());
        markingNode.removeChild(rankNodeOpt.get());

        rootGroup.getGroupNode().addChild(markingNode);

        // It should return false because Rank subgroup is missing
        assertFalse(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldReportMissingGroupsWhenRankSubgroupIsPartiallyPopulated() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());

        // Create marking and grading node, but remove the "One" subgroup from Rank to make it partial
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        Optional<GroupTreeNode> rankNodeOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeOpt.isPresent());
        GroupTreeNode rankNode = rankNodeOpt.get();
        Optional<GroupTreeNode> oneNodeOpt = rankNode.findGroupByName(Localization.lang("One"));
        assertTrue(oneNodeOpt.isPresent());
        rankNode.removeChild(oneNodeOpt.get());

        rootGroup.getGroupNode().addChild(markingNode);

        // It should return false because "One" subgroup under Rank is missing
        assertFalse(rootGroup.hasAllSuggestedGroups());
    }

    @Test
    void shouldAddMissingGroupsWhenMarkingGroupIsPartiallyPopulated() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());

        // Create marking and grading node, but remove Rank subgroup entirely
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        Optional<GroupTreeNode> rankNodeOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeOpt.isPresent());
        markingNode.removeChild(rankNodeOpt.get());

        rootGroup.getGroupNode().addChild(markingNode);

        // Before adding, it should be false
        assertFalse(rootGroup.hasAllSuggestedGroups());

        // Add suggested groups
        model.addSuggestedGroups(rootGroup);

        // After adding, it should be true
        assertTrue(rootGroup.hasAllSuggestedGroups());

        // Verify Rank parent and subgroups exist under marking node
        Optional<GroupTreeNode> rankNodeAfterOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeAfterOpt.isPresent());
        GroupTreeNode rankNodeAfter = rankNodeAfterOpt.get();
        assertEquals(5, rankNodeAfter.getChildren().size());
    }

    @Test
    void shouldAddMissingSubgroupsWhenRankSubgroupIsPartiallyPopulated() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());

        // Create marking and grading node, but remove the "One" subgroup from Rank
        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        Optional<GroupTreeNode> rankNodeOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeOpt.isPresent());
        GroupTreeNode rankNode = rankNodeOpt.get();
        Optional<GroupTreeNode> oneNodeOpt = rankNode.findGroupByName(Localization.lang("One"));
        assertTrue(oneNodeOpt.isPresent());
        rankNode.removeChild(oneNodeOpt.get());

        rootGroup.getGroupNode().addChild(markingNode);

        // Before adding, it should be false
        assertFalse(rootGroup.hasAllSuggestedGroups());

        // Add suggested groups
        model.addSuggestedGroups(rootGroup);

        // After adding, it should be true
        assertTrue(rootGroup.hasAllSuggestedGroups());

        // Verify Rank parent and all subgroups exist under marking node
        Optional<GroupTreeNode> rankNodeAfterOpt = markingNode.findGroupByName(Localization.lang("Rank"));
        assertTrue(rankNodeAfterOpt.isPresent());
        GroupTreeNode rankNodeAfter = rankNodeAfterOpt.get();
        assertEquals(5, rankNodeAfter.getChildren().size());
        assertTrue(rankNodeAfter.findGroupByName(Localization.lang("One")).isPresent());
    }

    @Test
    void shouldNotAddDuplicateMarkingGroupWhenRenamedOrLocalized() {
        GroupTreeViewModel model = new GroupTreeViewModel(stateManager, mock(BibEntryTypesManager.class), preferences, dialogService, mock(AiService.class), new CustomLocalDragboard(), taskExecutor);
        GroupNodeViewModel rootGroup = model.rootGroupProperty().getValue();
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutFilesGroup());
        rootGroup.getGroupNode().addSubgroup(GroupsFactory.createWithoutGroupsGroup());

        GroupTreeNode markingNode = GroupsFactory.createMarkingNode(',');
        markingNode.getGroup().nameProperty().setValue("Markieren und Bewerten");
        rootGroup.getGroupNode().addChild(markingNode);

        assertTrue(rootGroup.hasAllSuggestedGroups());

        model.addSuggestedGroups(rootGroup);

        assertEquals(3, rootGroup.getChildren().size());
        assertTrue(rootGroup.hasAllSuggestedGroups());
    }
}
