package net.sf.jabref.help;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

@SuppressWarnings("serial")
public class MyEditorKit extends LargeHTMLEditorKit {

    public class MyNextVisualPositionAction extends TextAction {
        private Action textActn;

        private int direction;

        private MyNextVisualPositionAction(Action textActn, int direction) {
            super((String) textActn.getValue(Action.NAME));
            this.textActn = textActn;
            this.direction = direction;
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent c = getTextComponent(e);

            if (c.getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) c.getParent();
                Point p = viewport.getViewPosition();

                if (this.direction == SwingConstants.NORTH) {
                    c.setCaretPosition(c.viewToModel(p));
                } else {
                    p.y += viewport.getExtentSize().height;
                    c.setCaretPosition(c.viewToModel(p));
                }
            }

            textActn.actionPerformed(e);
        }
    }

    private Action[] myActions;

    public Action[] getActions() {
        if (myActions == null) {
            Action[] actions = super.getActions();
            Action[] newActions = new Action[2];

            for (int i = 0; i < actions.length; i++) {
                Action actn = actions[i];

                String name = (String) actn.getValue(Action.NAME);

                if (name.equals(DefaultEditorKit.upAction)) {
                    newActions[0] = new MyNextVisualPositionAction(actions[i],
                        SwingConstants.NORTH);
                } else if (name.equals(DefaultEditorKit.downAction)) {
                    newActions[1] = new MyNextVisualPositionAction(actions[i],
                        SwingConstants.SOUTH);
                }
            }

            myActions = TextAction.augmentList(actions, newActions);
        }

        return myActions;
    }
}
