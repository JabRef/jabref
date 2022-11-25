package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.tobiasdiez.easybind.EasyBind;

import static org.jabref.gui.mergeentries.newmergedialog.cell.ThreeWayMergeCell.HEADER_ROW;

public class ThreeWayMergeCellViewModel {
    private final StringProperty text = new SimpleStringProperty();
    private final BooleanProperty odd = new SimpleBooleanProperty(ThreeWayMergeCell.class, "odd");
    private final BooleanProperty even = new SimpleBooleanProperty(ThreeWayMergeCell.class, "even");

    public ThreeWayMergeCellViewModel(String text, int rowIndex) {
        setText(text);
        if (rowIndex != HEADER_ROW) {
            if (rowIndex % 2 == 1) {
                odd.setValue(true);
            } else {
                even.setValue(true);
            }
        }

        EasyBind.subscribe(odd, isOdd -> {
            setEven(!isOdd);
        });

        EasyBind.subscribe(even, isEven -> {
            setOdd(!isEven);
        });
    }

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public boolean isOdd() {
        return odd.get();
    }

    public BooleanProperty oddProperty() {
        return odd;
    }

    public void setOdd(boolean odd) {
        this.odd.set(odd);
    }

    public boolean isEven() {
        return even.get();
    }

    public BooleanProperty evenProperty() {
        return even;
    }

    public void setEven(boolean even) {
        this.even.set(even);
    }
}
