package org.jabref.gui.menus;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.util.io.FileHistory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(ApplicationExtension.class)
public class FileHistoryMenuTest {
    private static final String BIBTEX_LIBRARY_PATH = "src/test/resources/org/jabref/";

    private FileHistoryMenu fileHistoryMenu;
    @Mock
    private FileHistory fileHistory;
    @Mock
    private DialogService dialogService;
    @Mock
    private OpenDatabaseAction openDatabaseAction;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        fileHistoryMenu = new FileHistoryMenu(fileHistory, dialogService, openDatabaseAction);
    }

    @Test
    public void recentLibrariesAreCleared() {
        fileHistoryMenu.newFile(Path.of(BIBTEX_LIBRARY_PATH.concat("bibtexFiles/test.bib")));
        assertEquals(3, fileHistoryMenu.getItems().size());

        fileHistoryMenu.clearRecentLibraries.getOnAction().handle(null); //  retrieves the event handler
        assertTrue(fileHistoryMenu.isDisable());

        verify(fileHistory).clear();
        assertEquals(0, fileHistory.size());
    }
}
