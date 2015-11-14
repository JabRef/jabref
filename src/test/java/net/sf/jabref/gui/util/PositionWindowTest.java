package net.sf.jabref.gui.util;

import java.awt.Container;
import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.JWindow;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PositionWindowTest {

    @Test
    @Ignore
    public void testPlaceDialog() {
        Dialog d = new JDialog();
        d.setSize(50, 50);
        Container c = new JWindow();
        c.setBounds(100, 200, 100, 50);

        PositionWindow.placeDialog(d, c);
        Assert.assertEquals(125, d.getX());
        Assert.assertEquals(200, d.getY());

        // Test upper left corner
        c.setBounds(0, 0, 100, 100);
        d.setSize(200, 200);

        PositionWindow.placeDialog(d, c);
        Assert.assertEquals(0, d.getX());
        Assert.assertEquals(0, d.getY());
    }

}
