package org.jabref.gui.importer.actions;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;

public class ResultHelper {

    private final ParserResult parserResult;
    private final BasePanel basePanel;
    private final DialogService dialogService;

    public ResultHelper(ParserResult parserResult, BasePanel basePanel, DialogService dialogService) {
        this.parserResult = parserResult;
        this.basePanel = basePanel;
        this.dialogService = dialogService;
    }

    public ParserResult getParserResult() {
        return parserResult;
    }

    public BasePanel getBasePanel() {
        return basePanel;
    }

    public DialogService getDialogService() {
        return dialogService;
    }
}
