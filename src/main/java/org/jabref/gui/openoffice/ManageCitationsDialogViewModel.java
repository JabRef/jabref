package org.jabref.gui.openoffice;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.openoffice.CitationEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;

public class ManageCitationsDialogViewModel {

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
