package javafx.embed.swing;

import java.awt.event.InputMethodEvent;
import java.lang.reflect.Field;

import com.sun.javafx.embed.EmbeddedSceneInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CustomJFXPanel extends JFXPanel {

    private EmbeddedSceneInterface myScencePeer;
    private Field scenePeerField = null;
    private static final Log LOGGER = LogFactory.getLog(CustomJFXPanel.class);

    public CustomJFXPanel() {
        super();
        try {
            scenePeerField = this.getClass().getSuperclass().getDeclaredField("scenePeer");
            scenePeerField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            // TODO Auto-generated catch block
            LOGGER.error("Could not access scenePeer Field", e);

        }
    }

    @Override
    protected void processInputMethodEvent(InputMethodEvent e) {
        if (e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
            sendInputMethodEventToFX(e);
        }

        super.processInputMethodEvent(e);
    }

    private void sendInputMethodEventToFX(InputMethodEvent e) {
        String t = InputMethodSupport.getTextForEvent(e);

        int insertionIndex = 0;
        if (e.getCaret() != null) {
            insertionIndex = e.getCaret().getInsertionIndex();
        }

        try {
            this.myScencePeer = (EmbeddedSceneInterface) scenePeerField.get(this);
        } catch (IllegalArgumentException | IllegalAccessException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        myScencePeer.inputMethodEvent(
                javafx.scene.input.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                InputMethodSupport.inputMethodEventComposed(t, e.getCommittedCharacterCount()),
                t.substring(0, e.getCommittedCharacterCount()),
                insertionIndex);
    }
}