package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupDialogViewModelTest {

    private GroupDialogViewModel viewModel;
    private Path temporaryFolder;
    private BibDatabaseContext bibDatabaseContext;
    private final MetaData metaData = mock(MetaData.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final GroupsPreferences groupsPreferences = mock(GroupsPreferences.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final AbstractGroup group = mock(AbstractGroup.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
        bibDatabaseContext = new BibDatabaseContext();

        when(group.getName()).thenReturn("Group");

        when(preferences.getBibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("MockedUser-mockedhost");
        when(preferences.getGroupsPreferences()).thenReturn(groupsPreferences);

        bibDatabaseContext.setMetaData(metaData);

        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferences, group, null, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor(), stateManager);
    }

    @Test
    void validateExistingAbsolutePath() throws IOException {
        Path anAuxFile = temporaryFolder.resolve("auxfile.aux").toAbsolutePath();

        Files.createFile(anAuxFile);
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.texGroupFilePathProperty().setValue(anAuxFile.toString());
        assertTrue(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateNonExistingAbsolutePath() {
        Path notAnAuxFile = temporaryFolder.resolve("notanauxfile.aux").toAbsolutePath();
        viewModel.texGroupFilePathProperty().setValue(notAnAuxFile.toString());
        assertFalse(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateExistingRelativePath() throws IOException {
        Path anAuxFile = Path.of("auxfile.aux");

        // The file needs to exist
        Files.createFile(temporaryFolder.resolve(anAuxFile));
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.texGroupFilePathProperty().setValue(anAuxFile.toString());
        assertTrue(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateNonExistingDirectoryPath() throws IOException {
        Path notADirectory = temporaryFolder.resolve("MyDirectory");
        
        viewModel.directoryGroupFilePathProperty().setValue(notADirectory.toString());
        assertFalse(viewModel.directoryGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateExistingDirectoryPath() throws IOException {
        Path aDirectory = temporaryFolder.resolve("MyDirectory");
        Files.createDirectory(aDirectory);

        viewModel.directoryGroupFilePathProperty().setValue(aDirectory.toString());
        assertTrue(viewModel.directoryGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void hierarchicalContextFromGroup() {
        GroupHierarchyType groupHierarchyType = GroupHierarchyType.INCLUDING;
        when(group.getHierarchicalContext()).thenReturn(groupHierarchyType);
        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferences, group, null, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor(), stateManager);

        assertEquals(groupHierarchyType, viewModel.groupHierarchySelectedProperty().getValue());
    }

    @Test
    void defaultHierarchicalContext() {
        GroupHierarchyType defaultHierarchicalContext = GroupHierarchyType.REFINING;
        when(preferences.getGroupsPreferences().getDefaultHierarchicalContext()).thenReturn(defaultHierarchicalContext);
        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferences, null, null, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor(), stateManager);

        assertEquals(defaultHierarchicalContext, viewModel.groupHierarchySelectedProperty().getValue());
    }
}
