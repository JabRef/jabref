package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;

import com.tobiasdiez.easybind.EasyBind;

/**
 *
 */
public abstract class ThreeWayMergeCell extends HBox {
    public static final String ODD_PSEUDO_CLASS = "odd";
    public static final String EVEN_PSEUDO_CLASS = "even";
    public static final int HEADER_ROW = -1;
    private static final String DEFAULT_STYLE_CLASS = "field-cell";

    private final ThreeWayMergeCellViewModel viewModel;

    public ThreeWayMergeCell(String text, int rowIndex) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        viewModel = new ThreeWayMergeCellViewModel(text, rowIndex);

        EasyBind.subscribe(viewModel.oddProperty(), isOdd -> {
            pseudoClassStateChanged(PseudoClass.getPseudoClass(ODD_PSEUDO_CLASS), isOdd);
        });
        EasyBind.subscribe(viewModel.evenProperty(), isEven -> {
            pseudoClassStateChanged(PseudoClass.getPseudoClass(EVEN_PSEUDO_CLASS), isEven);
        });

        setPadding(new Insets(8));
    }

    public String getText() {
        return viewModel.getText();
    }

    public StringProperty textProperty() {
        return viewModel.textProperty();
    }

    public void setText(String text) {
        viewModel.setText(text);
    }
}
