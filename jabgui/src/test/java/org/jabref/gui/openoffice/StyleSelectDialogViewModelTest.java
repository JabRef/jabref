package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class StyleSelectDialogViewModelTest {

    private DialogService dialogService;
    private CSLStyleLoader cslStyleLoader;
    private JStyleLoader jStyleLoader;
    private BstStyleLoader bstStyleLoader;
    private GuiPreferences guiPreferences;
    private OpenOfficePreferences openOfficePreferences;
    private JournalAbbreviationRepository abbreviationRepository;
    private TaskExecutor taskExecutor;
    private BibEntryTypesManager bibEntryTypesManager;

    private StyleSelectDialogViewModel viewModel;

    @TempDir
    private Path styleFolder;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        cslStyleLoader = mock(CSLStyleLoader.class);
        jStyleLoader = mock(JStyleLoader.class);
        bstStyleLoader = mock(BstStyleLoader.class);
        guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        openOfficePreferences = mock(OpenOfficePreferences.class, Answers.RETURNS_DEEP_STUBS);
        abbreviationRepository = mock(JournalAbbreviationRepository.class);
        taskExecutor = mock(TaskExecutor.class);
        bibEntryTypesManager = mock(BibEntryTypesManager.class);

        when(guiPreferences.getOpenOfficePreferences(abbreviationRepository)).thenReturn(openOfficePreferences);
        when(guiPreferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(bstStyleLoader.getStyles()).thenReturn(List.of());

        viewModel = new StyleSelectDialogViewModel(
                dialogService, cslStyleLoader, jStyleLoader, bstStyleLoader,
                guiPreferences, abbreviationRepository, taskExecutor, bibEntryTypesManager
        );
    }

    private Path createBstFile(Path directory, String filename) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(filename);
        Files.writeString(file, "% dummy bst file for tests");
        return file;
    }

    @Test
    void addingDuplicateBstFilenameShowsInfoDialogAndDoesNotAdd() throws IOException {
        Path firstFile = createBstFile(styleFolder.resolve("first"), "mystyle.bst");
        Path secondFile = createBstFile(styleFolder.resolve("second"), "mystyle.bst");

        when(dialogService.showFileOpenDialog(any()))
                .thenReturn(Optional.of(firstFile))
                .thenReturn(Optional.of(secondFile));

        when(bstStyleLoader.getStyles())
                .thenReturn(List.of())
                .thenReturn(List.of(new BstStyle(firstFile)));

        viewModel.addBstStyleFile();
        viewModel.addBstStyleFile();

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("Style already available"),
                Localization.lang("A style with the same filename already exists. If it is a different style, please rename and import.")
        );

        verify(bstStyleLoader, times(1)).addExternalStyle(any());
    }

    @Test
    void addingExternalStyleWithSameNameAsInternalStyleShowsInfoDialogAndDoesNotAdd() throws IOException {
        // have a file name that collides with the internal style
        Path externalFile = createBstFile(styleFolder, "apa.bst");

        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(externalFile));

        when(bstStyleLoader.getStyles())
                .thenReturn(List.of(BstStyle.createInternal(BstStyle.INTERNAL_APA_PATH)));

        viewModel.addBstStyleFile();

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("Style already available"),
                Localization.lang("The selected BST style is already contained in the list.")
        );

        verify(bstStyleLoader, times(0)).addExternalStyle(any());
    }
}
