package org.jabref.gui.entrybyplaintext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
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
  private boolean directAdd;
  private final BibDatabaseContext newDatabaseContext;
  private List<BibEntry> extractedEntries;


  public EntryByPlainTextViewModel(BibDatabaseContext bibDatabaseContext){

    this.bibDatabaseContext = bibDatabaseContext;
    newDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));

  }

  public StringProperty inputTextProperty(){

    return this.inputText;

  }

  public void startParsing(boolean directAdd){
    this.directAdd = directAdd;
    this.extractedEntries = null;
    JabRefExecutorService.INSTANCE.execute(() -> {
        this.extractedEntries = ParserPipeline.parsePlainRefCit(inputText.getValue());
        Platform.runLater(this::executeParse);
    });
  }

  public void executeParse(){
      if(extractedEntries.size() > 0){
          for (BibEntry bibEntry: extractedEntries) {
              parsingSuccess(bibEntry);
          }
      } else{
          parsingFail(inputText.getValue());
      }
  }

  public void parsingSuccess(BibEntry bibEntry){
      if(directAdd) {

          newDatabaseContext.getDatabase().insertEntry(bibEntry);
          newDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
          JabRefGUI.getMainFrame().addTab(newDatabaseContext,true);
      }else{
          this.bibDatabaseContext.getDatabase().insertEntry(bibEntry);
          JabRefGUI.getMainFrame().getCurrentBasePanel().showAndEdit(bibEntry);
          trackNewEntry(StandardEntryType.Article);


      }
      JabRefGUI.getMainFrame().getDialogService().notify("Successfully added a new entry.");
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
