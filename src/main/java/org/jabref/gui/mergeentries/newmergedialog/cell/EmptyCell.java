package org.jabref.gui.mergeentries.newmergedialog.cell;

public class EmptyCell extends AbstractCell {
    public EmptyCell(String styleClass, int rowIndex) {
        super("", rowIndex);
        getStyleClass().add(styleClass);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public EmptyCell(String styleClass) {
        this(styleClass, AbstractCell.NO_ROW_NUMBER);
    }
}
