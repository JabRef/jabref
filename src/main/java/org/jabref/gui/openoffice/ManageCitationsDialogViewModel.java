package org.jabref.gui.openoffice;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.openoffice.CitationEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;

public class ManageCitationsDialogViewModel {

    private final ListProperty<ManageCitationsItemViewModel> citations = new SimpleListProperty<>(FXCollections.observableArrayList());

    public ManageCitationsDialogViewModel(OOBibBase ooBase, DialogService dialogService) throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {

        XNameAccess nameAccess = ooBase.getReferenceMarks();
        List<String> names = ooBase.getJabRefReferenceMarks(nameAccess);
        for (String name : names) {
            new CitationEntry(name,
                              "<html>..." + ooBase.getCitationContext(nameAccess, name, 30, 30, true) + "...</html>",
                              ooBase.getCustomProperty(name));
        }
    }

}
