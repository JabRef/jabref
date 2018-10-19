package org.jabref.gui.exporter;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;

public class ExportCustomizationDialogViewModel extends BaseDialog<Void> {

    private final DialogService dialogService;


    //stuff here

    public ExportCustomizationDialogViewModel(DialogService dialogService) {
        this.dialogService = dialogService;

    }
}