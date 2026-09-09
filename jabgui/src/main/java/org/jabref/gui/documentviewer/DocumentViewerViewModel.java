package org.jabref.gui.documentviewer;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DocumentViewerViewModel extends AbstractViewModel {

    private final ObjectProperty<@Nullable Path> currentDocument = new SimpleObjectProperty<>(null);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final StringProperty highlightText = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper(Localization.lang("Document viewer"));

    public DocumentViewerViewModel() {
        this(null);
    }

    public DocumentViewerViewModel(@Nullable Path document) {
        currentDocument.addListener((_, _, newDocument) -> {
            if (newDocument != null) {
                title.set(Localization.lang("Document viewer - %0", newDocument.getFileName().toString()));
            } else {
                title.set(Localization.lang("Document viewer"));
            }
        });

        if (document != null) {
            showDocument(document);
        }
    }

    public ObjectProperty<@Nullable Path> currentDocumentProperty() {
        return currentDocument;
    }

    public Optional<Path> getCurrentDocument() {
        return Optional.ofNullable(currentDocument.get());
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public StringProperty highlightTextProperty() {
        return highlightText;
    }

    public ReadOnlyStringProperty titleProperty() {
        return title.getReadOnlyProperty();
    }

    public void showDocument(@Nullable Path document) {
        if (document != null && FileUtil.isPDFFile(document)) {
            currentDocument.set(document);
        } else {
            currentDocument.set(null);
        }
    }

    public void showPage(int pageNumber) {
        currentPage.set(Math.max(0, pageNumber - 1));
    }

    public void highlightText(String text) {
        highlightText.set(text);
    }

    public void reset() {
        currentDocument.set(null);
        currentPage.set(0);
        highlightText.set("");
    }
}
