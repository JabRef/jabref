package org.jabref.gui.renderer;

import java.awt.Color;

public class CompleteRenderer extends GeneralRenderer {

    public CompleteRenderer(Color color) {
        super(color);
    }

    public void setNumber(int number) {
        super.setValue(String.valueOf(number + 1));
    }

    @Override
    protected void setValue(Object value) {
        // do not support normal behaviour
    }
}
