package javafx.embed.swing;

import java.awt.event.InputMethodEvent;
import java.lang.reflect.Field;

import com.sun.javafx.embed.EmbeddedSceneInterface;

public class CustomJFXPanel extends JFXPanel

{

    private EmbeddedSceneInterface scenePeer;
    private Field f = null;

    public CustomJFXPanel() {
        super();
        try {
            f = this.getClass().getSuperclass().getDeclaredField("scenePeer");
            f.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            this.scenePeer = (EmbeddedSceneInterface) f.get(this);
        } catch (IllegalArgumentException | IllegalAccessException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        scenePeer.inputMethodEvent(
                javafx.scene.input.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                InputMethodSupport.inputMethodEventComposed(t, e.getCommittedCharacterCount()),
                t.substring(0, e.getCommittedCharacterCount()),
                insertionIndex);
    }

}