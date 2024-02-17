package org.jabref.gui;
import org.jabref.gui.ClipBoardManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javafx.scene.input.Clipboard;

public class ClipBoardManagerTest {

  @Mock
  private Clipboard clipboard;
  private ClipBoardManager clipboardManager;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    clipboardManager = new ClipBoardManager(clipboard, null, null);
  }

  @Test
  public void testGetHtmlContents() {
    String expectedHtml = "<li>Hello World!</li>";
    when(clipboard.getHtml()).thenReturn(expectedHtml);

    String actualHtml = clipboardManager.getHtmlContents();
    assertEquals(expectedHtml, actualHtml);
  }
}
