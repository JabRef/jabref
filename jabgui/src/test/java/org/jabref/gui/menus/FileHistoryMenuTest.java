package org.jabref.gui.menus;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.FileHistoryMenu;
import org.jabref.logic.util.io.FileHistory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

@ExtendWith(ApplicationExtension.class)
class FileHistoryMenuTest {
    private static final String BIBTEX_LIBRARY_PATH = "src/test/resources/org/jabref/";

    private FileHistoryMenu fileHistoryMenu;
    @Mock
    private FileHistory fileHistory;
    @Mock
    private DialogService dialogService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        // "null" is a workaround, because OpenDatabaseAction cannot be mocked easily
        fileHistoryMenu = new FileHistoryMenu(fileHistory, dialogService, null);
    }

    @Test
    void recentLibrariesAreCleared() {
        fileHistoryMenu.newFile(Path.of(BIBTEX_LIBRARY_PATH.concat("bibtexFiles/test.bib")));

        fileHistoryMenu.clearLibrariesHistory();
        assertTrue(fileHistoryMenu.isDisable());
        assertEquals(0, fileHistory.size());
    }
}
