package net.sf.jabref.gui.prrv;

import javafx.event.Event;

import prefux.FxDisplay;
import prefux.controls.ControlAdapter;
import prefux.data.Table;
import prefux.data.event.TableListener;
import prefux.visual.VisualItem;


/**
 * Created by Daniel on 10/13/2016.
 */
public class DisplayControl extends ControlAdapter implements TableListener {


    @Override
    public void tableChanged(Table table, int i, int i1, int i2, int i3) {


    }

    @Override
    public void itemEvent(VisualItem item, Event e) {

        System.out.println(item.get("bibtexkey").toString());
    }

    public void changes(FxDisplay fdx) {

    }
}
