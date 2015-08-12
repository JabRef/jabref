/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.help;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

@SuppressWarnings("serial")
class MyEditorKit extends LargeHTMLEditorKit {

    public static class MyNextVisualPositionAction extends TextAction {

        private final Action textActn;

        private final int direction;


        private MyNextVisualPositionAction(Action textActn, int direction) {
            super((String) textActn.getValue(Action.NAME));
            this.textActn = textActn;
            this.direction = direction;
        }

        @Override
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


    @Override
    public Action[] getActions() {
        if (myActions == null) {
            Action[] actions = super.getActions();
            Action[] newActions = new Action[2];

            for (Action actn : actions) {
                String name = (String) actn.getValue(Action.NAME);

                if (name.equals(DefaultEditorKit.upAction)) {
                    newActions[0] = new MyNextVisualPositionAction(actn,
                            SwingConstants.NORTH);
                } else if (name.equals(DefaultEditorKit.downAction)) {
                    newActions[1] = new MyNextVisualPositionAction(actn,
                            SwingConstants.SOUTH);
                }
            }

            myActions = TextAction.augmentList(actions, newActions);
        }

        return myActions;
    }
}
