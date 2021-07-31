package org.jabref.gui.groups;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) throws Exception {
        this.temporaryFolder = temporaryFolder;
        bibDatabaseContext = new BibDatabaseContext();
        DialogService dialogService = mock(DialogService.class);

        AbstractGroup group = mock(AbstractGroup.class);
        when(group.getName()).thenReturn("Group");

        PreferencesService preferencesService = mock(PreferencesService.class);
        when(preferencesService.getKeywordDelimiter()).thenReturn(',');
        when(preferencesService.getUser()).thenReturn("MockedUser");

        bibDatabaseContext.setMetaData(metaData);

        viewModel = new GroupDialogViewModel(dialogService, bibDatabaseContext, preferencesService, group, GroupDialogHeader.SUBGROUP);
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
    void validateNonExistingAbsolutePath() throws Exception {
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
}
