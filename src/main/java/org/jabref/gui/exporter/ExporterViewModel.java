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

    public ExporterViewModel() {
        this(new TemplateExporter()); //You will need a new constructor in logic for this, that is probably also called by JAbREf preferences, for creating a new TE in the Export Customization subdialog
    }

    public ExporterViewModel(TemplateExporter exporter) {
        this.exporter = exporter;
        this.name.setValue(exporter.getName());
        this.lfFileName.setValue(exporter.getLayoutFileName());
        //This should return at least one of the extensions, but may need to be changed to return the most common extension
        this.extension.setValue(exporter.getFileType().getExtensionsWithDot().get(0));
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
