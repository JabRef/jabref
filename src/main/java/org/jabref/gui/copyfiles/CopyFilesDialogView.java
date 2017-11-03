package org.jabref.gui.copyfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public class CopyFilesDialogView extends AbstractDialogView {

    public CopyFilesDialogView(BibDatabaseContext bibDatabaseContext, CopyFilesResultListDependency results) {
        super(createContext(bibDatabaseContext, results));
    }

    @Override
    public void show() {
        FXDialog copyFilesResultDlg = new FXDialog(AlertType.INFORMATION, Localization.lang("Result"));
        copyFilesResultDlg.setResizable(true);
        copyFilesResultDlg.setDialogPane((DialogPane) this.getView());
        copyFilesResultDlg.show();
    }

    private static Function<String, Object> createContext(BibDatabaseContext bibDatabaseContext, CopyFilesResultListDependency copyfilesresultlistDependency) {
        Map<String, Object> context = new HashMap<>();
        //The "keys" of the HashMap must have the same name as the with @inject annotated field in the controller
        context.put("bibdatabasecontext", bibDatabaseContext);
        context.put("copyfilesresultlistDependency", copyfilesresultlistDependency);
        return context::get;
    }
}
