/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.undo;

import java.util.Objects;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.undo.AddUndoEvent;
import net.sf.jabref.logic.undo.UndoRedoEvent;

import com.google.common.eventbus.EventBus;

public class CountingUndoManager extends UndoManager {

    private int unchangedPoint;
    private int current;
    private final BasePanel panel;

    private final EventBus eventBus = new EventBus();


    public CountingUndoManager(BasePanel basePanel) {
        super();
        panel = Objects.requireNonNull(basePanel);
        eventBus.post(new UndoRedoEvent());
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        current++;
        boolean returnvalue = super.addEdit(edit);
        eventBus.post(new AddUndoEvent());
        return returnvalue;
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
        current--;
        eventBus.post(new UndoRedoEvent());
    }

    @Override
    public synchronized void redo() throws CannotUndoException {
        super.redo();
        current++;
        eventBus.post(new UndoRedoEvent());
        panel.updateEntryEditorIfShowing();
    }

    public synchronized void markUnchanged() {
        unchangedPoint = current;
    }

    public synchronized boolean hasChanged() {
        return (current != unchangedPoint);
    }


    public void registerListener(Object object) {
        this.eventBus.register(object);
    }

    public void unregisterListener(Object object) {
        this.eventBus.unregister(object);
    }

    public void triggerEvent() {
        eventBus.post(new UndoRedoEvent());
    }
}
