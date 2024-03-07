package org.jabref.gui.groups;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.BibEntryPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

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
    private final GroupsPreferences groupsPreferences = mock(GroupsPreferences.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final AbstractGroup group = mock(AbstractGroup.class);
    private final PreferencesService preferencesService = mock(PreferencesService.class);

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
        bibDatabaseContext = new BibDatabaseContext();

        when(group.getName()).thenReturn("Group");

        when(preferencesService.getBibEntryPreferences()).thenReturn(mock(BibEntryPreferences.class));
        when(preferencesService.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferencesService.getFilePreferences().getUserAndHost()).thenReturn("MockedUser-mockedhost");
        when(preferencesService.getGroupsPreferences()).thenReturn(groupsPreferences);

        bibDatabaseContext.setMetaData(metaData);

        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferencesService, group, null, new DummyFileUpdateMonitor());
    }

    @Test
    void validateExistingAbsolutePath() throws Exception {
        var anAuxFile = temporaryFolder.resolve("auxfile.aux").toAbsolutePath();

        Files.createFile(anAuxFile);
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.texGroupFilePathProperty().setValue(anAuxFile.toString());
        assertTrue(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateNonExistingAbsolutePath() {
        var notAnAuxFile = temporaryFolder.resolve("notanauxfile.aux").toAbsolutePath();
        viewModel.texGroupFilePathProperty().setValue(notAnAuxFile.toString());
        assertFalse(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateExistingRelativePath() throws Exception {
        var anAuxFile = Path.of("auxfile.aux");

        // The file needs to exist
        Files.createFile(temporaryFolder.resolve(anAuxFile));
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.texGroupFilePathProperty().setValue(anAuxFile.toString());
        assertTrue(viewModel.texGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateExistingDirectoryAbsolutePath() throws Exception {
        var directory = temporaryFolder.resolve("directory").toAbsolutePath();

        Files.createDirectory(directory);
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.dirGroupFilePathProperty().setValue(directory.toString());
        assertTrue(viewModel.dirGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateNonExistingDirectoryAbsolutePath() {
        var notDirectory = temporaryFolder.resolve("notdirectory").toAbsolutePath();
        viewModel.dirGroupFilePathProperty().setValue(notDirectory.toString());
        assertFalse(viewModel.dirGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateNonExistingDirectoryAsFileAbsolutePath() throws Exception {
        var file = temporaryFolder.resolve("file").toAbsolutePath(); // File without .extension

        Files.createFile(file);
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.dirGroupFilePathProperty().setValue(file.toString());
        assertFalse(viewModel.dirGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void validateExistingDirectoryRelativePath() throws Exception {
        var directory = Path.of("directory");

        // The file needs to exist
        Files.createDirectory(temporaryFolder.resolve(directory));
        when(metaData.getLatexFileDirectory(any(String.class))).thenReturn(Optional.of(temporaryFolder));

        viewModel.dirGroupFilePathProperty().setValue(directory.toString());
        assertTrue(viewModel.dirGroupFilePathValidatonStatus().isValid());
    }

    @Test
    void hierarchicalContextFromGroup() throws Exception {
        GroupHierarchyType groupHierarchyType = GroupHierarchyType.INCLUDING;
        when(group.getHierarchicalContext()).thenReturn(groupHierarchyType);
        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferencesService, group, null, new DummyFileUpdateMonitor());

        assertEquals(groupHierarchyType, viewModel.groupHierarchySelectedProperty().getValue());
    }

    @Test
    void defaultHierarchicalContext() throws Exception {
        GroupHierarchyType defaultHierarchicalContext = GroupHierarchyType.REFINING;
        when(preferencesService.getGroupsPreferences().getDefaultHierarchicalContext()).thenReturn(defaultHierarchicalContext);
        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferencesService, null, null, new DummyFileUpdateMonitor());

        assertEquals(defaultHierarchicalContext, viewModel.groupHierarchySelectedProperty().getValue());
    }
}
