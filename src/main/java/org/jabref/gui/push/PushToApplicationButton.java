package org.jabref.gui.push;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * Customized UI component for pushing to external applications. Has a selection popup menu to change the selected
 * external application. This class implements the ActionListener interface. When actionPerformed() is invoked, the
 * currently selected PushToApplication is activated. The actionPerformed() method can be called with a null argument.
 */
public class PushToApplicationButton implements ActionListener {

    private static final Icon ARROW_ICON = IconTheme.JabRefIcon.DOWN.getSmallIcon();
    private final JabRefFrame frame;
    private final List<PushToApplication> pushActions;
    private JPanel comp;
    private JButton pushButton;
    private PushToApplication toApp;
    private JPopupMenu popup;
    private final Map<PushToApplication, PushToApplicationAction> actions = new HashMap<>();
    private final Dimension buttonDim = new Dimension(23, 23);
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
        for (PushToApplication application : pushActions) {
            if (application.getApplicationName().equals(appSelected)) {
                toApp = application;
                break;
            }
        }

        if (toApp == null) {
            // Nothing found, pick first
            toApp = pushActions.get(0);
        }

        setSelected();
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
        for (PushToApplication application : pushActions) {
            JMenuItem item = new JMenuItem(application.getApplicationName(), application.getIcon());
            item.setToolTipText(application.getTooltip());
            item.addActionListener(new PopupItemActionListener(application));
            popup.add(item);
        }
    }

    /**
     * Update the PushButton to default to the given application.
     *
     * @param i The List index of the application to default to.
     */
    private void setSelected(PushToApplication newApplication) {
        toApp = newApplication;
        setSelected();
    }

    private void setSelected() {
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
        // Lazy initialization of the push action:
        PushToApplicationAction action = actions.get(toApp);
        if (action == null) {
            action = new PushToApplicationAction(frame, toApp);
            actions.put(toApp, action);
        }
        action.actionPerformed(new ActionEvent(toApp, 0, "push"));
    }

    private static class BooleanHolder {

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

        private final PushToApplication application;


        public PopupItemActionListener(PushToApplication application) {
            this.application = application;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Change the selection:
            setSelected(application);
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
            if (toApp.getSettingsPanel() != null) {
                optPopup.show(pushButton, e.getX(), e.getY());
            }

        }
    }
}
