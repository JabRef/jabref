package net.sf.jabref.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.jdesktop.swingx.JXTitledPanel;
import org.jdesktop.swingx.painter.MattePainter;

public abstract class SidePaneComponent extends JXTitledPanel {

    protected final JButton close = new JButton(IconTheme.JabRefIcon.CLOSE.getSmallIcon());

    protected final SidePaneManager manager;

    protected BasePanel panel;


    public SidePaneComponent(SidePaneManager manager, Icon icon, String title) {
        super(title);
        this.manager = manager;

        this.add(new JLabel(icon));

        setTitleForeground(new Color(79, 95, 143));
        setBorder(BorderFactory.createEmptyBorder());

        close.setMargin(new Insets(0, 0, 0, 0));
        close.setBorder(null);
        close.addActionListener(e -> hideAway());

        JButton up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());
        up.setMargin(new Insets(0, 0, 0, 0));
        up.setBorder(null);
        up.addActionListener(e -> moveUp());

        JButton down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
        down.setMargin(new Insets(0, 0, 0, 0));
        down.setBorder(null);
        down.addActionListener(e -> moveDown());

        JToolBar tlb = new OSXCompatibleToolbar();
        tlb.add(up);
        tlb.add(down);
        tlb.add(close);
        tlb.setOpaque(false);
        tlb.setFloatable(false);
        this.getUI().getTitleBar().add(tlb);
        setTitlePainter(new MattePainter(Color.lightGray));

    }

    private void hideAway() {
        manager.hideComponent(this);
    }

    private void moveUp() {
        manager.moveUp(this);
    }

    private void moveDown() {
        manager.moveDown(this);
    }

    public void setActiveBasePanel(BasePanel panel) {
        this.panel = panel;
    }

    public BasePanel getActiveBasePanel() {
        return panel;
    }

    /**
     * Override this method if the component needs to make any changes before it can close.
     */
    public void componentClosing() {
        // Nothing right now
    }

    /**
     * Override this method if the component needs to do any actions when opening.
     */
    public void componentOpening() {
        // Nothing right now
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public abstract String getSidePaneName();

    /**
     * Specifies how to distribute extra vertical space between side pane components.
     * 0: fixed height, 1: fill the remaining space
     */
    public abstract int getRescalingWeight();

    /**
     * @return the action which toggles this {@link SidePaneComponent}
     */
    public abstract ToggleAction getToggleAction();


    public class ToggleAction extends AbstractAction {

        public ToggleAction(String text, String description, KeyStroke key, IconTheme.JabRefIcon icon){
            super(text, icon.getSmallIcon());
            putValue(Action.ACCELERATOR_KEY, key);
            putValue(Action.LARGE_ICON_KEY, icon.getIcon());
            putValue(Action.SHORT_DESCRIPTION, description);
        }

        public ToggleAction(String text, String description, KeyStroke key, Icon icon){
            super(text, icon);
            putValue(Action.ACCELERATOR_KEY, key);
            putValue(Action.SHORT_DESCRIPTION, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!manager.hasComponent(getSidePaneName())) {
                manager.register(SidePaneComponent.this);
            }

            // if clicked by mouse just toggle
            if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
                manager.toggle(getSidePaneName());
            } else {
                manager.toggleThreeWay(getSidePaneName());
            }
            putValue(Action.SELECTED_KEY, manager.isComponentVisible(getSidePaneName()));
        }

        public void setSelected(boolean selected){
            putValue(Action.SELECTED_KEY, selected);
        }

        public boolean isSelected() {
            return Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
        }

    }

}
