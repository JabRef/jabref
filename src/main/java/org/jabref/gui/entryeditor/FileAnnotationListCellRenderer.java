package org.jabref.gui.entryeditor;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import org.jabref.model.pdf.FileAnnotation;

public class FileAnnotationListCellRenderer implements Callback<ListView<FileAnnotation>, ListCell<FileAnnotation>> {

    @Override
    public ListCell<FileAnnotation> call(ListView<FileAnnotation> param) {
        return new ListCell<FileAnnotation>() {
            @Override
            protected void updateItem(FileAnnotation t, boolean bln) {
                super.updateItem(t, bln);
                if (t != null) {
                    setText(t.getContent());
                }
            }
        };
    }
}
