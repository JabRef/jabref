package org.jabref.gui.maintable.columns;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableColumnModel.Type;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;

public class LibraryColumn extends MainTableColumn<String> {

    public LibraryColumn(MainTableColumnModel model) {
        super(model);

        setText(Localization.lang("Library"));
        new ValueTableCellFactory<BibEntryTableViewModel, String>().withText(FileUtil::getBaseName)
                                                                   .install(this);
        setCellValueFactory(param -> param.getValue().bibDatabaseContextProperty());
    }

    public LibraryColumn() {
        this(new MainTableColumnModel(Type.LIBRARY_NAME));
    }

}
