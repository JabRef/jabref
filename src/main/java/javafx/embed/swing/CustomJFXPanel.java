package javafx.embed.swing;

import java.awt.event.InputMethodEvent;
import java.lang.reflect.Field;

import com.sun.javafx.embed.EmbeddedSceneInterface;

public class CustomJFXPanel extends JFXPanel {

    public CustomJFXPanel() {
        super();

        Field f;
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


        EmbeddedSceneInterface scenePeer = (EmbeddedSceneInterface) f.get;
        scenePeer.inputMethodEvent(
                javafx.scene.input.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                InputMethodSupport.inputMethodEventComposed(t, e.getCommittedCharacterCount()),
                t.substring(0, e.getCommittedCharacterCount()),
                insertionIndex);
    }
}