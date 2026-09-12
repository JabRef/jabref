package org.jabref.gui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import de.sandec.jmemorybuddy.JMemoryBuddy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Closing a library has to release the tab that showed it.
///
/// The [StateManager] outlives every library, so whatever it still points at after a close stays for
/// the rest of the session - the tab, its table model, its [BibDatabaseContext] and all its entries.
/// [LibraryTab#onClosed] is what has to prevent that.
@ExtendWith(ApplicationExtension.class)
class LibraryTabRetentionTest {

    private GuiPreferences preferences;
    private StateManager stateManager;
    private TabPane tabPane;
    private int libraryCount;

    @BeforeEach
    void setUp() {
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn("owner");
        when(preferences.getAutoCompletePreferences().shouldAutoComplete()).thenReturn(true);
        when(preferences.getSearchPreferences().usePostgresSearchProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(preferences.getSearchPreferences().isFulltext()).thenReturn(false);
        when(preferences.getFilePreferences().shouldCreateBackup()).thenReturn(false);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(preferences.getKeyBindingRepository()).thenReturn(new KeyBindingRepository());
        when(preferences.getImporterPreferences().getCustomImporters()).thenReturn(FXCollections.observableSet());
        when(preferences.getMainTablePreferences().getColumnPreferences().getColumnSortOrder()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getMainTablePreferences().getColumnPreferences().getColumns()).thenReturn(FXCollections.observableArrayList());

        // Real properties wherever the code under test registers a listener: a mock remembers the
        // listener it was handed, and Mockito's bookkeeping would then root everything that listener
        // captures - the very objects this test asserts are gone.
        when(preferences.getSearchPreferences().searchDisplayModeProperty()).thenReturn(new SimpleObjectProperty<>(SearchDisplayMode.FILTER));
        when(preferences.getGroupsPreferences().groupViewModeProperty()).thenReturn(new SimpleSetProperty<>(FXCollections.observableSet(EnumSet.noneOf(GroupViewMode.class))));
        when(preferences.getFilePreferences().fulltextIndexLinkedFilesProperty()).thenReturn(new SimpleBooleanProperty(false));

        stateManager = new JabRefGuiStateManager();

        Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(UndoManager.class, new JabRefUndoManager());
        Injector.setModelOrService(UndoAction.class, mock(UndoAction.class));
        Injector.setModelOrService(RedoAction.class, mock(RedoAction.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());
        Injector.setModelOrService(JournalAbbreviationRepository.class, mock(JournalAbbreviationRepository.class));
        Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());
    }

    private static void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(60, TimeUnit.SECONDS));
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
    }

    /// Mirrors [org.jabref.gui.frame.JabRefFrame#initBindings]: the libraries the [StateManager]
    /// reports as open are the library tabs of the tab pane.
    private void setUpTabPane() throws InterruptedException {
        runOnFxThreadAndWait(() -> {
            tabPane = new TabPane();
            BindingsHelper.bindContentFiltered(tabPane.getTabs(), stateManager.getOpenDatabases(), LibraryTab.class::isInstance);
            // ... and the active library follows the selected tab, so closing one hands the state
            // manager the tab that takes over instead of leaving the closed one behind.
            com.tobiasdiez.easybind.EasyBind.subscribe(tabPane.getSelectionModel().selectedItemProperty(), selected -> {
                if (selected instanceof LibraryTab libraryTab) {
                    stateManager.setActiveDatabase(libraryTab.getBibDatabaseContext());
                    stateManager.activeTabProperty().set(Optional.of(libraryTab));
                } else {
                    stateManager.setActiveDatabase(null);
                    stateManager.activeTabProperty().set(Optional.empty());
                }
            });
        });
    }

    private LibraryTab openTab() throws InterruptedException {
        LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList());

        BibDatabase database = new BibDatabase(List.of(
                new BibEntry(StandardEntryType.Article)
                        // Distinct content per library: BibDatabaseContext#equals compares content,
                        // and the open-database list removes by equality when a tab closes.
                        .withCitationKey("Author2021_" + libraryCount++)
                        .withField(StandardField.AUTHOR, "Author, A.")
                        .withField(StandardField.TITLE, "Title")));
        BibDatabaseContext context = new BibDatabaseContext(database);

        LibraryTab[] created = new LibraryTab[1];
        runOnFxThreadAndWait(() -> {
            LibraryTab tab = LibraryTab.createLibraryTab(
                    context,
                    tabContainer,
                    mock(DialogService.class),
                    mock(AiService.class, Answers.RETURNS_DEEP_STUBS),
                    preferences,
                    stateManager,
                    new DummyFileUpdateMonitor(),
                    new BibEntryTypesManager(),
                    mock(ClipBoardManager.class),
                    new CurrentThreadTaskExecutor(),
                    mock(GitHandlerRegistry.class));
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            created[0] = tab;
        });
        // Part of the tab's listeners are registered from a Platform.runLater, so let that run
        runOnFxThreadAndWait(() -> {
        });
        return created[0];
    }

    /// Mirrors [org.jabref.gui.frame.JabRefFrame#closeTabs].
    private void closeTab(LibraryTab tab) throws InterruptedException {
        runOnFxThreadAndWait(() -> {
            tabPane.getTabs().remove(tab);
            Event.fireEvent(tab, new Event(tabPane, tab, Tab.CLOSED_EVENT));
        });
        runOnFxThreadAndWait(() -> {
        });
    }

    /// Mockito keeps the last invocation per mock plus a per-thread "ongoing stubbing", and both
    /// hold on to the arguments they saw. Deep stubs record on whichever thread called them, so the
    /// JavaFX thread has state of its own. Left in place, that roots the objects under test and the
    /// assertions below would be measuring the harness instead of the code.
    private static void releaseMocks() throws InterruptedException {
        runOnFxThreadAndWait(Mockito::validateMockitoUsage);
        Mockito.validateMockitoUsage();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    void closedTabIsReleased() throws InterruptedException {
        setUpTabPane();

        // Held in a slot the assertion can empty: a local variable, or a lambda capturing one, would
        // keep the tab alive by itself.
        LibraryTab[] tab = {openTab()};
        closeTab(tab[0]);
        releaseMocks();

        JMemoryBuddy.memoryTest(checker -> {
            checker.assertCollectable(tab[0]);
            tab[0] = null;
        });
    }

    @Test
    void closedTabIsReleasedWhileAnotherLibraryStaysOpen() throws InterruptedException {
        setUpTabPane();

        LibraryTab stays = openTab();
        LibraryTab[] goes = {openTab()};
        closeTab(goes[0]);
        releaseMocks();

        JMemoryBuddy.memoryTest(checker -> {
            checker.assertCollectable(goes[0]);
            checker.setAsReferenced(stays);
            goes[0] = null;
        });
    }

    @Test
    void closedLibrariesDoNotAccumulate() throws InterruptedException {
        setUpTabPane();

        List<BibDatabaseContext> contexts = new ArrayList<>();
        for (int round = 0; round < 3; round++) {
            LibraryTab tab = openTab();
            contexts.add(tab.getBibDatabaseContext());
            closeTab(tab);
        }
        releaseMocks();

        JMemoryBuddy.memoryTest(checker -> {
            contexts.forEach(checker::assertCollectable);
            contexts.clear();
        });
    }
}
