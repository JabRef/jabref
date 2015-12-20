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
package net.sf.jabref.exporter;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Exception thrown if saving goes wrong. If caused by a specific
 * entry, keeps track of which entry caused the problem.
 */
public class SaveException extends Exception {

    public static final SaveException FILE_LOCKED = new SaveException(
            "Could not save, file locked by another JabRef instance.",
            Localization.lang("Could not save, file locked by another JabRef instance."));
    public static final SaveException BACKUP_CREATION = new SaveException("Unable to create backup",
            Localization.lang("Unable to create backup"));

    private final BibEntry entry;
    private int status;
    private String localizedMessage;

    public SaveException(String message) {
        super(message);
        entry = null;
    }

    public SaveException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
        entry = null;
    }

    public SaveException(String message, int status) {
        super(message);
        entry = null;
        this.status = status;
    }

    public SaveException(String message, BibEntry entry) {
        super(message);
        this.entry = entry;
    }

    public SaveException(String message, String localizedMessage, BibEntry entry) {
        super(message);
        this.localizedMessage = localizedMessage;
        this.entry = entry;
    }

    public int getStatus() {
        return status;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public boolean specificEntry() {
        return entry != null;
    }

    @Override
    public String getLocalizedMessage() {
        if (localizedMessage == null) {
            return getMessage();
        } else {
            return localizedMessage;
        }
    }
}
