/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.external.push;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customized UI component for pushing to external applications. Has a selection popup menu to change the selected
 * external application. This class implements the ActionListener interface. When actionPerformed() is invoked, the
 * currently selected PushToApplication is activated. The actionPerformed() method can be called with a null argument.
 */
public class PushToApplicationButton implements ActionListener {

    private final JabRefFrame frame;
    private final List<PushToApplication> pushActions;
    private JPanel comp;
    private JButton pushButton;
    private int selected;
    private JPopupMenu popup;
    private final Map<PushToApplication, PushToApplicationAction> actions = new HashMap<>();
    private final Dimension buttonDim = new Dimension(23, 23);
    private static final Icon ARROW_ICON = IconTheme.JabRefIcon.DOWN.getSmallIcon();
    private final MenuAction mAction = new MenuAction();
    private final JPopupMenu optPopup = new JPopupMenu();
    private final JMenuItem settings = new JMenuItem(Localization.lang("Settings"));


    public PushToApplicationButton(JabRefFrame frame, List<PushToApplication> pushActions) {
        this.frame = frame;
        this.pushActions = pushActions;
        init();
    }

    private void init() {
        comp = new JPanel();
        comp.setLayout(new BorderLayout());

        JButton menuButton = new JButton(PushToApplicationButton.ARROW_ICON);
        menuButton.setMargin(new Insets(0, 0, 0, 0));
        menuButton.setPreferredSize(
                new Dimension(menuButton.getIcon().getIconWidth(), menuButton.getIcon().getIconHeight()));
        menuButton.addActionListener(e -> {
            if (popup == null) {
                buildPopupMenu();
            }
            popup.show(comp, 0, menuButton.getHeight());
        });

        menuButton.setToolTipText(Localization.lang("Select external application"));

        pushButton = new JButton();
        if (OS.OS_X) {
            menuButton.putClientProperty("JButton.buttonType", "toolbar");
            pushButton.putClientProperty("JButton.buttonType", "toolbar");
        }

        // Set the last used external application
        String appSelected = Globals.prefs.get(JabRefPreferences.PUSH_TO_APPLICATION);
        for (int i = 0; i < pushActions.size(); i++) {
            if (pushActions.get(i).getApplicationName().equals(appSelected)) {
                selected = i;
                break;
            }
        }

        setSelected(selected);
        pushButton.addActionListener(this);
        pushButton.addMouseListener(new PushButtonMouseListener());
        pushButton.setOpaque(false);
        menuButton.setOpaque(false);
        comp.setOpaque(false);
        comp.add(pushButton, BorderLayout.CENTER);
        comp.add(menuButton, BorderLayout.EAST);
        comp.setMaximumSize(comp.getPreferredSize());

        optPopup.add(settings);
        settings.addActionListener(event -> {
            PushToApplication toApp = pushActions.get(selected);
            JPanel options = toApp.getSettingsPanel();
            if (options != null) {
                PushToApplicationButton.showSettingsDialog(frame, toApp, options);
            }
        });

        buildPopupMenu();
    }

    /**
     * Create a selection menu for the available "Push" options.
     */
    private void buildPopupMenu() {
        popup = new JPopupMenu();
        int j = 0;
        for (PushToApplication application : pushActions) {
            JMenuItem item = new JMenuItem(application.getApplicationName(), application.getIcon());
            item.setToolTipText(application.getTooltip());
            item.addActionListener(new PopupItemActionListener(j));
            popup.add(item);
            j++;
        }
    }

    /**
     * Update the PushButton to default to the given application.
     *
     * @param i The List index of the application to default to.
     */
    private void setSelected(int i) {
        selected = i;
        PushToApplication toApp = pushActions.get(i);
        pushButton.setIcon(toApp.getIcon());
        pushButton.setToolTipText(toApp.getTooltip());
        pushButton.setPreferredSize(buttonDim);

        // Store the last used application
        Globals.prefs.put(JabRefPreferences.PUSH_TO_APPLICATION, toApp.getApplicationName());

        mAction.setTitle(toApp.getApplicationName());
        mAction.setIcon(toApp.getIcon());
    }

    /**
     * Get the toolbar component for the push button.
     *
     * @return The component.
     */
    public Component getComponent() {
        return comp;
    }

    public Action getMenuAction() {
        return mAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PushToApplication toApp = pushActions.get(selected);

        // Lazy initialization of the push action:
        PushToApplicationAction action = actions.get(toApp);
        if (action == null) {
            action = new PushToApplicationAction(frame, toApp);
            actions.put(toApp, action);
        }
        action.actionPerformed(new ActionEvent(toApp, 0, "push"));
    }


    static class BooleanHolder {

        public boolean value;


        public BooleanHolder(boolean value) {
            this.value = value;
        }
    }


    public static void showSettingsDialog(JFrame parent, PushToApplication toApp, JPanel options) {

        final BooleanHolder okPressed = new BooleanHolder(false);
        final JDialog diag = new JDialog(parent, Localization.lang("Settings"), true);
        options.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(options, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        ok.addActionListener(e -> {
            okPressed.value = true;
            diag.dispose();
        });
        cancel.addActionListener(e -> diag.dispose());

        // Key bindings:
        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });
        diag.pack();
        diag.setLocationRelativeTo(parent);

        // Show the dialog:
        diag.setVisible(true);
        // If the user pressed Ok, ask the PushToApplication implementation
        // to store its settings:
        if (okPressed.value) {
            toApp.storeSettings();
        }
    }


    class PopupItemActionListener implements ActionListener {

        private final int index;


        public PopupItemActionListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Change the selection:
            setSelected(index);
            // Invoke the selected operation (is that expected behaviour?):
            //PushToApplicationButton.this.actionPerformed(null);
            // It makes sense to transfer focus to the push button after the
            // menu closes:
            pushButton.requestFocus();
        }
    }

    class MenuAction extends MnemonicAwareAction {

        public MenuAction() {
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.PUSH_TO_APPLICATION));
        }

        public void setTitle(String appName) {
            putValue(Action.NAME, Localization.menuTitle("Push entries to external application (%0)", appName));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            PushToApplicationButton.this.actionPerformed(null);
        }

        public void setIcon(Icon icon) {
            putValue(Action.SMALL_ICON, icon);
        }
    }

    class PushButtonMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent event) {
            if (event.isPopupTrigger()) {
                processPopupTrigger(event);
            }
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.isPopupTrigger()) {
                processPopupTrigger(event);
            }
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (event.isPopupTrigger()) {
                processPopupTrigger(event);
            }
        }

        private void processPopupTrigger(MouseEvent e) {
            // We only want to show the popup if a settings panel exists for the selected
            // item:
            PushToApplication toApp = pushActions.get(selected);
            if (toApp.getSettingsPanel() != null) {
                optPopup.show(pushButton, e.getX(), e.getY());
            }

        }
    }
}
