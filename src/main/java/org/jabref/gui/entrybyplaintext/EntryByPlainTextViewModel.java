package org.jabref.gui.entrybyplaintext;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.logic.plaintextparser.ParserPipeline;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class EntryByPlainTextViewModel {

  private final StringProperty inputText = new SimpleStringProperty("");
  private final BibDatabaseContext bibDatabaseContext;
  private final BibDatabaseContext newDatabaseContext;



  public EntryByPlainTextViewModel(BibDatabaseContext bibDatabaseContext){

    this.bibDatabaseContext = bibDatabaseContext;
    newDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));

  }

  public StringProperty inputTextProperty(){

    return this.inputText;

  }

  public void startParsing(){
    //TODO: Add method to start the parsing of the text

    if(ParserPipeline.parsePlainRefCit(inputText.getValue()).isPresent()){
      BibEntry bibEntry = ParserPipeline.parsePlainRefCit(inputText.getValue()).get();

      this.bibDatabaseContext.getDatabase().insertEntry(bibEntry);
      newDatabaseContext.getDatabase().insertEntry(bibEntry);
      newDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
      JabRefGUI.getMainFrame().addTab(newDatabaseContext,true);

      JabRefGUI.getMainFrame().getCurrentBasePanel().showAndEdit(bibEntry);
      trackNewEntry(StandardEntryType.Article);
      JabRefGUI.getMainFrame().getDialogService().notify("Successfully added a new entry.");
    } else{
      parsingFail(inputTextProperty().getValue());
    }
  }

  public void parsingFail(String input){
      FailedToParseDialog dlg = new FailedToParseDialog(input);
      dlg.showAndWait();
  }



  private void trackNewEntry(EntryType type) {
    Map<String, String> properties = new HashMap<>();
    properties.put("EntryType", type.getName());

    Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("NewEntry", properties, new HashMap<>()));
  }
}
