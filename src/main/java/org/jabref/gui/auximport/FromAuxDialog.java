package org.jabref.gui.auximport;

import java.nio.file.Paths;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelPreferences;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * A wizard dialog for generating a new sub database from existing TeX AUX file
 */
public class FromAuxDialog extends BaseDialog<Void> {

    private final DialogService dialogService;
    private final BasePanel basePanel;
    @FXML private ButtonType generateButtonType;
    private Button generateButton;
    @FXML private TextField auxFileField;
    @FXML private ListView<String> notFoundList;

    private AuxParserResult auxParserResult;
    @FXML private TextArea statusInfos;

    public FromAuxDialog(JabRefFrame frame) {
        basePanel = frame.getCurrentBasePanel();
        dialogService = frame.getDialogService();

        this.setTitle(Localization.lang("AUX file import"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        generateButton = (Button) this.getDialogPane().lookupButton(generateButtonType);
        generateButton.setDisable(true);
        generateButton.defaultButtonProperty().bind(generateButton.disableProperty().not());
        setResultConverter(button -> {
            if (button == generateButtonType) {
                BasePanel bp = new BasePanel(frame, BasePanelPreferences.from(Globals.prefs), new BibDatabaseContext(auxParserResult.getGeneratedBibDatabase()), ExternalFileTypes.getInstance());
                frame.addTab(bp, true);
            }
            return null;
        });
    }

    @FXML
    private void parseActionPerformed() {
        notFoundList.getItems().clear();
        statusInfos.setText("");
        BibDatabase refBase = basePanel.getDatabase();
        String auxName = auxFileField.getText();

        if ((auxName != null) && (refBase != null) && !auxName.isEmpty()) {
            AuxParser auxParser = new DefaultAuxParser(refBase);
            auxParserResult = auxParser.parse(Paths.get(auxName));
            notFoundList.getItems().setAll(auxParserResult.getUnresolvedKeys());
            statusInfos.setText(new AuxParserResultViewModel(auxParserResult).getInformation(false));

            if (!auxParserResult.getGeneratedBibDatabase().hasEntries()) {
                // The generated database contains no entries -> no active generate-button
                statusInfos.setText(statusInfos.getText() + "\n" + Localization.lang("empty library"));
                generateButton.setDisable(true);
            } else {
                generateButton.setDisable(false);
            }
        } else {
            generateButton.setDisable(true);
        }
    }

    @FXML
    private void browseButtonClicked() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> auxFileField.setText(file.toAbsolutePath().toString()));
    }

}
