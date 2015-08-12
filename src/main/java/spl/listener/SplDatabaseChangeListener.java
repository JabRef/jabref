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
package spl.listener;

import net.sf.jabref.*;
import net.sf.jabref.external.DroppedFileHandler;
import net.sf.jabref.gui.MainTable;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 10.09.2010
 * Time: 14:02:55
 * To change this template use File | Settings | File Templates.
 */
public class SplDatabaseChangeListener implements DatabaseChangeListener {

    private final JabRefFrame frame;
    private final BasePanel panel;
    private final MainTable entryTable;
    private final String fileName;


    public SplDatabaseChangeListener(JabRefFrame frame, BasePanel panel, MainTable entryTable, String fileName) {
        this.frame = frame;
        this.panel = panel;
        this.entryTable = entryTable;
        this.fileName = fileName;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        if (e.getType() == DatabaseChangeEvent.ChangeType.ADDED_ENTRY) {
            DroppedFileHandler dfh = new DroppedFileHandler(frame, panel);
            dfh.linkPdfToEntry(fileName, entryTable, e.getEntry());
            panel.database().removeDatabaseChangeListener(this);
        }
    }
}
