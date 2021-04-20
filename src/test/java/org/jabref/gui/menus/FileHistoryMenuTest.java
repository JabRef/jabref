package org.jabref.gui.menus;

import javafx.scene.input.KeyEvent;
import org.jabref.gui.DialogService;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.preferences.PreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileHistoryMenuTest {

  private final FileHistory history = mock(FileHistory.class);
  private final PreferencesService preferences = mock(PreferencesService.class);
  private final DialogService dialogService = mock(DialogService.class);
  private final OpenDatabaseAction openDatabaseAction = mock(OpenDatabaseAction.class);

  @BeforeEach
  void setUp() {
  }

  @Test
  void openCorrectFileInMenuAfterKeystroke() {
    Path p1 = Paths.get("/tmp/foo/1");

    LinkedList<Path> fileHistory = new LinkedList<Path>(Arrays.asList(p1));

    when(history.getHistory()).thenReturn(fileHistory);
    when(history.getFileAt(0)).thenReturn(fileHistory.get(0));
    when(preferences.getFileHistory()).thenReturn(new FileHistory(fileHistory));
    when(history.isEmpty()).thenReturn(false);

    KeyEvent pressOne = mock(KeyEvent.class);
    when(pressOne.getCharacter()).thenReturn(String.valueOf('1'));

    FileHistoryMenu historyMenu = new FileHistoryMenu(preferences,dialogService,openDatabaseAction);
    FileHistoryMenu spyHistory = Mockito.spy(historyMenu);

    spyHistory.openFileByKey(pressOne);

    verify(spyHistory,times(1)).openFile(history.getFileAt(0)); // First element in the file history should be opened, when pressing 1
  }
}
