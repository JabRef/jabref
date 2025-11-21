package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.walkthrough.Walkthrough;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Side effect that opens a specified library in a new tab.
public class OpenLibrarySideEffect implements WalkthroughSideEffect {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenLibrarySideEffect.class);
    private static final String WALKTHROUGH_LIBRARY_TEMPLATE = "Example Library (%s)";

    private final String libraryName;
    private final LibraryTabContainer tabContainer;
    private final StateManager stateManager;
    private @Nullable LibraryTab createdTab;

    public OpenLibrarySideEffect(LibraryTabContainer frame) {
        this(frame, "Chocolate.bib");
    }

    public OpenLibrarySideEffect(LibraryTabContainer frame, String libraryName) {
        this.tabContainer = frame;
        this.stateManager = Injector.instantiateModelOrService(StateManager.class);
        this.libraryName = libraryName;
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        LOGGER.debug("Executing forward: Opening example library");

        // Check if example library is already open
        Optional<LibraryTab> existingTabOpt = findLibraryTab();
        if (existingTabOpt.isPresent()) {
            LOGGER.debug("Example library already open, selecting existing tab");
            tabContainer.showLibraryTab(existingTabOpt.get());
            createdTab = existingTabOpt.get();
            return true;
        }

        try {
            Optional<BibDatabaseContext> databaseContext = loadExampleLibrary();
            if (databaseContext.isEmpty()) {
                LOGGER.error("Failed to load example library");
                return false;
            }

            LibraryTab libraryTab = LibraryTab.createLibraryTab(
                    databaseContext.get(),
                    tabContainer,
                    Injector.instantiateModelOrService(DialogService.class),
                    Injector.instantiateModelOrService(AiService.class),
                    Injector.instantiateModelOrService(GuiPreferences.class),
                    stateManager,
                    Injector.instantiateModelOrService(FileUpdateMonitor.class),
                    Injector.instantiateModelOrService(DirectoryUpdateMonitor.class),
                    Injector.instantiateModelOrService(BibEntryTypesManager.class),
                    Injector.instantiateModelOrService(CountingUndoManager.class),
                    Injector.instantiateModelOrService(ClipBoardManager.class),
                    Injector.instantiateModelOrService(TaskExecutor.class)
            );

            libraryTab.setText(WALKTHROUGH_LIBRARY_TEMPLATE.formatted(libraryName));

            tabContainer.addTab(libraryTab, true);
            createdTab = libraryTab;

            LOGGER.debug("Successfully opened example library");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to open example library", e);
            return false;
        }
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        LOGGER.debug("Executing backward: Closing example library");

        if (createdTab != null) {
            boolean closed = tabContainer.closeTab(createdTab);
            if (closed) {
                LOGGER.debug("Successfully closed example library tab");
                createdTab = null;
                return true;
            } else {
                LOGGER.warn("Failed to close example library tab");
                return false;
            }
        }

        Optional<LibraryTab> exampleTab = findLibraryTab();
        if (exampleTab.isPresent()) {
            boolean closed = tabContainer.closeTab(exampleTab.get());
            if (!closed) {
                LOGGER.debug("Successfully closed found example library tab");
            } else {
                LOGGER.warn("Failed to close found example library tab");
                return false;
            }
        }

        LOGGER.debug("No example library tab found to close");
        return true;
    }

    @Override
    public @NonNull String description() {
        return "Open \"%s\" library.".formatted(libraryName);
    }

    private Optional<LibraryTab> findLibraryTab() {
        return tabContainer.getLibraryTabs().stream()
                           .filter(tab -> WALKTHROUGH_LIBRARY_TEMPLATE
                                   .formatted(libraryName).equals(tab.getText()) ||
                                   (tab.getBibDatabaseContext().getDatabasePath().isEmpty() &&
                                           tab.getBibDatabaseContext().getDatabase().getEntryCount() > 0))
                           .findFirst();
    }

    private Optional<BibDatabaseContext> loadExampleLibrary() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(libraryName)) {
            if (in == null) {
                LOGGER.warn("\"{}\" Library file not found", libraryName);
                return Optional.empty();
            }

            return Optional.of(OpenDatabase.loadDatabase(in,
                    Injector.instantiateModelOrService(GuiPreferences.class).getImportFormatPreferences(),
                    Injector.instantiateModelOrService(FileUpdateMonitor.class),
                    Injector.instantiateModelOrService(DirectoryUpdateMonitor.class)).getDatabaseContext());
        } catch (IOException e) {
            LOGGER.error("Failed to load \"{}\" library from resource", libraryName, e);
            return Optional.empty();
        }
    }
}
