package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.BstCitationFormat;
import org.jabref.logic.openoffice.bst.BstStyle;
import org.jabref.logic.openoffice.bst.BstStyleLoader;
import org.jabref.logic.openoffice.jstyle.JStyle;
import org.jabref.logic.openoffice.jstyle.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
@Isolated("Afterburner injection and CSL style discovery are shared across dialogs")
@ResourceLock("Localization.lang")
class StyleSelectDialogViewTest {

    private static final List<String> TABLE_IDS = List.of("#cslStylesTable", "#jStylesTable", "#bstStylesTable");
    private static final List<String> PREVIEW_IDS = List.of("#cslPreviewBox", "#jStylePreviewBox", "#bstPreviewBox");

    private final CSLStyleLoader cslStyleLoader = mock(CSLStyleLoader.class);
    private final JStyleLoader jStyleLoader = mock(JStyleLoader.class);
    private final BstStyleLoader bstStyleLoader = mock(BstStyleLoader.class);
    private final JournalAbbreviationRepository journalAbbreviationRepository = mock(JournalAbbreviationRepository.class);
    private final OpenOfficePreferences openOfficePreferences = mock(OpenOfficePreferences.class);
    private final List<CitationStyle> cslStyles = List.of(cslStyle("First CSL"), cslStyle("Latest CSL"));
    private final List<JStyle> jStyles = List.of(jStyle("first"), jStyle("latest"));
    private final List<BstStyle> bstStyles = List.of(bstStyle("first.bst"), bstStyle("latest.bst"));
    private final List<BstPreviewLayout> bstLayouts = List.of(mock(BstPreviewLayout.class), mock(BstPreviewLayout.class));

    private MockedStatic<CSLStyleLoader> cslDiscovery;
    private MockedStatic<BstPreviewLayout> bstPreviews;
    private final List<PreviewViewer> viewers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        JavaFxExtension.invokeAndWait(() -> {
            cslDiscovery = mockStatic(CSLStyleLoader.class);
            cslDiscovery.when(CSLStyleLoader::getStyles).thenReturn(cslStyles);
            bstPreviews = mockStatic(BstPreviewLayout.class);
            for (int index = 0; index < bstStyles.size(); index++) {
                BstStyle style = bstStyles.get(index);
                BstPreviewLayout layout = bstLayouts.get(index);
                bstPreviews.when(() -> BstPreviewLayout.of(style)).thenReturn(layout);
            }

            when(jStyleLoader.getStyles()).thenReturn(jStyles);
            when(bstStyleLoader.getStyles()).thenReturn(bstStyles);
            when(openOfficePreferences.getBstCitationFormat()).thenReturn(BstCitationFormat.NUMERIC);
            when(openOfficePreferences.getPandocPath()).thenReturn("");
            GuiPreferences preferences = mock(GuiPreferences.class);
            when(preferences.getOpenOfficePreferences(journalAbbreviationRepository)).thenReturn(openOfficePreferences);
            when(preferences.getWorkspacePreferences()).thenReturn(WorkspacePreferences.getDefault());
            when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));
            Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
            Injector.setModelOrService(GuiPreferences.class, preferences);
            Injector.setModelOrService(DialogService.class, mock(DialogService.class));
            Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
            Injector.setModelOrService(BibEntryTypesManager.class, mock(BibEntryTypesManager.class));
        });
    }

    @AfterEach
    void tearDown() {
        try {
            JavaFxExtension.awaitEvents();
        } finally {
            JavaFxExtension.invokeAndWait(() -> {
                try (MockedStatic<CSLStyleLoader> ignoredCsl = cslDiscovery;
                     MockedStatic<BstPreviewLayout> ignoredBst = bstPreviews) {
                    Injector.forgetAll();
                }
            });
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void openingRendersAllSelectedPreviewsAndPreservesFallbackSelections(int activeFamily) {
        JavaFxExtension.invokeAndWait(() -> {
            StyleSelectDialogView dialog = dialog(activeFamily, 1);
            TabPane tabs = tabs(dialog);

            assertEquals(activeFamily, tabs.getSelectionModel().getSelectedIndex());
            for (int family = 0; family < 3; family++) {
                assertEquals(family == activeFamily ? 1 : 0, table(tabs, family).getSelectionModel().getSelectedIndex());
                assertEquals(viewerCount(family), previewBox(tabs, family).getChildren().size());
                assertPreview(tabs, family, family == activeFamily ? 1 : 0);
            }
            assertEquals(4, viewers.size());
            verify(openOfficePreferences, never()).setCurrentStyle(any());
            assertEquals(style(activeFamily, 1), dialog.getResultConverter().call(ButtonType.OK));
            verify(openOfficePreferences).setCurrentStyle(style(activeFamily, 1));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void activeTabRendersNewlySelectedRowWithoutChangingAppliedStyle(int family) {
        JavaFxExtension.invokeAndWait(() -> {
            StyleSelectDialogView dialog = dialog(family, 0);
            TabPane tabs = tabs(dialog);
            clearPreviewInvocations();

            table(tabs, family).getSelectionModel().selectLast();

            assertPreview(tabs, family, 1);
            assertEquals(4, viewers.size());
            verify(openOfficePreferences, never()).setCurrentStyle(any());
            assertNull(dialog.getResultConverter().call(ButtonType.CANCEL));
            verify(openOfficePreferences, never()).setCurrentStyle(any());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void hiddenSelectionUpdatesItsPreviewWithoutWaitingForTabActivation(int family) {
        JavaFxExtension.invokeAndWait(() -> {
            int otherFamily = (family + 1) % 3;
            StyleSelectDialogView dialog = dialog(otherFamily, 0);
            TabPane tabs = tabs(dialog);
            clearPreviewInvocations();

            table(tabs, family).getSelectionModel().selectLast();

            assertEquals(4, viewers.size());
            assertPreview(tabs, family, 1);
            clearPreviewInvocations();

            tabs.getSelectionModel().select(family);

            assertNoPreviewUpdates();
            bstPreviews.verifyNoInteractions();
            tabs.getSelectionModel().select(otherFamily);
            table(tabs, family).getSelectionModel().selectFirst();

            assertPreview(tabs, family, 0);
            clearPreviewInvocations();
            tabs.getSelectionModel().select(family);

            assertNoPreviewUpdates();
            assertEquals(4, viewers.size());
            assertEquals(style(family, 0), dialog.getResultConverter().call(ButtonType.OK));
            verify(openOfficePreferences).setCurrentStyle(style(family, 0));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void delayedCslDiscoveryRendersItsSelectionRegardlessOfActiveTab(int activeFamily) {
        JavaFxExtension.invokeAndWait(() -> {
            TaskExecutor executor = mock(TaskExecutor.class);
            Injector.setModelOrService(TaskExecutor.class, executor);
            StyleSelectDialogView dialog = dialog(activeFamily, 0);
            TabPane tabs = tabs(dialog);
            assertEquals(List.of(), table(tabs, 0).getItems());
            clearPreviewInvocations();
            ArgumentCaptor<BackgroundTask<List<CitationStyle>>> discovery = ArgumentCaptor.captor();
            verify(executor).execute(discovery.capture());

            new CurrentThreadTaskExecutor().execute(discovery.getValue());

            assertEquals(2, table(tabs, 0).getItems().size());
            assertPreview(tabs, 0, 0);
            assertEquals(4, viewers.size());
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void familyWithoutASelectionDoesNotCreatePreviewViewers(int emptyFamily) {
        JavaFxExtension.invokeAndWait(() -> {
            switch (emptyFamily) {
                case 0 ->
                        cslDiscovery.when(CSLStyleLoader::getStyles).thenReturn(List.of());
                case 1 ->
                        when(jStyleLoader.getStyles()).thenReturn(List.of());
                default ->
                        when(bstStyleLoader.getStyles()).thenReturn(List.of());
            }
            StyleSelectDialogView dialog = dialog((emptyFamily + 1) % 3, 0);
            TabPane tabs = tabs(dialog);

            assertEquals(List.of(), previewBox(tabs, emptyFamily).getChildren());
            assertEquals(4 - viewerCount(emptyFamily), viewers.size());
        });
    }

    private StyleSelectDialogView dialog(int family, int row) {
        when(openOfficePreferences.getCurrentStyle()).thenReturn(style(family, row));
        return new StyleSelectDialogView(cslStyleLoader, jStyleLoader, bstStyleLoader, journalAbbreviationRepository,
                (dialogService, preferences, taskExecutor) -> {
                    PreviewViewer viewer = spy(new PreviewViewer(dialogService, preferences, taskExecutor));
                    doNothing().when(viewer).setDatabaseContext(any());
                    doNothing().when(viewer).setEntry(any());
                    doNothing().when(viewer).setLayout(any());
                    viewers.add(viewer);
                    return viewer;
                });
    }

    private OOStyle style(int family, int row) {
        return switch (family) {
            case 0 ->
                    cslStyles.get(row);
            case 1 ->
                    jStyles.get(row);
            default ->
                    bstStyles.get(row);
        };
    }

    private TabPane tabs(StyleSelectDialogView dialog) {
        return JavaFxExtension.lookup(dialog.getDialogPane(), "#tabPane", TabPane.class);
    }

    private TableView<?> table(TabPane tabs, int family) {
        return JavaFxExtension.lookup((Parent) tabs.getTabs().get(family).getContent(), TABLE_IDS.get(family), TableView.class);
    }

    private VBox previewBox(TabPane tabs, int family) {
        return JavaFxExtension.lookup((Parent) tabs.getTabs().get(family).getContent(), PREVIEW_IDS.get(family), VBox.class);
    }

    private int viewerCount(int family) {
        return family == 1 ? 2 : 1;
    }

    private void clearPreviewInvocations() {
        viewers.forEach(viewer -> clearInvocations(viewer));
        bstPreviews.clearInvocations();
    }

    private void assertNoPreviewUpdates() {
        viewers.forEach(viewer -> verify(viewer, never()).setLayout(any()));
    }

    private void assertPreview(TabPane tabs, int family, int row) {
        List<PreviewViewer> previews = previewBox(tabs, family).getChildren().stream().map(PreviewViewer.class::cast).toList();
        assertEquals(viewerCount(family), previews.size());
        for (int index = 0; index < previews.size(); index++) {
            ArgumentCaptor<PreviewLayout> layout = ArgumentCaptor.forClass(PreviewLayout.class);
            verify(previews.get(index), atLeastOnce()).setLayout(layout.capture());
            PreviewLayout latest = layout.getAllValues().getLast();
            switch (family) {
                case 0 ->
                        assertEquals(cslStyles.get(row), ((CitationStylePreviewLayout) latest).citationStyle());
                case 1 ->
                        assertEquals((index == 0 ? "article-" : "book-") + jStyles.get(row).getName(), latest.getText());
                default ->
                        assertEquals(bstLayouts.get(row), latest);
            }
        }
    }

    private static CitationStyle cslStyle(String title) {
        CitationStyle style = mock(CitationStyle.class);
        when(style.getTitle()).thenReturn(title);
        when(style.isInternalStyle()).thenReturn(true);
        return style;
    }

    private static JStyle jStyle(String name) {
        JStyle style = mock(JStyle.class);
        when(style.getName()).thenReturn(name);
        when(style.getPath()).thenReturn(name + ".jstyle");
        when(style.getJournals()).thenReturn(Set.of());
        when(style.isInternalStyle()).thenReturn(true);
        Layout article = mock(Layout.class);
        when(article.getText()).thenReturn("article-" + name);
        when(style.getReferenceFormat(StandardEntryType.Article)).thenReturn(article);
        Layout book = mock(Layout.class);
        when(book.getText()).thenReturn("book-" + name);
        when(style.getReferenceFormat(StandardEntryType.Book)).thenReturn(book);
        return style;
    }

    private static BstStyle bstStyle(String name) {
        BstStyle style = mock(BstStyle.class);
        when(style.getName()).thenReturn(name);
        when(style.getPath()).thenReturn(name);
        when(style.isInternalStyle()).thenReturn(true);
        return style;
    }
}
