package org.jabref.gui.util;

import java.util.EnumSet;

import javafx.geometry.Pos;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class RadioButtonCell<S, T extends Enum<T>> extends TableCell<S, T> {

    private final EnumSet<T> enumeration;

    public RadioButtonCell(EnumSet<T> enumeration) {
        this.enumeration = enumeration;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
            setGraphic(null);
        } else {
            // gui setup
            HBox hb = new HBox(7);
            hb.setAlignment(Pos.CENTER);
            final ToggleGroup group = new ToggleGroup();

            // create a radio button for each 'element' of the enumeration
            for (Enum<T> enumElement : enumeration) {
                RadioButton radioButton = new RadioButton(enumElement.toString());
                radioButton.setUserData(enumElement);
                radioButton.setToggleGroup(group);
                radioButton.setMinWidth(USE_PREF_SIZE);
                hb.getChildren().add(radioButton);
                if (enumElement.equals(item)) {
                    radioButton.setSelected(true);
                }
                hb.setMinWidth(USE_PREF_SIZE);
                hb.setHgrow(radioButton, Priority.ALWAYS);
            }

            // issue events on change of the selected radio button
            group.selectedToggleProperty().addListener((oservable, oldValue, newValue) -> {
                getTableView().edit(getIndex(), getTableColumn());
                RadioButtonCell.this.commitEdit((T) newValue.getUserData());
            });

            setGraphic(hb);
        }
    }
}
