package org.jabref.gui.entryeditor;

import java.util.Optional;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.MathSciNetId;

public class MathSciNetTab extends EntryEditorTab {

    public MathSciNetTab() {
        setText(Localization.lang("MathSciNet Review"));
    }

    private Optional<MathSciNetId> getMathSciNetId(BibEntry entry) {
        return entry.getField(FieldName.MR_NUMBER).flatMap(MathSciNetId::parse);
    }

    private StackPane getPane(BibEntry entry) {
        StackPane root = new StackPane();
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(100, 100);
        WebView browser = new WebView();

        // Quick hack to disable navigating
        browser.addEventFilter(javafx.scene.input.MouseEvent.ANY, javafx.scene.input.MouseEvent::consume);
        browser.setContextMenuEnabled(false);

        root.getChildren().addAll(browser, progress);

        Optional<MathSciNetId> mathSciNetId = getMathSciNetId(entry);
        mathSciNetId.flatMap(MathSciNetId::getExternalURI)
                .ifPresent(url -> browser.getEngine().load(url.toASCIIString()));

        // Hide progress indicator if finished (over 70% loaded)
        browser.getEngine().getLoadWorker().progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.7) {
                progress.setVisible(false);
            }
        });
        return root;
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return getMathSciNetId(entry).isPresent();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setContent(getPane(entry));
    }
}
