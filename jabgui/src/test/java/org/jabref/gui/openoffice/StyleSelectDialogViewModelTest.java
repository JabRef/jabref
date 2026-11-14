package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Tab;
import javafx.stage.FileChooser.ExtensionFilter;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@Isolated("CSLStyleLoader shares its external styles across instances")
@ResourceLock("Localization.lang")
class StyleSelectDialogViewModelTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final JStyleLoader jStyleLoader = mock(JStyleLoader.class);
    private final OpenOfficePreferences openOfficePreferences = OpenOfficePreferences.getDefault();

    @Test
    void storeJStyleSelectionInCurrentStylePreference() {
        JStyle currentStyle = mock(JStyle.class);
        JStyle selectedStyle = mock(JStyle.class);
        when(currentStyle.getPath()).thenReturn("current.jstyle");
        when(selectedStyle.getPath()).thenReturn("selected.jstyle");
        when(jStyleLoader.getStyles()).thenReturn(List.of(currentStyle, selectedStyle));
        StyleSelectDialogViewModel viewModel = viewModel(currentStyle);
        viewModel.setSelectedTab(new Tab("JStyles"));
        viewModel.selectedJStyleProperty().set(viewModel.jStylesProperty().getLast());

        viewModel.storeStylePreferences();

        assertEquals(selectedStyle, openOfficePreferences.getCurrentStyle());
        assertEquals(List.of("current.jstyle", "selected.jstyle"), openOfficePreferences.getExternalJStyles());
    }

    @Test
    void addingJStyleSelectsItAndStoresCurrentStylePreference(@TempDir Path directory) {
        JStyle currentStyle = mock(JStyle.class);
        when(currentStyle.getPath()).thenReturn("current.jstyle");
        when(jStyleLoader.getStyles()).thenReturn(List.of(currentStyle));
        StyleSelectDialogViewModel viewModel = viewModel(currentStyle);
        Path selectedPath = Path.of("added.jstyle");
        Path stylePath = selectedPath.toAbsolutePath();
        JStyle addedStyle = mock(JStyle.class);
        when(addedStyle.getPath()).thenReturn(stylePath.toString());
        when(filePreferences.getWorkingDirectory()).thenReturn(directory);
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(selectedPath));
        when(jStyleLoader.addStyleIfValid(stylePath)).thenReturn(true);
        when(jStyleLoader.getStyles()).thenReturn(List.of(currentStyle, addedStyle));

        viewModel.addJStyleFile();

        verify(jStyleLoader).addStyleIfValid(stylePath);
        assertEquals(addedStyle, viewModel.selectedJStyleProperty().get().getJStyle());
        assertEquals(List.of(currentStyle, addedStyle), viewModel.jStylesProperty().stream().map(JStyleSelectViewModel::getJStyle).toList());
        assertEquals(currentStyle, openOfficePreferences.getCurrentStyle());

        viewModel.setSelectedTab(new Tab("JStyles"));
        viewModel.storeStylePreferences();

        assertEquals(addedStyle, openOfficePreferences.getCurrentStyle());
    }

    @Test
    void matchStyleSearchIgnoresPunctuationSeparators() {
        assertTrue(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "springer "));
    }

    @Test
    void matchStyleSearchFindsCapitalizedSubsetTerms() {
        assertTrue(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "Springer lecture"));
    }

    @Test
    void matchStyleSearchKeepsUnmatchedTermsHidden() {
        assertFalse(StyleSelectDialogViewModel.matchStyleSearch(
                "Springer - Lecture Notes in Computer Science",
                "springer medicine"));
    }

    @Nested
    @NullMarked
    class StyleFileLifecycleTest {

        private final List<CitationStyle> previousExternalStyles = CSLStyleLoader.getStyles().stream()
                                                                                 .filter(style -> !style.isInternalStyle())
                                                                                 .toList();
        private final CSLStyleLoader cslStyleLoader = spy(new CSLStyleLoader(openOfficePreferences));
        private final JournalAbbreviationRepository journalAbbreviationRepository = mock(JournalAbbreviationRepository.class);
        private final JStyleLoader realJStyleLoader = spy(new JStyleLoader(openOfficePreferences,
                LayoutFormatterPreferences.getDefault(), journalAbbreviationRepository));
        private final BstStyleLoader bstStyleLoader = spy(new BstStyleLoader(openOfficePreferences));
        private final BibEntryTypesManager bibEntryTypesManager = mock(BibEntryTypesManager.class);

        @TempDir
        private Path directory;

        private StyleSelectDialogViewModel viewModel;

        @BeforeEach
        void setUp() {
            when(filePreferences.getWorkingDirectory()).thenReturn(directory);
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.empty());
            openOfficePreferences.setCurrentStyle(bstStyleLoader.getStyles().getLast());
            viewModel = createViewModel(cslStyleLoader);
            viewModel.selectedCslLayoutProperty().set(viewModel.getAvailableCslLayouts().getLast());
            viewModel.selectedJStyleProperty().set(viewModel.jStylesProperty().getLast());
            viewModel.setSelectedTab(new Tab("BST Styles"));
        }

        @AfterEach
        void restoreExternalCslStyles() {
            CSLStyleLoader restoringLoader = new CSLStyleLoader(OpenOfficePreferences.getDefault());
            previousExternalStyles.forEach(restoringLoader::addExternalStyle);
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void cancelledImportPreservesStylesSelectionsAndPreferencesWithFamilyFileFilter(StandardFileType fileType) {
            ImportState before = state();

            importStyle(fileType);

            assertUnchanged(before);
            ArgumentCaptor<FileDialogConfiguration> configurationCaptor = ArgumentCaptor.forClass(FileDialogConfiguration.class);
            verify(dialogService).showFileOpenDialog(configurationCaptor.capture());
            FileDialogConfiguration configuration = configurationCaptor.getValue();
            assertEquals(Optional.of(directory), configuration.getInitialDirectory());
            assertEquals(1, configuration.getExtensionFilters().size());
            ExtensionFilter filter = configuration.getExtensionFilters().getFirst();
            assertEquals(Localization.lang("%0 file", fileType.getName()), filter.getDescription());
            assertEquals(fileType.getExtensionsWithAsteriskAndDot(), filter.getExtensions());
            assertEquals(filter.getDescription(), configuration.getDefaultExtension().getDescription());
            assertEquals(filter.getExtensions(), configuration.getDefaultExtension().getExtensions());
            verifyNoMoreInteractions(dialogService);
        }

        @Test
        void externalCslIsRegisteredSelectedAndReloadedFromLoaderPreferences() throws IOException {
            Path path = writeCslStyle("imported.csl", "External import regression style", "Import regression");
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(path));
            JStyle jStyleSelection = viewModel.selectedJStyleProperty().get().getJStyle();
            BstStyle bstStyleSelection = viewModel.selectedBstStyleProperty().get().getBstStyle();

            viewModel.addCslStyleFile();

            CitationStyle imported = viewModel.selectedCslLayoutProperty().get().citationStyle();
            assertEquals(path.toString(), imported.getPath());
            assertFalse(imported.isInternalStyle());
            assertEquals(imported, CSLStyleLoader.getStyles().getLast());
            assertEquals(imported, viewModel.getAvailableCslLayouts().getLast().citationStyle());
            assertEquals(List.of(path.toString()), openOfficePreferences.getExternalCslStyles());
            assertEquals(imported, openOfficePreferences.getCurrentStyle());
            assertEquals(jStyleSelection, viewModel.selectedJStyleProperty().get().getJStyle());
            assertEquals(bstStyleSelection, viewModel.selectedBstStyleProperty().get().getBstStyle());
            viewModel.setSelectedTab(new Tab("CSL Styles"));
            assertEquals(imported, viewModel.getSelectedStyle());
            viewModel.storeStylePreferences();

            StyleSelectDialogViewModel reopened = createViewModel(new CSLStyleLoader(openOfficePreferences));

            assertEquals(imported, CSLStyleLoader.getStyles().getLast());
            assertEquals(imported, reopened.selectedCslLayoutProperty().get().citationStyle());
            assertEquals(List.of(path.toString()), openOfficePreferences.getExternalCslStyles());
            verify(dialogService, never()).showErrorDialogAndWait(anyString(), anyString());
        }

        @Test
        void externalJStyleSelectsAbsolutePathAndDefersCurrentStyleUntilPreferencesAreStored() throws IOException {
            Path path = writeJStyle();
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(path));
            OOStyle currentStyle = openOfficePreferences.getCurrentStyle();
            CitationStyle cslSelection = viewModel.selectedCslLayoutProperty().get().citationStyle();
            BstStyle bstSelection = viewModel.selectedBstStyleProperty().get().getBstStyle();

            viewModel.addJStyleFile();

            JStyle imported = viewModel.selectedJStyleProperty().get().getJStyle();
            assertEquals(path.toString(), imported.getPath());
            assertFalse(imported.isInternalStyle());
            assertEquals(imported, realJStyleLoader.getStyles().getLast());
            assertEquals(List.of(path.toString()), openOfficePreferences.getExternalJStyles());
            assertEquals(currentStyle, openOfficePreferences.getCurrentStyle());
            assertEquals(cslSelection, viewModel.selectedCslLayoutProperty().get().citationStyle());
            assertEquals(bstSelection, viewModel.selectedBstStyleProperty().get().getBstStyle());
            viewModel.setSelectedTab(new Tab("JStyles"));
            assertEquals(imported, viewModel.getSelectedStyle());
            viewModel.storeStylePreferences();
            assertEquals(imported, openOfficePreferences.getCurrentStyle());
            verify(dialogService, never()).showErrorDialogAndWait(anyString(), anyString());
        }

        @Test
        void externalBstSelectsAbsolutePathAndStoresCurrentStyleImmediately() throws IOException {
            Path path = Files.writeString(directory.resolve("imported.BST"), "READ");
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(path));
            CitationStyle cslSelection = viewModel.selectedCslLayoutProperty().get().citationStyle();
            JStyle jStyleSelection = viewModel.selectedJStyleProperty().get().getJStyle();

            viewModel.addBstStyleFile();

            BstStyle imported = viewModel.selectedBstStyleProperty().get().getBstStyle();
            assertEquals(path.toString(), imported.getPath());
            assertFalse(imported.isInternalStyle());
            assertEquals(imported, bstStyleLoader.getStyles().getLast());
            assertEquals(List.of(path.toString()), openOfficePreferences.getExternalBstStyles());
            assertEquals(imported, openOfficePreferences.getCurrentStyle());
            assertEquals(cslSelection, viewModel.selectedCslLayoutProperty().get().citationStyle());
            assertEquals(jStyleSelection, viewModel.selectedJStyleProperty().get().getJStyle());
            assertEquals(imported, viewModel.getSelectedStyle());
            viewModel.storeStylePreferences();
            assertEquals(imported, openOfficePreferences.getCurrentStyle());
            verify(dialogService, never()).showErrorDialogAndWait(anyString(), anyString());
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void invalidStyleDoesNotMutateOrFallBackToAnotherStyle(StandardFileType fileType) throws IOException {
            Path path = Files.writeString(directory.resolve("invalid." + fileType.getExtensions().getFirst()), "}");
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(path));
            ImportState before = state();

            importStyle(fileType);

            assertUnchanged(before);
            verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
        }

        @ParameterizedTest
        @CsvSource({
                "CITATION_STYLE, ieee.csl",
                "JSTYLE, default_authoryear.jstyle",
                "BST, IEEEtran.bst"
        })
        void missingExternalFileDoesNotFallBackToBundledStyle(StandardFileType fileType, String filename) {
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(directory.resolve(filename)));
            ImportState before = state();

            importStyle(fileType);

            assertUnchanged(before);
            verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
        }

        @ParameterizedTest
        @CsvSource({
                "External import regression style, Different short title",
                "Different external title, Import regression"
        })
        void duplicateCslTitleOrShortTitleDoesNotChangeSelection(String title, String shortTitle) throws IOException {
            CitationStylePreviewLayout previousSelection = viewModel.selectedCslLayoutProperty().get();
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(
                    writeCslStyle("original.csl", "External import regression style", "Import regression")));
            viewModel.addCslStyleFile();
            viewModel.selectedCslLayoutProperty().set(previousSelection);
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(writeCslStyle("duplicate.csl", title, shortTitle)));
            ImportState before = state();

            viewModel.addCslStyleFile();

            assertUnchanged(before);
            verify(dialogService).showInformationDialogAndWait(Localization.lang("Style already available"),
                    Localization.lang("The selected CSL style is already available in the list."));
        }

        @Test
        void duplicateJStylePathDoesNotChangeSelection() throws IOException {
            JStyleSelectViewModel previousSelection = viewModel.selectedJStyleProperty().get();
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(writeJStyle()));
            viewModel.addJStyleFile();
            viewModel.selectedJStyleProperty().set(viewModel.jStylesProperty().stream()
                                                            .filter(style -> style.getJStyle().equals(previousSelection.getJStyle()))
                                                            .findFirst().orElseThrow());
            ImportState before = state();

            viewModel.addJStyleFile();

            assertUnchanged(before);
            verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
        }

        @Test
        void duplicateBstFilenameDoesNotChangeSelection() throws IOException {
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(
                    Files.writeString(directory.resolve("IEEEtran.bst"), "READ")));
            ImportState before = state();

            viewModel.addBstStyleFile();

            assertUnchanged(before);
            verify(dialogService).showErrorDialogAndWait(Localization.lang("Style already available"),
                    Localization.lang("A style with the same filename already exists. If it is a different style, please rename and import."));
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void cancelledRemovalPreservesRegistryRowsSelectionsPreferencesAndFile(StandardFileType fileType) throws IOException {
            OOStyle style = addExternalStyle(fileType);
            openOfficePreferences.setCurrentStyle(style);
            ImportState before = state();
            String source = Files.readString(Path.of(style.getPath()));
            clearInvocations(dialogService, cslStyleLoader, realJStyleLoader, bstStyleLoader);

            deleteStyle(style);

            assertUnchanged(before);
            verifyConfirmation(style);
            verifyRemovalNotAttempted(style);
            assertEquals(source, Files.readString(Path.of(style.getPath())));
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void bundledStyleCannotBeRemovedProgrammatically(StandardFileType fileType) {
            OOStyle style = styles(fileType).getFirst();
            selectStyle(style);
            openOfficePreferences.setCurrentStyle(style);
            ImportState before = state();

            deleteStyle(style);

            assertUnchanged(before);
            verifyRemovalNotAttempted(style);
            verifyNoInteractions(dialogService);
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void refusedRemovalPreservesRegistryRowsSelectionsPreferencesAndFile(StandardFileType fileType) throws IOException {
            OOStyle style = addExternalStyle(fileType);
            openOfficePreferences.setCurrentStyle(style);
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
            switch (style) {
                case CitationStyle cslStyle ->
                        doReturn(false).when(cslStyleLoader).removeStyle(cslStyle);
                case JStyle jStyle ->
                        doReturn(false).when(realJStyleLoader).removeStyle(jStyle);
                case BstStyle bstStyle ->
                        doReturn(false).when(bstStyleLoader).removeStyle(bstStyle);
            }
            ImportState before = state();
            String source = Files.readString(Path.of(style.getPath()));

            deleteStyle(style);

            assertUnchanged(before);
            verifyConfirmation(style);
            verify(dialogService).showErrorDialogAndWait(Localization.lang("Cannot delete style"),
                    Localization.lang("Could not delete style. It might be an internal style that cannot be removed."));
            assertEquals(source, Files.readString(Path.of(style.getPath())));
        }

        @ParameterizedTest
        @CsvSource({
                "CITATION_STYLE, true, true",
                "CITATION_STYLE, true, false",
                "CITATION_STYLE, false, true",
                "CITATION_STYLE, false, false",
                "JSTYLE, true, true",
                "JSTYLE, true, false",
                "JSTYLE, false, true",
                "JSTYLE, false, false",
                "BST, true, true",
                "BST, true, false",
                "BST, false, true",
                "BST, false, false"
        })
        void successfulRemovalRepairsOnlyAffectedSelectionAndCurrentPreference(StandardFileType fileType,
                                                                               boolean selected, boolean applied) throws IOException {
            OOStyle previousSelection = selectedStyle(fileType).orElseThrow();
            OOStyle style = addExternalStyle(fileType);
            if (!selected) {
                selectStyle(previousSelection);
            }
            OOStyle unrelatedStyle = fileType == StandardFileType.CITATION_STYLE
                                     ? bstStyleLoader.getStyles().getLast() : CSLStyleLoader.getDefaultStyle();
            openOfficePreferences.setCurrentStyle(applied ? style : unrelatedStyle);
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
            List<? extends OOStyle> remaining = styles(fileType).stream().filter(existing -> !existing.equals(style)).toList();
            OOStyle fallback = remaining.getFirst();
            ImportState before = state();
            String source = Files.readString(Path.of(style.getPath()));

            deleteStyle(style);

            assertEquals(remaining, styles(fileType));
            assertEquals(Optional.of(selected ? fallback : previousSelection), selectedStyle(fileType));
            assertEquals(applied ? fallback : unrelatedStyle, openOfficePreferences.getCurrentStyle());
            ImportState after = state();
            assertEquals(before.cslStyles().stream().filter(existing -> !existing.equals(style)).toList(), after.cslStyles());
            assertEquals(before.jStyles().stream().filter(existing -> !existing.equals(style)).toList(), after.jStyles());
            assertEquals(before.bstStyles().stream().filter(existing -> !existing.equals(style)).toList(), after.bstStyles());
            assertEquals(before.selections().stream().map(existing -> existing.equals(style) ? fallback : existing).toList(), after.selections());
            assertEquals(List.of(), openOfficePreferences.getExternalCslStyles());
            assertEquals(List.of(), openOfficePreferences.getExternalJStyles());
            assertEquals(List.of(), openOfficePreferences.getExternalBstStyles());
            assertUnchanged(after);
            verifyConfirmation(style);
            assertEquals(source, Files.readString(Path.of(style.getPath())));

            StyleSelectDialogViewModel reopened = createViewModel(new CSLStyleLoader(openOfficePreferences),
                    new JStyleLoader(openOfficePreferences, LayoutFormatterPreferences.getDefault(), journalAbbreviationRepository),
                    new BstStyleLoader(openOfficePreferences));

            assertEquals(after.cslStyles(), reopened.getAvailableCslLayouts().stream().map(CitationStylePreviewLayout::citationStyle).toList());
            assertEquals(after.jStyles(), reopened.jStylesProperty().stream().map(JStyleSelectViewModel::getJStyle).toList());
            assertEquals(after.bstStyles(), reopened.bstStylesProperty().stream().map(BstStyleSelectViewModel::getBstStyle).toList());
            assertEquals(applied ? fallback : unrelatedStyle, openOfficePreferences.getCurrentStyle());
        }

        @Test
        void removingJStyleRepairsAppliedPathEvenIfItsParsedContentDiffers() throws IOException {
            JStyle style = (JStyle) addExternalStyle(StandardFileType.JSTYLE);
            Path path = Path.of(style.getPath());
            String source = Files.readString(path).replace("External import regression style", "Updated applied style");
            Files.writeString(path, source);
            JStyle appliedStyle = new JStyle(path, LayoutFormatterPreferences.getDefault(), journalAbbreviationRepository);
            openOfficePreferences.setCurrentStyle(appliedStyle);
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            viewModel.deleteJStyle(style);

            assertEquals(realJStyleLoader.getStyles().getFirst(), openOfficePreferences.getCurrentStyle());
            assertEquals(List.of(), openOfficePreferences.getExternalJStyles());
            assertEquals(source, Files.readString(path));
        }

        @ParameterizedTest
        @EnumSource(value = StandardFileType.class, names = {"CITATION_STYLE", "JSTYLE", "BST"})
        void removingAnAlreadyRemovedStyleDoesNotChangeStateOrPromptAgain(StandardFileType fileType) throws IOException {
            OOStyle style = addExternalStyle(fileType);
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
            deleteStyle(style);
            ImportState before = state();
            String source = Files.readString(Path.of(style.getPath()));
            clearInvocations(dialogService, cslStyleLoader, realJStyleLoader, bstStyleLoader);

            deleteStyle(style);

            assertUnchanged(before);
            verifyRemovalNotAttempted(style);
            verifyNoInteractions(dialogService);
            assertEquals(source, Files.readString(Path.of(style.getPath())));
        }

        @ParameterizedTest
        @CsvSource({
                "CITATION_STYLE, true",
                "CITATION_STYLE, false",
                "JSTYLE, true",
                "JSTYLE, false",
                "BST, true",
                "BST, false"
        })
        void removingLastRowClearsSelectionAndRepairsOnlyTheDeletedCurrentStyle(StandardFileType fileType, boolean applied) throws IOException {
            OOStyle style = addExternalStyle(fileType);
            CitationStyle fallback = CSLStyleLoader.getDefaultStyle();
            OOStyle unrelatedStyle = fileType == StandardFileType.CITATION_STYLE
                                     ? bstStyleLoader.getStyles().getLast() : fallback;
            openOfficePreferences.setCurrentStyle(applied ? style : unrelatedStyle);
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
            String source = Files.readString(Path.of(style.getPath()));

            try (MockedStatic<CSLStyleLoader> catalog = mockStatic(CSLStyleLoader.class, CALLS_REAL_METHODS)) {
                switch (style) {
                    case CitationStyle cslStyle ->
                            catalog.when(CSLStyleLoader::getStyles).thenReturn(List.of(cslStyle));
                    case JStyle jStyle ->
                            doReturn(List.of(jStyle)).when(realJStyleLoader).getStyles();
                    case BstStyle bstStyle ->
                            doReturn(List.of(bstStyle)).when(bstStyleLoader).getStyles();
                }
                viewModel = createViewModel(cslStyleLoader);

                deleteStyle(style);

                assertEquals(Optional.empty(), selectedStyle(fileType));
                switch (style) {
                    case CitationStyle _ ->
                            assertEquals(List.of(), viewModel.getAvailableCslLayouts());
                    case JStyle _ ->
                            assertEquals(List.of(), viewModel.jStylesProperty());
                    case BstStyle _ ->
                            assertEquals(List.of(), viewModel.bstStylesProperty());
                }
                assertEquals(applied ? fallback : unrelatedStyle, openOfficePreferences.getCurrentStyle());
                assertFalse(fallback.getSource().isBlank());
                assertEquals(source, Files.readString(Path.of(style.getPath())));
            }
        }

        @Test
        void cslRemovalUsesUnfilteredRowsAndKeepsTheFilter() throws IOException {
            CitationStyle style = (CitationStyle) addExternalStyle(StandardFileType.CITATION_STYLE);
            CitationStyle firstRemaining = CSLStyleLoader.getStyles().getFirst();
            viewModel.setAvailableCslLayoutsFilter(style.getTitle());
            assertEquals(List.of(style), viewModel.getAvailableCslLayouts().stream().map(CitationStylePreviewLayout::citationStyle).toList());
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            viewModel.deleteCslStyle(style);

            assertEquals(List.of(), viewModel.getAvailableCslLayouts());
            assertEquals(firstRemaining, viewModel.selectedCslLayoutProperty().get().citationStyle());
            assertEquals(firstRemaining, openOfficePreferences.getCurrentStyle());
            viewModel.setAvailableCslLayoutsFilter("");
            assertEquals(CSLStyleLoader.getStyles(), viewModel.getAvailableCslLayouts().stream().map(CitationStylePreviewLayout::citationStyle).toList());
        }

        @Test
        void hiddenCslStyleCanBeRemovedWithoutChangingAnotherSelection() throws IOException {
            CitationStyle style = (CitationStyle) addExternalStyle(StandardFileType.CITATION_STYLE);
            List<CitationStyle> remaining = CSLStyleLoader.getStyles().stream().filter(existing -> !existing.equals(style)).toList();
            CitationStyle visibleStyle = CSLStyleLoader.getStyles().getFirst();
            selectStyle(visibleStyle);
            viewModel.setAvailableCslLayoutsFilter(visibleStyle.getTitle());
            List<CitationStylePreviewLayout> visibleLayouts = List.copyOf(viewModel.getAvailableCslLayouts());
            when(dialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            viewModel.deleteCslStyle(style);

            assertEquals(visibleLayouts, viewModel.getAvailableCslLayouts());
            assertEquals(visibleStyle, viewModel.selectedCslLayoutProperty().get().citationStyle());
            assertEquals(visibleStyle, openOfficePreferences.getCurrentStyle());
            assertEquals(remaining, CSLStyleLoader.getStyles());
        }

        private OOStyle addExternalStyle(StandardFileType fileType) throws IOException {
            Path path = switch (fileType) {
                case CITATION_STYLE ->
                        writeCslStyle("imported.csl", "External import regression style", "Import regression");
                case JSTYLE ->
                        writeJStyle();
                case BST ->
                        Files.writeString(directory.resolve("imported.bst"), "READ");
                default ->
                        throw new AssertionError("Not a style file type: " + fileType);
            };
            when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(path));
            importStyle(fileType);
            return selectedStyle(fileType).orElseThrow();
        }

        private void deleteStyle(OOStyle style) {
            switch (style) {
                case CitationStyle cslStyle ->
                        viewModel.deleteCslStyle(cslStyle);
                case JStyle jStyle ->
                        viewModel.deleteJStyle(jStyle);
                case BstStyle bstStyle ->
                        viewModel.deleteBstStyle(bstStyle);
            }
        }

        private void selectStyle(OOStyle style) {
            switch (style) {
                case CitationStyle cslStyle ->
                        viewModel.selectedCslLayoutProperty().set(viewModel.getAvailableCslLayouts().stream()
                                                                           .filter(layout -> layout.citationStyle().equals(cslStyle))
                                                                           .findFirst().orElseThrow());
                case JStyle jStyle ->
                        viewModel.selectedJStyleProperty().set(viewModel.jStylesProperty().stream()
                                                                        .filter(row -> row.getJStyle().equals(jStyle))
                                                                        .findFirst().orElseThrow());
                case BstStyle bstStyle ->
                        viewModel.selectedBstStyleProperty().set(viewModel.bstStylesProperty().stream()
                                                                          .filter(row -> row.getBstStyle().equals(bstStyle))
                                                                          .findFirst().orElseThrow());
            }
        }

        private Optional<OOStyle> selectedStyle(StandardFileType fileType) {
            return switch (fileType) {
                case CITATION_STYLE ->
                        Optional.ofNullable(viewModel.selectedCslLayoutProperty().get()).map(CitationStylePreviewLayout::citationStyle);
                case JSTYLE ->
                        Optional.ofNullable(viewModel.selectedJStyleProperty().get()).map(JStyleSelectViewModel::getJStyle);
                case BST ->
                        Optional.ofNullable(viewModel.selectedBstStyleProperty().get()).map(BstStyleSelectViewModel::getBstStyle);
                default ->
                        throw new AssertionError("Not a style file type: " + fileType);
            };
        }

        private List<? extends OOStyle> styles(StandardFileType fileType) {
            return switch (fileType) {
                case CITATION_STYLE ->
                        CSLStyleLoader.getStyles();
                case JSTYLE ->
                        realJStyleLoader.getStyles();
                case BST ->
                        bstStyleLoader.getStyles();
                default ->
                        throw new AssertionError("Not a style file type: " + fileType);
            };
        }

        private void verifyConfirmation(OOStyle style) {
            verify(dialogService).showConfirmationDialogAndWait(Localization.lang("Delete style"),
                    Localization.lang("Are you sure you want to delete the style '%0'?", style.getName()),
                    Localization.lang("Delete"), Localization.lang("Cancel"));
        }

        private void verifyRemovalNotAttempted(OOStyle style) {
            switch (style) {
                case CitationStyle cslStyle ->
                        verify(cslStyleLoader, never()).removeStyle(cslStyle);
                case JStyle jStyle ->
                        verify(realJStyleLoader, never()).removeStyle(jStyle);
                case BstStyle bstStyle ->
                        verify(bstStyleLoader, never()).removeStyle(bstStyle);
            }
        }

        private StyleSelectDialogViewModel createViewModel(CSLStyleLoader loader) {
            return createViewModel(loader, realJStyleLoader, bstStyleLoader);
        }

        private StyleSelectDialogViewModel createViewModel(CSLStyleLoader loader, JStyleLoader jStyles, BstStyleLoader bstStyles) {
            GuiPreferences preferences = mock(GuiPreferences.class);
            when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));
            when(preferences.getFilePreferences()).thenReturn(filePreferences);
            when(preferences.getOpenOfficePreferences(journalAbbreviationRepository)).thenReturn(openOfficePreferences);
            return new StyleSelectDialogViewModel(dialogService, loader, jStyles, bstStyles,
                    preferences, journalAbbreviationRepository, new CurrentThreadTaskExecutor(), bibEntryTypesManager);
        }

        private Path writeCslStyle(String filename, String title, String shortTitle) throws IOException {
            return Files.writeString(directory.resolve(filename), """
                    <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                      <info>
                        <title>%s</title>
                        <title-short>%s</title-short>
                        <id>https://example.org/styles/import-regression</id>
                      </info>
                      <citation><layout><text variable="title"/></layout></citation>
                    </style>
                    """.formatted(title, shortTitle));
        }

        private Path writeJStyle() throws IOException {
            return Files.writeString(directory.resolve("imported.jstyle"), """
                    NAME
                    External import regression style
                    LAYOUT
                    default=\\title
                    """);
        }

        private void importStyle(StandardFileType fileType) {
            switch (fileType) {
                case CITATION_STYLE ->
                        viewModel.addCslStyleFile();
                case JSTYLE ->
                        viewModel.addJStyleFile();
                case BST ->
                        viewModel.addBstStyleFile();
                default ->
                        throw new AssertionError("Not a style file type: " + fileType);
            }
        }

        private ImportState state() {
            return new ImportState(CSLStyleLoader.getStyles(), realJStyleLoader.getStyles(), bstStyleLoader.getStyles(),
                    List.of(viewModel.selectedCslLayoutProperty().get().citationStyle(),
                            viewModel.selectedJStyleProperty().get().getJStyle(), viewModel.selectedBstStyleProperty().get().getBstStyle()),
                    openOfficePreferences.getCurrentStyle(), List.copyOf(openOfficePreferences.getExternalCslStyles()),
                    List.copyOf(openOfficePreferences.getExternalJStyles()), List.copyOf(openOfficePreferences.getExternalBstStyles()));
        }

        private void assertUnchanged(ImportState expected) {
            assertEquals(expected, state());
            assertEquals(expected.cslStyles(), viewModel.getAvailableCslLayouts().stream().map(CitationStylePreviewLayout::citationStyle).toList());
            assertEquals(expected.jStyles(), viewModel.jStylesProperty().stream().map(JStyleSelectViewModel::getJStyle).toList());
            assertEquals(expected.bstStyles(), viewModel.bstStylesProperty().stream().map(BstStyleSelectViewModel::getBstStyle).toList());
        }

        @NullMarked
        private record ImportState(List<CitationStyle> cslStyles, List<JStyle> jStyles, List<BstStyle> bstStyles,
                                   List<OOStyle> selections, OOStyle currentStyle, List<String> externalCslStyles,
                                   List<String> externalJStyles, List<String> externalBstStyles) {
        }
    }

    private StyleSelectDialogViewModel viewModel(JStyle currentStyle) {
        GuiPreferences preferences = mock(GuiPreferences.class);
        JournalAbbreviationRepository journalAbbreviationRepository = mock(JournalAbbreviationRepository.class);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getOpenOfficePreferences(journalAbbreviationRepository)).thenReturn(openOfficePreferences);
        openOfficePreferences.setCurrentStyle(currentStyle);
        return new StyleSelectDialogViewModel(dialogService, mock(CSLStyleLoader.class), jStyleLoader,
                mock(BstStyleLoader.class), preferences, journalAbbreviationRepository,
                mock(TaskExecutor.class), mock(BibEntryTypesManager.class));
    }
}
