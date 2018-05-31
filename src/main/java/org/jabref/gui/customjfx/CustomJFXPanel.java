package org.jabref.gui.customjfx;

import java.awt.event.InputMethodEvent;
import java.lang.reflect.Field;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.gui.customjfx.support.InputMethodSupport;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.util.OS;

import com.sun.javafx.embed.EmbeddedSceneInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * WARNING: THIS IS A CUSTOM HACK TO PREVENT A BUG WITH ACCENTED CHARACTERS PRODUCING AN NPE IN LINUX </br>
 * So far the bug has only been resolved in openjfx10: <a href="https://bugs.openjdk.java.net/browse/JDK-8185792">https://bugs.openjdk.java.net/browse/JDK-8185792</a>
 *
 */
public class CustomJFXPanel extends JFXPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomJFXPanel.class);
    private Field scenePeerField = null;

    private CustomJFXPanel() {
        super();
        try {
            scenePeerField = this.getClass().getSuperclass().getDeclaredField("scenePeer");
            scenePeerField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            LOGGER.error("Could not access scenePeer Field", e);

        }
    }

    public static JFXPanel create() {
        return OS.LINUX ? new CustomJFXPanel() : new JFXPanel();
    }

    public static JFXPanel wrap(Scene scene) {
        JFXPanel container = OS.LINUX ? new CustomJFXPanel() : new JFXPanel();
        DefaultTaskExecutor.runInJavaFXThread(() -> container.setScene(scene));
        return container;
    }

    @Override
    protected void processInputMethodEvent(InputMethodEvent e) {
        if (e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            sendInputMethodEventToFX(e);
        }

    }

    private void sendInputMethodEventToFX(InputMethodEvent e) {
        String t = InputMethodSupport.getTextForEvent(e);

        int insertionIndex = 0;
        if (e.getCaret() != null) {
            insertionIndex = e.getCaret().getInsertionIndex();
        }

        EmbeddedSceneInterface myScencePeer = null;
        try {
            //the variable must be named different to the original, otherwise reflection does not find the right ones
            myScencePeer = (EmbeddedSceneInterface) scenePeerField.get(this);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOGGER.error("Could not access scenePeer Field", ex);
        }

        myScencePeer.inputMethodEvent(
                javafx.scene.input.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                InputMethodSupport.inputMethodEventComposed(t, e.getCommittedCharacterCount()),
                t.substring(0, e.getCommittedCharacterCount()),
                insertionIndex);
    }
}
