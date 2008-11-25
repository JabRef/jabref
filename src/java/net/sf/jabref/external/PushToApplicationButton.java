package net.sf.jabref.external;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Customized UI component for pushing to external applications. Has a selection popup
 * menu to change the selected external application.
 * This class implements the ActionListener interface. When actionPerformed() is
 * invoked, the currently selected PushToApplication is activated. The actionPerformed()
 * method can be called with a null argument.
 */
public class PushToApplicationButton implements ActionListener {

    public static List<PushToApplication> applications;

    private JabRefFrame frame;
    private List<PushToApplication> pushActions;
    private JPanel comp;
    private JButton pushButton, menuButton;
    private int selected = 0;
    private JPopupMenu popup = null;
    private HashMap<PushToApplication, PushToApplicationAction> actions = new HashMap<PushToApplication, PushToApplicationAction>();
    private final Dimension buttonDim = new Dimension(23, 23);
    private static final URL ARROW_ICON = GUIGlobals.class.getResource("/images/secondary_sorted_reverse.png");
    private MenuAction mAction = new MenuAction();
    private JPopupMenu optPopup = new JPopupMenu();
    private JMenuItem settings = new JMenuItem(Globals.lang("Settings"));

    /**
     * Set up the current available choices:
     */
    static {

        applications = new ArrayList<PushToApplication>();

        JabRefPlugin jabrefPlugin = JabRefPlugin.getInstance(PluginCore.getManager());
        List<_JabRefPlugin.PushToApplicationExtension> plugins = jabrefPlugin.getPushToApplicationExtensions();
        for (_JabRefPlugin.PushToApplicationExtension extension : plugins) {
            applications.add(extension.getPushToApp());
        }

        applications.add(new PushToLyx());
        applications.add(new PushToEmacs());
        applications.add(new PushToWinEdt());
        applications.add(new PushToLatexEditor());
        applications.add(new PushToVim());

        // Finally, sort the entries:
        //Collections.sort(applications, new PushToApplicationComparator());
    }


    public PushToApplicationButton(JabRefFrame frame, List<PushToApplication> pushActions) {
        this.frame = frame;
        this.pushActions = pushActions;
        init();
    }

    private void init() {
        comp = new JPanel();
        comp.setLayout(new BorderLayout());

        menuButton = new JButton(new ImageIcon(ARROW_ICON));
        menuButton.setMargin(new Insets(0,0,0,0));
        menuButton.setPreferredSize(new Dimension(menuButton.getIcon().getIconWidth(),
                menuButton.getIcon().getIconHeight()));
        menuButton.addActionListener(new MenuButtonActionListener());
        menuButton.setToolTipText(Globals.lang("Select external application"));
        pushButton = new JButton();
        if (Globals.prefs.hasKey("pushToApplication")) {
            String appSelected = Globals.prefs.get("pushToApplication");
            for (int i=0; i<pushActions.size(); i++) {
                PushToApplication toApp = pushActions.get(i);
                if (toApp.getName().equals(appSelected)) {
                    selected = i;
                    break;
                }
            }
        }

        setSelected(selected);
        pushButton.addActionListener(this);
        pushButton.addMouseListener(new PushButtonMouseListener());

        comp.add(pushButton, BorderLayout.CENTER);
        comp.add(menuButton, BorderLayout.EAST);
        comp.setBorder(BorderFactory.createLineBorder(Color.gray));
        comp.setMaximumSize(comp.getPreferredSize());

        optPopup.add(settings);
        settings.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                PushToApplication toApp = pushActions.get(selected);
                JPanel options = toApp.getSettingsPanel();
                if (options != null) {
                    showSettingsDialog(frame, toApp, options);
                }

            }
        });

        buildPopupMenu();
    }

    /**
     * Create a selection menu for the available "Push" options.
     */
    private void buildPopupMenu() {
        popup = new JPopupMenu();
        int j=0;
        for (PushToApplication application : pushActions){
            JMenuItem item = new JMenuItem(application.getApplicationName(),
                    application.getIcon());
            item.setToolTipText(application.getTooltip());
            item.addActionListener(new PopupItemActionListener(j));
            popup.add(item);
            j++;
        }
    }

    /**
     * Update the PushButton to default to the given application.
     * @param i The List index of the application to default to.
     */
    private void setSelected(int i) {
        this.selected = i;
        PushToApplication toApp = pushActions.get(i);
        pushButton.setIcon(toApp.getIcon());
        pushButton.setToolTipText(toApp.getTooltip());
        pushButton.setPreferredSize(buttonDim);

        Globals.prefs.put("pushToApplication", toApp.getName());
        mAction.setTitle(toApp.getApplicationName());
    }

    /**
     * Get the toolbar component for the push button.
     * @return The component.
     */
    public Component getComponent() {
       return comp;
    }

    public Action getMenuAction() {
        return mAction;
    }

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
        public BooleanHolder(boolean value) {
            this.value = value;
        }
        public boolean value;
    }

    public static void showSettingsDialog(Object parent, PushToApplication toApp, JPanel options) {
        
        final BooleanHolder okPressed = new BooleanHolder(false);
        JDialog dg;
        if (parent instanceof JDialog)
            dg = new JDialog((JDialog)parent, Globals.lang("Settings"), true);
        else
            dg = new JDialog((JFrame)parent, Globals.lang("Settings"), true);
        final JDialog diag = dg;
        options.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.getContentPane().add(options, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton ok = new JButton(Globals.lang("Ok"));
        JButton cancel = new JButton(Globals.lang("Cancel"));
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                okPressed.value = true;
                diag.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                diag.dispose();
            }
        });
        // Key bindings:
        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        });
        diag.pack();
        if (parent instanceof JDialog)
            diag.setLocationRelativeTo((JDialog)parent);
        else
            diag.setLocationRelativeTo((JFrame)parent);
        // Show the dialog:
        diag.setVisible(true);
        // If the user pressed Ok, ask the PushToApplication implementation
        // to store its settings:
        if (okPressed.value) {
            toApp.storeSettings();
        }
    }

    class PopupItemActionListener implements ActionListener {
        private int index;
        public PopupItemActionListener(int index) {
            this.index = index;
        }

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


    class MenuButtonActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            // Lazy initialization of the popup menu:
            if (popup == null)
                buildPopupMenu();
            popup.show(comp, 0, menuButton.getHeight());
        }
    }

    class MenuAction extends MnemonicAwareAction {

        public MenuAction() {
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Push to application"));
        }

        public void setTitle(String appName) {
            putValue(NAME, Globals.lang("Push entries to external application (%0)",
                    appName));
        }

        public void actionPerformed(ActionEvent e) {
            PushToApplicationButton.this.actionPerformed(null);
        }
    }

    class PushButtonMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent event) {
            if (event.isPopupTrigger())
                processPopupTrigger(event);
        }

        public void mouseClicked(MouseEvent event) {
            if (event.isPopupTrigger())
                processPopupTrigger(event);
        }

        public void mouseReleased(MouseEvent event) {
            if (event.isPopupTrigger())
                processPopupTrigger(event);
        }

        private void processPopupTrigger(MouseEvent e) {
            // We only want to show the popup if a settings panel exists for the selected
            // item:
            PushToApplication toApp = pushActions.get(selected);
            if (toApp.getSettingsPanel() != null)
                optPopup.show(pushButton, e.getX(), e.getY());

        }
    }

    /**
     * Comparator for sorting the selection according to name.
     */
    static class PushToApplicationComparator implements Comparator<PushToApplication> {

        public int compare(PushToApplication one, PushToApplication two) {
            return one.getName().compareTo(two.getName());
        }
    }
}
