/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.gui.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Constructs a {@link TableCell} based on the value of the cell and a bunch of specified converter methods.
 *
 * @param <S> view model
 * @param <T> cell value
 */
public class ValueTableCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;

    public ValueTableCellFactory<S, T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ValueTableCellFactory<S, T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ValueTableCellFactory<S, T> withOnMouseClickedEvent(
            Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {

        return new TableCell<S, T>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                } else {
                    if (toText != null) {
                        setText(toText.call(item));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.call(item));
                    }
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(toOnMouseClickedEvent.call(item));
                    }
                }
            }
        };
    }
}
