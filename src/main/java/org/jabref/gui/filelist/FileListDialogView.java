package org.jabref.gui.filelist;

import javafx.scene.control.Alert.AlertType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;

public class FileListDialogView extends AbstractDialogView {

    public FileListDialogView(FileListDialogOptions... options) {
        this(getDeps(options));
    }

    public FileListDialogView(FileListDialogOptionDependency deps) {
        super(createContext(deps));
    }

    @Override
    public void show() {
        FXDialog filelistDialog = new FXDialog(AlertType.INFORMATION, "FileListDialog ");
        filelistDialog.setDialogPane((DialogPane) this.getView());
        filelistDialog.setResizable(true);
        filelistDialog.show();

    }

    public boolean okPressed() {
        FileListDialogViewModel viewModel = (FileListDialogViewModel) this.getController().get().getViewModel();
        return viewModel.isOkPressed();
    }

    private static Function<String, Object> createContext(FileListDialogOptionDependency dependency) {
        Map<String, Object> context = new HashMap<>();

        //The "keys" of the HashMap must have the same name as the with @inject annotated field in the controller
        context.put("dependency", dependency);

        return context::get;
    }

    private static FileListDialogOptionDependency getDeps(FileListDialogOptions... options) {
        EnumSet<FileListDialogOptions> set = EnumSet.noneOf(FileListDialogOptions.class);
        set.addAll(Arrays.asList(options));
        FileListDialogOptionDependency deps = new FileListDialogOptionDependency();
        deps.setDialogOptions(set);
        return deps;
    }
}
