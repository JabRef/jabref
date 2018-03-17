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
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

/**
 * Customized UI component for pushing to external applications. Has a selection popup menu to change the selected
 * external application. This class implements the ActionListener interface. When actionPerformed() is invoked, the
 * currently selected PushToApplication is activated. The actionPerformed() method can be called with a null argument.
 */
public class PushToApplicationButton extends SimpleCommand implements ActionListener {

    private static final Icon ARROW_ICON = IconTheme.JabRefIcons.DOWN.getSmallIcon();
    private final JabRefFrame frame;
    private final List<PushToApplication> pushActions;
    private JPanel comp;
    private JButton pushButton;
    private PushToApplication toApp;
    private JPopupMenu popup;
    private final Map<PushToApplication, PushToApplicationAction> actions = new HashMap<>();
    private final Dimension buttonDim = new Dimension(23, 23);
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
        toApp = getLastUsedApplication();

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
                PushToApplicationSettingsDialog.showSettingsDialog(null, toApp, options);
            }
        });

        buildPopupMenu();
    }

    private PushToApplication getLastUsedApplication() {
        String appSelected = Globals.prefs.get(JabRefPreferences.PUSH_TO_APPLICATION);
        for (PushToApplication application : pushActions) {
            if (application.getApplicationName().equals(appSelected)) {
                return application;
            }
        }

        // Nothing found, pick first
        return pushActions.get(0);
    }

    /**
     * Create a selection menu for the available "Push" options.
     */
    private void buildPopupMenu() {
        popup = new JPopupMenu();
        for (PushToApplication application : pushActions) {
            JMenuItem item = new JMenuItem(application.getApplicationName(), application.getIcon().getIcon());
            item.setToolTipText(application.getTooltip());
            item.addActionListener(new PopupItemActionListener(application));
            popup.add(item);
        }
    }

    /**
     * Update the PushButton to default to the given application.
     *
     * @param newApplication the application to default to
     */
    private void setSelected(PushToApplication newApplication) {
        toApp = newApplication;
        setSelected();
    }

    private void setSelected() {
        pushButton.setIcon(toApp.getIcon().getIcon());
        pushButton.setToolTipText(toApp.getTooltip());
        pushButton.setPreferredSize(buttonDim);

        // Store the last used application
        Globals.prefs.put(JabRefPreferences.PUSH_TO_APPLICATION, toApp.getApplicationName());
    }

    /**
     * Get the toolbar component for the push button.
     *
     * @return The component.
     */
    public Component getComponent() {
        return comp;
    }

    public org.jabref.gui.actions.Action getMenuAction() {
        PushToApplication application = getLastUsedApplication();

        return new org.jabref.gui.actions.Action() {
            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(application.getIcon());
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
            }

            @Override
            public String getText() {
                return Localization.menuTitle("Push entries to external application (%0)", application.getApplicationName());
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    @Override
    public void execute() {
        // Lazy initialization of the push action:
        PushToApplicationAction action = actions.get(toApp);
        if (action == null) {
            action = new PushToApplicationAction(frame, toApp);
            actions.put(toApp, action);
        }
        action.actionPerformed(new ActionEvent(toApp, 0, "push"));
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
