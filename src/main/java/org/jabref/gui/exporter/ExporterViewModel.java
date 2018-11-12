package org.jabref.gui.exporter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.exporter.TemplateExporter;

public class ExporterViewModel {

    private final TemplateExporter exporter; //Should this be final?  Eclipse says so
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty lfFileName = new SimpleStringProperty();
    //If the TemplateExporter
    private final StringProperty extension = new SimpleStringProperty();

    public ExporterViewModel(TemplateExporter exporter) {
        this.exporter = exporter;
        this.name.setValue(exporter.getName());
        this.lfFileName.setValue(exporter.getLayoutFileNameWithExtension());
        //This should return at least one of the extensions, but may need to be changed to return the most common extension
        //Removes the asterisk with substring(1)
        String extensionString = exporter.getFileType().getExtensionsWithDot().get(0).substring(1);
        this.extension.setValue(extensionString);
    }

    public TemplateExporter getLogic() {
        return this.exporter;
    }

    public StringProperty getName() {
        return this.name;

    }

    public StringProperty getLayoutFileName() {
        return this.lfFileName;
    }

    public StringProperty getExtension() {
        return this.extension;
    }
}
