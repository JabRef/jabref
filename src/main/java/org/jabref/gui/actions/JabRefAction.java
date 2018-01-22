package org.jabref.gui.actions;

import org.jabref.gui.keyboard.KeyBindingRepository;

import org.controlsfx.control.action.Action;

/**
 * Wrapper around one of our actions from {@link ActionsFX} to convert them to controlsfx {@link Action}.
 */
class JabRefAction extends Action {
    public JabRefAction(ActionsFX action, KeyBindingRepository keyBindingRepository) {
        super(action.getText());
        action.getIcon()
              .ifPresent(icon -> setGraphic(icon.getGraphicNode()));
        action.getKeyBinding()
              .ifPresent(keyBinding -> setAccelerator(keyBindingRepository.getKeyCombination(keyBinding)));
    }
}
