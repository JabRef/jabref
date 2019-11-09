package org.jabref.gui.entrybyplaintext;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


import org.jabref.Globals;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.bibtexextractor.BibtexExtractor;
import org.jabref.gui.bibtexextractor.FailedToExtractDialog;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class EntryByPlainTextViewModel {

  private final StringProperty inputText = new SimpleStringProperty("");
  private final BibDatabaseContext bibDatabaseContext;



  public EntryByPlainTextViewModel(BibDatabaseContext bibDatabaseContext){

    this.bibDatabaseContext = bibDatabaseContext;

  }

  public StringProperty inputTextProperty(){

    return this.inputText;

  }

  public void startParsing(){
    //TODO: Add method to start the parsing of the text
    BibtexExtractor bibtexExtractor = new BibtexExtractor();
    BibEntry bibEntry = bibtexExtractor.extract(inputText.getValue()); //TODO: Replace with our methods
    this.bibDatabaseContext.getDatabase().insertEntry(bibEntry);
    trackNewEntry(StandardEntryType.Article);
  }

  public void parsingFail(String input){
      FailedToExtractDialog dlg = new FailedToExtractDialog(input);
      dlg.showAndWait();
  }



  private void trackNewEntry(EntryType type) {
    Map<String, String> properties = new HashMap<>();
    properties.put("EntryType", type.getName());

    Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
  }
}
