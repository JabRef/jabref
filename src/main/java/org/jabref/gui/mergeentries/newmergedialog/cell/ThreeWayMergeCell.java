package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;

/**
 *
 */
public abstract class ThreeWayMergeCell extends HBox {
    public static final String ODD_PSEUDO_CLASS = "odd";
    public static final String EVEN_PSEUDO_CLASS = "even";
    public static final int HEADER_ROW = -1;
    private static final String DEFAULT_STYLE_CLASS = "field-cell";
    private final StringProperty text = new SimpleStringProperty();
    private final BooleanProperty odd = new BooleanPropertyBase() {
        @Override
        public Object getBean() {
            return ThreeWayMergeCell.this;
        }

        @Override
        public String getName() {
            return "odd";
        }

        @Override
        protected void invalidated() {
            pseudoClassStateChanged(PseudoClass.getPseudoClass(ODD_PSEUDO_CLASS), get());
            pseudoClassStateChanged(PseudoClass.getPseudoClass(EVEN_PSEUDO_CLASS), !get());
        }
    };

    private final BooleanProperty even = new BooleanPropertyBase() {
        @Override
        public Object getBean() {
            return ThreeWayMergeCell.this;
        }

        @Override
        public String getName() {
            return "even";
        }

        @Override
        protected void invalidated() {
            pseudoClassStateChanged(PseudoClass.getPseudoClass(EVEN_PSEUDO_CLASS), get());
            pseudoClassStateChanged(PseudoClass.getPseudoClass(ODD_PSEUDO_CLASS), !get());
        }
    };

    public ThreeWayMergeCell(String text, int rowIndex) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        if (rowIndex != HEADER_ROW) {
            if (rowIndex % 2 == 1) {
                odd.setValue(true);
            } else {
                even.setValue(true);
            }
        }

        setPadding(new Insets(8));

        setText(text);
    }

    public String getText() {
        return textProperty().get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public void setText(String text) {
        textProperty().set(text);
    }
}
