package org.jabref.gui.specialfields;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldDropDown {

    private SpecialFieldDropDown() {
    }

    public static JButton generateSpecialFieldButtonWithDropDown(SpecialField field, JabRefFrame frame) {
        Dimension buttonDim = new Dimension(23, 23);
        SpecialFieldViewModel viewModel = new SpecialFieldViewModel(field);
        JButton button = new JButton(viewModel.getRepresentingIcon());
        button.setToolTipText(viewModel.getLocalization());
        button.setPreferredSize(buttonDim);
        if (!OS.OS_X) {
            button.setMargin(new Insets(1, 0, 2, 0));
        }
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setRolloverEnabled(true);
        button.setOpaque(false);
        button.setBounds(0, 0, buttonDim.width, buttonDim.height);
        button.setSize(buttonDim);
        button.setMinimumSize(buttonDim);
        button.setMaximumSize(buttonDim);
        button.addActionListener(new MenuButtonActionListener(field, frame, button, buttonDim));
        return button;
    }

    private static class MenuButtonActionListener implements ActionListener {

        private JPopupMenu popup;
        private final Dimension dim;
        private final JabRefFrame frame;
        private final SpecialField field;
        private final JButton button;


        public MenuButtonActionListener(SpecialField field, JabRefFrame frame, JButton button, Dimension dim) {
            this.field = field;
            this.dim = dim;
            this.frame = frame;
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (popup == null) {
                popup = new JPopupMenu();
                for (SpecialFieldValue val : field.getValues()) {
                    SpecialFieldValueViewModel viewModel = new SpecialFieldValueViewModel(val);
                    JMenuItem item = new JMenuItem(viewModel.getSpecialFieldValueIcon());
                    item.setText(viewModel.getMenuString());
                    item.setToolTipText(viewModel.getToolTipText());
                    item.addActionListener(new PopupitemActionListener(frame.getCurrentBasePanel(), new SpecialFieldValueViewModel(val).getActionName()));
                    item.setMargin(new Insets(0, 0, 0, 0));
                    popup.add(item);
                }
            }
            popup.show(button, 0, dim.height);
        }

        private class PopupitemActionListener implements ActionListener {

            private final BasePanel panel;
            private final String actionName;


            public PopupitemActionListener(BasePanel panel, String actionName) {
                this.panel = panel;
                this.actionName = actionName;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                panel.runCommand(actionName);
                popup.setVisible(false);
            }

        }

    }

}
