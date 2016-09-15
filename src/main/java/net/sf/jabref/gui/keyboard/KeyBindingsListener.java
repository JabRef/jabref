package net.sf.jabref.gui.keyboard;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * respond to grabKey and display the key binding
 */
public class KeyBindingsListener extends KeyAdapter {

    private final KeyBindingTable table;

    public KeyBindingsListener(KeyBindingTable table) {
        this.table = table;
    }

    @Override
    public void keyPressed(KeyEvent evt) {

        boolean isFunctionKey = false;
        boolean isEscapeKey = false;
        boolean isDeleteKey = false;

        // first check if anything is selected if not then return
        final int selRow = table.getSelectedRow();
        boolean isAnyRowSelected = selRow >= 0;
        if (!isAnyRowSelected) {
            return;
        }

        final String modifier = getModifierText(evt);

        // VALIDATE code and modifier
        // all key bindings must have a modifier: ctrl alt etc
        if ("".equals(modifier)) {
            int kc = evt.getKeyCode();
            isFunctionKey = (kc >= KeyEvent.VK_F1) && (kc <= KeyEvent.VK_F12);
            isEscapeKey = kc == KeyEvent.VK_ESCAPE;
            isDeleteKey = kc == KeyEvent.VK_DELETE;
            if (!(isFunctionKey || isEscapeKey || isDeleteKey)) {
                // need a modifier except for function, escape and delete keys
                return;
            }
        }

        int code = evt.getKeyCode();
        String newKey;
        //skip the event triggered only by a modifier, tab, backspace or enter because these normally have preset
        // functionality if they alone are pressed
        if (code == KeyEvent.VK_ALT ||
                code == KeyEvent.VK_TAB ||
                code == KeyEvent.VK_BACK_SPACE ||
                code == KeyEvent.VK_ENTER ||
                code == KeyEvent.VK_SPACE ||
                code == KeyEvent.VK_CONTROL ||
                code == KeyEvent.VK_SHIFT ||
                code == KeyEvent.VK_META) {
            return;
        }
        if ("".equals(modifier)) {
            if (isFunctionKey) {
                newKey = KeyEvent.getKeyText(code);
            } else if (isEscapeKey) {
                newKey = "ESCAPE";
            } else if (isDeleteKey) {
                newKey = "DELETE";
            } else {
                return;
            }
        } else {
            newKey = modifier.toLowerCase(Locale.ENGLISH) + " " + KeyEvent.getKeyText(code);
        }
        //SHOW new key binding
        //find which key is selected and set its value
        table.setValueAt(newKey, selRow, 1);
        table.revalidate();
        table.repaint();
    }

    /**
     * Collects th English translations of all modifiers and returns them separated by a space
     *
     * @param evt the KeyEvent that was triggered to set the KeyBindings
     * @return a String containing the modifier keys text
     */
    private String getModifierText(KeyEvent evt) {
        ArrayList<String> modifiersList = new ArrayList<>();

        if (evt.isControlDown()) {
            modifiersList.add("ctrl");
        }
        if (evt.isAltDown()) {
            modifiersList.add("alt");
        }
        if (evt.isShiftDown()) {
            modifiersList.add("shift");
        }
        if (evt.isAltGraphDown()) {
            modifiersList.add("alt gr");
        }
        if (evt.isMetaDown()) {
            modifiersList.add("meta");
        }
        //Now build the String with all the modifier texts
        String modifiersAsString = modifiersList.stream().collect(Collectors.joining(" "));
        return modifiersAsString;
    }
}
