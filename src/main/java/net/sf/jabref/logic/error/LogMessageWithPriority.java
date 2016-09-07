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
 * This class will be get the message and handle it with a priority, which will be define in {@link MessagePriority}
 * <ul>
 * <li>MessagePriority.LOW is define for log entries
 * <li>MessagePriority.MEDIUM is define for output entries
 * <li>MessagePriority.HIGH is define for exception entries
 * </ul>
 */
public class LogMessageWithPriority {

    private String message;
    private MessagePriority priority;
    private BooleanProperty isFiltered = new SimpleBooleanProperty();

    public LogMessageWithPriority(String message, MessagePriority priority) {
        this.message = message;
        this.priority = priority;
        isFiltered.set(priority != MessagePriority.LOW);
    }

    public MessagePriority getPriority() {
        return priority;
    }

    public void setPriority(MessagePriority priority) {
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setIsFiltered(boolean isFiltered) {
        this.isFiltered.set(isFiltered);
    }

    public BooleanProperty isFilteredProperty() {
        return isFiltered;
    }

}
