package org.jabref.gui.contentselector;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import org.jabref.gui.util.BaseDialog;

public class ContentSelectorDialogView extends BaseDialog<Void> {

    @FXML private Button addFieldNameButton;
    @FXML private Button removeFieldNameButton;
    @FXML private Button addKeywordButton;
    @FXML private Button removeKeywordButton;
    @FXML private ListView<String> fieldNamesListView;
    @FXML private ListView<String> keywordsListView;
    @FXML private ButtonType saveButton;



}
