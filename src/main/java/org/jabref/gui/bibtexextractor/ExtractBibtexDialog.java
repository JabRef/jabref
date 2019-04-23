package org.jabref.gui.bibtexextractor;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.EntryType;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    private final JabRefFrame frame;
    private TextArea textArea;
    private Button buttonExtract;

    public ExtractBibtexDialog(JabRefFrame frame) {
        super();
        this.setTitle(Localization.lang("Input text to parse"));
        this.frame = frame;

       initialize();
    }

    private void initialize(){
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.textProperty()
                .addListener((observable, oldValue, newValue) -> buttonExtract.setDisable(newValue.isEmpty()));

        VBox container = new VBox(20);
        container.getChildren().addAll(
                textArea);
        container.setPrefWidth(600);

        ButtonType buttonTypeGenerate = new ButtonType(Localization.lang("Extract"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(
                buttonTypeGenerate,
                ButtonType.CANCEL
        );

        buttonExtract = (Button) getDialogPane().lookupButton(buttonTypeGenerate);
        buttonExtract.setTooltip(new Tooltip((Localization.lang("Starts the extraction of the BibTeX entry"))));
        buttonExtract.setDisable(true);
        buttonExtract.setOnAction(e -> startExtraction());

        getDialogPane().setContent(container);
    }

    private void startExtraction()
    {
        BibtexExtractor extractor = new BibtexExtractor();
        BibEntry entity = extractor.Extract(textArea.getText());
        trackNewEntry(BiblatexEntryTypes.ARTICLE);
        frame.getCurrentBasePanel().insertEntry(entity);
    }

    private void trackNewEntry(EntryType type) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", type.getName());

        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
    }
}
