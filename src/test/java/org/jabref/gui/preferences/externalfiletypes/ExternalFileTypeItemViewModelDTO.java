package org.jabref.gui.preferences.externalfiletypes;

public class ExternalFileTypeItemViewModelDTO {
    private ExternalFileTypeItemViewModel externalFileTypeItemViewModel = new ExternalFileTypeItemViewModel();

    public void setup(){
        externalFileTypeItemViewModel.nameProperty().set("Excel 2007");
        externalFileTypeItemViewModel.extensionProperty().set("xlsx");
        externalFileTypeItemViewModel.mimetypeProperty().set("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        externalFileTypeItemViewModel.applicationProperty().set("oocalc");
    }
    public void setupWithoutName(){
        externalFileTypeItemViewModel.nameProperty().set("");
    }
    public ExternalFileTypeItemViewModel get(){
        return externalFileTypeItemViewModel;
    };

    public void clone(ExternalFileTypeItemViewModel updatedModel){
        updatedModel.nameProperty().set(externalFileTypeItemViewModel.getName());
        updatedModel.extensionProperty().set(externalFileTypeItemViewModel.extensionProperty().get());
        updatedModel.mimetypeProperty().set(externalFileTypeItemViewModel.mimetypeProperty().get());
        updatedModel.applicationProperty().set(externalFileTypeItemViewModel.applicationProperty().get());
    }

    public boolean isSameValue(ExternalFileTypeItemViewModel item) {
        if (!item.getName().equals(externalFileTypeItemViewModel.getName())
            || !item.extensionProperty().get().equals(externalFileTypeItemViewModel.extensionProperty().get())
            || !item.mimetypeProperty().get().equals(externalFileTypeItemViewModel.mimetypeProperty().get())
            || !item.applicationProperty().get().equals(externalFileTypeItemViewModel.applicationProperty().get())
        )
            return false;
        else
            return true;
    }
}
