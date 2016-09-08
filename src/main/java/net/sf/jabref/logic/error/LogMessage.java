/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.logic.error;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * This class will be get the message and handle it with a typ, which will be define in {@link MessageType}
 * <ul>
 * <li>MessageType.LOG is define for log entries
 * <li>MessageType.OUTPUT is define for output entries
 * <li>MessageType.EXCEPTION is define for exception entries
 * </ul>
 */
public class LogMessage {

    private String message;
    private MessageType priority;
    private BooleanProperty isFiltered = new SimpleBooleanProperty();

    public LogMessage(String message, MessageType priority) {
        this.message = message;
        this.priority = priority;
        isFiltered.set(priority != MessageType.LOG);
    }

    public MessageType getPriority() {
        return priority;
    }

    public void setPriority(MessageType priority) {
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BooleanProperty isFilteredProperty() {
        return isFiltered;
    }

}
