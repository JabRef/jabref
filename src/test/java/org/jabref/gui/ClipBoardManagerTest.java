package org.jabref.gui;

import javafx.scene.input.Clipboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
  public void getHtmlContents() {
    String expectedHtml = "<li>Hello World!</li>";
    when(clipboard.getHtml()).thenReturn(expectedHtml);

    String actualHtml = clipboardManager.getHtmlContents();
    assertEquals(expectedHtml, actualHtml);
  }
}
