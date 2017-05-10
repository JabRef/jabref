package org.jabref.gui.actions;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.jabref.gui.IconTheme;

/**
 * This class extends {@link AbstractAction} with the ability to set
 * the mnemonic key based on a '&' character inserted in front of
 * the desired mnemonic letter. This is done by setting the action's
 * name using putValue(NAME, actionname).
 * This facilitates localized mnemonics.
 */
public abstract class MnemonicAwareAction extends AbstractAction {

    public MnemonicAwareAction() { }

    public MnemonicAwareAction(Icon icon) {
        if (icon instanceof IconTheme.FontBasedIcon) {
            putValue(Action.SMALL_ICON, ((IconTheme.FontBasedIcon) icon).createSmallIcon());
            putValue(Action.LARGE_ICON_KEY, icon);
        } else {
            putValue(Action.SMALL_ICON, icon);
        }
    }

    @Override
    public void putValue(String key, Object value) {
        if (key.equals(Action.NAME)) {
            String name = value.toString();
            int i = name.indexOf('&');
            if (i >= 0) {
                char mnemonic = Character.toUpperCase(name.charAt(i + 1));
                putValue(Action.MNEMONIC_KEY, (int) mnemonic);
                value = name.substring(0, i) + name.substring(i + 1);
            } else {
                value = name;
            }
        }
        super.putValue(key, value);
    }
}
