package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiSelectionMenuBuilderTest {

    private MultiSelectionMenuBuilder builder;

    @BeforeEach
    void setUp() {
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);

        DialogService dialogService = mock(DialogService.class);
        LinkedFilesEditorViewModel viewModel = mock(LinkedFilesEditorViewModel.class);
        @SuppressWarnings("unchecked")
        ObservableOptionalValue<BibEntry> bibEntry = (ObservableOptionalValue<BibEntry>) mock(ObservableOptionalValue.class);

        builder = new MultiSelectionMenuBuilder(dialogService, databaseContext, bibEntry, guiPreferences, viewModel);
    }

    @Nested
    class LocalFiles {

        @Test
        void isLocalAndExistsReturnsTrueWhenOfflineFileFound() {
            LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel(Path.of("a.pdf"));
            assertTrue(builder.isLocalAndExists(fileViewModel));
        }

        @Test
        void isLocalAndExistsReturnsFalseWhenOfflineMissing() {
            LinkedFileViewModel fileViewModel = mockOfflineMissingFileViewModel();
            assertFalse(builder.isLocalAndExists(fileViewModel));
        }

        @Test
        void isOnlineReturnsFalseForOfflineFile() {
            assertFalse(builder.isOnline(mockOfflineExistingFileViewModel(Path.of("b.pdf"))));
        }
    }

    @Nested
    class OnlineLinks {

        @Test
        void isOnlineReturnsTrueForOnlineLink() {
            assertTrue(builder.isOnline(mockOnlineLinkViewModel("https://x")));
        }

        @Test
        void hasSourceUrlReturnsTrueWhenPresent() {
            LinkedFileViewModel withSource = mockOnlineLinkViewModel("https://x");
            when(withSource.getFile().getSourceUrl()).thenReturn("https://x");
            when(withSource.getFile().sourceUrlProperty()).thenReturn(new SimpleStringProperty("https://x"));
            assertTrue(builder.hasSourceUrl(withSource));
        }

        @Test
        void hasSourceUrlReturnsFalseWhenEmpty() {
            LinkedFileViewModel withoutSource = mockOnlineLinkViewModel("");
            when(withoutSource.getFile().getSourceUrl()).thenReturn("");
            when(withoutSource.getFile().sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));
            assertFalse(builder.hasSourceUrl(withoutSource));
        }
    }

    @Nested
    class MoveToDefaultDir {

        @Test
        void isMovableToDefaultDirReturnsTrueWhenLocalExistingAndGeneratedPathDiffers() {
            Path path = Path.of("c.pdf");
            LinkedFileViewModel movable = mockOfflineExistingFileViewModel(path);
            when(movable.isGeneratedPathSameAsOriginal()).thenReturn(false);
            assertTrue(builder.isMovableToDefaultDir(movable));
        }

        @Test
        void isMovableToDefaultDirReturnsFalseWhenPathsSame() {
            Path path = Path.of("c.pdf");
            LinkedFileViewModel samePath = mockOfflineExistingFileViewModel(path);
            when(samePath.isGeneratedPathSameAsOriginal()).thenReturn(true);
            assertFalse(builder.isMovableToDefaultDir(samePath));
        }

        @Test
        void isMovableToDefaultDirReturnsFalseForOnlineLink() {
            assertFalse(builder.isMovableToDefaultDir(mockOnlineLinkViewModel("https://x")));
        }
    }

    private LinkedFileViewModel mockOfflineExistingFileViewModel(Path path) {
        LinkedFile linkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(linkedFile.isOnlineLink()).thenReturn(false);
        when(linkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.of(path));
        when(linkedFile.linkProperty()).thenReturn(new SimpleStringProperty(path.getFileName().toString()));
        when(linkedFile.getSourceUrl()).thenReturn("");
        when(linkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel viewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(viewModel.getFile()).thenReturn(linkedFile);
        when(viewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return viewModel;
    }

    private LinkedFileViewModel mockOfflineMissingFileViewModel() {
        LinkedFile linkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(linkedFile.isOnlineLink()).thenReturn(false);
        when(linkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.empty());
        when(linkedFile.linkProperty()).thenReturn(new SimpleStringProperty("missing.pdf"));
        when(linkedFile.getSourceUrl()).thenReturn("");
        when(linkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel viewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(viewModel.getFile()).thenReturn(linkedFile);
        when(viewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return viewModel;
    }

    private LinkedFileViewModel mockOnlineLinkViewModel(String sourceUrl) {
        String url = (sourceUrl == null) ? "" : sourceUrl;

        LinkedFile linkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(linkedFile.isOnlineLink()).thenReturn(true);
        when(linkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.empty());
        when(linkedFile.linkProperty()).thenReturn(new SimpleStringProperty("https://host/file.pdf"));
        when(linkedFile.getSourceUrl()).thenReturn(url);
        when(linkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(url));

        LinkedFileViewModel viewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(viewModel.getFile()).thenReturn(linkedFile);
        when(viewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return viewModel;
    }
}
